// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.miners.pop.common.Utility;
import nodecore.miners.pop.contracts.*;
import nodecore.miners.pop.events.*;
import nodecore.miners.pop.tasks.ProcessManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.utilities.BlockUtility;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DefaultPoPMiner implements PoPMiner {
    private static final Logger logger = LoggerFactory.getLogger(DefaultPoPMiner.class);

    private final Configuration configuration;
    private final BitcoinService bitcoinService;
    private final NodeCoreService nodeCoreService;
    private final PoPStateService stateService;
    private final PoPRepository repository;
    private final Context context;
    private final ConcurrentHashMap<String, PoPMiningOperationState> operations;
    private final KeyValueRepository keyValueRepository;
    private final ProcessManager processManager;

    private boolean stateRestored;
    private EnumSet<PoPMinerDependencies> readyConditions;
    private boolean readyToMine() {
        ensureBitcoinServiceReady();
        ensureBlockchainDownloaded();
        ensureSufficientFunds();

        return isReady();
    }

    @Inject
    public DefaultPoPMiner(
            Configuration configuration,
            BitcoinService bitcoinService,
            NodeCoreService nodeCoreService,
            PoPStateService stateService,
            PoPRepository repository,
            KeyValueRepository keyValueRepository,
            ProcessManager processManager,
            Context context) {

        this.configuration = configuration;
        this.bitcoinService = bitcoinService;
        this.nodeCoreService = nodeCoreService;
        this.stateService = stateService;
        this.repository = repository;
        this.keyValueRepository = keyValueRepository;
        this.processManager = processManager;
        this.context = context;
        this.operations = new ConcurrentHashMap<>();

        this.stateRestored = false;
        this.readyConditions = EnumSet.noneOf(PoPMinerDependencies.class);
    }

    @Override
    public void run() {
        InternalEventBus.getInstance().register(this);


        bitcoinService.initialize();
        if (!configuration.getBoolean(Constants.BYPASS_ACKNOWLEDGEMENT_KEY)) {
            KeyValueData data = keyValueRepository.get(Constants.WALLET_SEED_VIEWED_KEY);
            if (data == null || !data.value.equals("1")) {
                InternalEventBus.getInstance().post(new WalletSeedAgreementMissingEvent());
            }
        }
    }

    @Override
    public void shutdown() throws InterruptedException {
        InternalEventBus.getInstance().unregister(this);

        processManager.shutdown();
        bitcoinService.shutdown();
        nodeCoreService.shutdown();
    }

    @Override
    public boolean isReady() {
        return PoPMinerDependencies.SATISFIED.equals(readyConditions);
    }

    @Override
    public List<OperationSummary> listOperations() {
        return operations.values().stream()
                .map(state -> {
                    if (state == null) return null;

                    PoPMiningInstruction miningInstruction = state.getMiningInstruction();
                    int blockNumber = -1;
                    if (miningInstruction != null) {
                        blockNumber = BlockUtility.extractBlockHeightFromBlockHeader(miningInstruction.endorsedBlockHeader);
                    }
                    String status = state.getStatus() != null ? state.getStatus().toString() : "";

                    return new OperationSummary(state.getOperationId(), blockNumber, status, state.getCurrentActionAsString(), state.getMessage());
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(OperationSummary::getEndorsedBlockNumber))
                .collect(Collectors.toList());
    }

    @Override
    public PreservedPoPMiningOperationState getOperationState(String id) {
        PoPMiningOperationState state = stateService.getOperation(id);

        if (state == null) return null;

        // TODO: Implement
        PreservedPoPMiningOperationState result = new PreservedPoPMiningOperationState();
        result.operationId = state.getOperationId();
        result.status = state.getStatus();
        result.currentAction = state.getCurrentAction();
        result.miningInstruction = state.getMiningInstruction();
        result.transaction = state.getTransactionBytes();
        result.submittedTransactionId = state.getSubmittedTransactionId();
        result.bitcoinBlockHeaderOfProof = state.getBitcoinBlockHeaderOfProofBytes();
        result.bitcoinContextBlocks = state.getBitcoinContextBlocksBytes();
        result.merklePath = state.getMerklePath();
        result.detail = state.getMessage();
        result.popTransactionId = state.getPopTransactionId();

        return result;
    }

    @Override
    public MineResult mine(Integer blockNumber) {
        MineResult result = new MineResult();
        if (!readyToMine()) {
            result.fail();
            List<String> reasons = listPendingReadyConditions();
            result.addMessage(new DefaultResultMessage("V412", "Miner is not ready", reasons, true));
            return result;
        }

        // TODO: This is pretty naive. Wallet right now uses DefaultCoinSelector which doesn't do a great job with
        // multiple UTXO and long mempool chains. If that was improved, this count algorithm wouldn't be sufficient.
        long count = operations.values().parallelStream()
                .filter(state -> state.getCurrentAction() == PoPMiningOperationState.Action.WAIT)
                .count();

        if (count >= Constants.MEMPOOL_CHAIN_LIMIT) {
            result.fail();
            result.addMessage(new DefaultResultMessage("V412", "Too Many Pending Transactions", "Creating additional transactions at this time would result in rejection on the Bitcoin network", true));
            return result;
        }

        String operationId = Utility.generateOperationId();
        PoPMiningOperationState state = new PoPMiningOperationState(operationId, blockNumber);
        operations.putIfAbsent(operationId, state);

        processManager.submit(state);

        result.setOperationId(operationId);
        result.addMessage("V201", "Mining operation started",
                String.format("To view details, run command: getoperation %s", operationId),
                false);

        return result;
    }

    @Override
    public Result resubmit(String id) {
        DefaultResult result = new DefaultResult();
        if (!readyToMine()) {
            result.fail();
            List<String> reasons = listPendingReadyConditions();
            result.addMessage("V412", "Miner is not ready", String.join("; ", reasons), true);
            return result;
        }

        PoPMiningOperationState operation = operations.get(id);
        if (operation == null) {
            result.fail();
            result.addMessage("V404", "Operation not found", String.format("Could not find operation with id '%s'", id), true);
            return result;
        }

        processManager.submit(operation);

        result.addMessage("V200", "Success",
                String.format("To view details, run command: getoperation %s", operation.getOperationId()),
                false);

        return result;
    }

    @Override
    public String getMinerAddress() throws StatusRuntimeException {
        if (readyConditions.contains(PoPMinerDependencies.NODECORE_CONNECTED)) {
            return nodeCoreService.getMinerAddress();
        }

        return null;
    }

    @Override
    public Coin getBitcoinBalance() {
        return bitcoinService.getBalance();
    }
    
    @Override
    public StoredBlock getLastBitcoinBlock() {
        return bitcoinService.getLastBlock();
    }

    @Override
    public String getBitcoinReceiveAddress() {
        return bitcoinService.currentReceiveAddress();
    }

    @Override
    public List<String> getWalletSeed() {
        KeyValueData data = keyValueRepository.get(Constants.WALLET_SEED_VIEWED_KEY);
        if (data == null || !data.value.equals("1")) {
            agreeToWalletSeedRequirement();

            return bitcoinService.getMnemonicSeed();
        }

        return null;
    }

    @Override
    public void agreeToWalletSeedRequirement() {
        KeyValueData data = new KeyValueData();
        data.key = Constants.WALLET_SEED_VIEWED_KEY;
        data.value = "1";

        keyValueRepository.insert(data);
    }

    @Override
    public boolean importWallet(List<String> seedWords, Long creationDate) {
        return bitcoinService.importWallet(StringUtils.join(seedWords, " "), creationDate);
    }

    @Override
    public Result sendBitcoinToAddress(String address, BigDecimal amount) {
        Result result = new DefaultResult();

        Coin coinAmount = Utility.amountToCoin(amount);

        try {
            Transaction tx = bitcoinService.sendCoins(address, coinAmount);
            result.addMessage("V201", "Created", String.format("Transaction: %s", tx.getHashAsString()), false);
        } catch (ApplicationExceptions.SendTransactionException e) {
            for (Throwable t : e.getSuppressed()) {
                if (t instanceof ApplicationExceptions.UnableToAcquireTransactionLock) {
                    result.addMessage("V409", "Temporarily Unable to Create Tx", "A previous transaction has not yet completed broadcasting to peers and new transactions would result in double spending. Wait a few seconds and try again.", true);
                } else if (t instanceof InsufficientMoneyException) {
                    result.addMessage("V400", "Insufficient Funds", "Wallet does not contain sufficient funds to create transaction", true);
                } else if (t instanceof ApplicationExceptions.ExceededMaxTransactionFee) {
                    result.addMessage("V400", "Exceeded Max Fee", "Transaction fee was calculated to be more than the configured maximum transaction fee", true);
                } else if (t instanceof ApplicationExceptions.DuplicateTransactionException) {
                    result.addMessage("V409", "Duplicate Transaction", "Transaction created is a duplicate of a previously broadcast transaction", true);
                } else {
                    result.addMessage("V500", "Send Failed", "Unable to send coins, view logs for details", true);
                }
            }
            result.fail();
        }

        return result;
    }

    @Override
    public Pair<Integer, Long> showRecentBitcoinFees() {
        return bitcoinService.calculateFeesFromLatestBlock();
    }

    @Override
    public Result resetBitcoinWallet() {
        Result result = new DefaultResult();

        bitcoinService.resetWallet();

        result.addMessage("V200", "Success", "Wallet has been reset", false);

        return result;
    }

    @Override
    public Result exportBitcoinPrivateKeys() {
        Result result = new DefaultResult();

        try {
            String destination = String.format("keys-%d.txt", Instant.now().getEpochSecond());
            File export = new File(destination);
            boolean created = export.createNewFile();

            if (created) {
                List<String> keys = bitcoinService.exportPrivateKeys();
                try (PrintWriter writer = new PrintWriter(export)) {
                    for (String key : keys) {
                        writer.println(key);
                    }
                }

                result.addMessage("V201", "Export Successful", String.format("Keys have been exported to %s", export.getCanonicalPath()), false);
            } else {
                result.fail();
                result.addMessage("V409", "Export Failed", "The destination file already exists and could not be created", true);
            }

        } catch (IOException e) {
            logger.error("Unable to export private keys", e);
            result.fail();
            result.addMessage("V500", "Export Failed", e.getMessage(), true);
        }

        return result;
    }

    private void ensureBlockchainDownloaded() {
        if (!readyConditions.contains(PoPMinerDependencies.BLOCKCHAIN_DOWNLOADED) && bitcoinService.blockchainDownloaded()) {
            addReadyCondition(PoPMinerDependencies.BLOCKCHAIN_DOWNLOADED);
        }
    }

    private void ensureBitcoinServiceReady() {
        if (!readyConditions.contains(PoPMinerDependencies.BITCOIN_SERVICE_READY) && bitcoinService.serviceReady()) {
            addReadyCondition(PoPMinerDependencies.BITCOIN_SERVICE_READY);
        }
    }

    private void ensureSufficientFunds() {
        Coin maximumTransactionFee = Coin.valueOf(configuration.getMaxTransactionFee());
        if (bitcoinService.getBalance().isGreaterThan(maximumTransactionFee)) {
            if (!readyConditions.contains(PoPMinerDependencies.SUFFICIENT_FUNDS)) {
                InternalEventBus.getInstance().post(new FundsAddedEvent());
            }
            addReadyCondition(PoPMinerDependencies.SUFFICIENT_FUNDS);
        }
        else {
            removeReadyCondition(PoPMinerDependencies.SUFFICIENT_FUNDS);
        }
    }

    private void addReadyCondition(PoPMinerDependencies flag) {
        boolean previousReady = isReady();
        readyConditions.add(flag);

        if (!previousReady && isReady()) {
            if (!stateRestored) {
                restoreOperations();
            }
            InternalEventBus.getInstance().post(new PoPMinerReadyEvent());
        }
    }

    private void removeReadyCondition(PoPMinerDependencies flag) {
        boolean removed = readyConditions.remove(flag);
        if (removed) {
            InternalEventBus.getInstance().post(new PoPMinerNotReadyEvent(getMessageForDependencyCondition(flag), flag));
        }
    }

    private List<String> listPendingReadyConditions() {
        List<String> reasons = new ArrayList<>();
        EnumSet<PoPMinerDependencies> pending = EnumSet.complementOf(readyConditions);
        for (PoPMinerDependencies flag : pending) {
            reasons.add(getMessageForDependencyCondition(flag));
        }
        return reasons;
    }

    private String getMessageForDependencyCondition(PoPMinerDependencies flag) {
        switch (flag) {
            case BLOCKCHAIN_DOWNLOADED:
                return "Bitcoin blockchain is not downloaded";
            case SUFFICIENT_FUNDS:
                Coin maximumTransactionFee = Coin.valueOf(configuration.getMaxTransactionFee());
                Coin balance = bitcoinService.getBalance();
                return "PoP wallet does not contain sufficient funds" +
                        System.lineSeparator() +
                        "  Current balance: " + Utility.formatBTCFriendlyString(balance) +
                        System.lineSeparator() +
                        String.format("  Minimum required: %1$s, need %2$s more",
                                Utility.formatBTCFriendlyString(maximumTransactionFee),
                                Utility.formatBTCFriendlyString(maximumTransactionFee.subtract(balance))) +
                        System.lineSeparator() +
                        "  Send Bitcoin to: " + bitcoinService.currentReceiveAddress();
            case NODECORE_CONNECTED:
                return "Waiting for connection to NodeCore";
            case BITCOIN_SERVICE_READY:
                return "Bitcoin service is not ready";
        }
        return "";
    }

    private void restoreOperations() {
        List<PoPMiningOperationState> preservedOperations = stateService.getActiveOperations();
        logger.info("Found {} operations to restore", preservedOperations.size());
        for (PoPMiningOperationState state : preservedOperations) {
            try {
                if (state != null) {
                    operations.put(state.getOperationId(), state);
                    processManager.restore(state);
                    logger.info("Successfully restored operation {}", state.getOperationId());
                }
            } catch (Exception e) {
                logger.error("Unable to restore previous operation {}", state.getOperationId());
            }
        }

        stateRestored = true;
    }

    @Subscribe public void onPoPMiningOperationCompleted(PoPMiningOperationCompletedEvent event) {
        try {
            operations.remove(event.getOperationId());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Subscribe public void onCoinsReceived(CoinsReceivedEvent event) {
        try {
            InternalEventBus.getInstance().post(new InfoMessageEvent(
                    String.format("Received pending tx '%s', pending balance: '%s'",
                            event.getTx().getHashAsString(),
                            Utility.formatBTCFriendlyString(event.getNewBalance()))));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Subscribe public void onInsufficientFunds(InsufficientFundsEvent event) {
        try {
            removeReadyCondition(PoPMinerDependencies.SUFFICIENT_FUNDS);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Subscribe public void onBitcoinServiceReady(BitcoinServiceReadyEvent event) {
        try {

            addReadyCondition(PoPMinerDependencies.BITCOIN_SERVICE_READY);

            if (!readyToMine()) {
                EnumSet<PoPMinerDependencies> failed = EnumSet.complementOf(readyConditions);
                for (PoPMinerDependencies flag : failed) {
                    InternalEventBus.getInstance().post(new PoPMinerNotReadyEvent(getMessageForDependencyCondition(flag), flag));
                }
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Subscribe public void onBitcoinServiceNotReady(BitcoinServiceNotReadyEvent event) {
        try {
            removeReadyCondition(PoPMinerDependencies.BITCOIN_SERVICE_READY);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Subscribe public void onBlockchainDownloaded(BlockchainDownloadedEvent event) {
        try {
            addReadyCondition(PoPMinerDependencies.BLOCKCHAIN_DOWNLOADED);

            ensureSufficientFunds();

            InternalEventBus.getInstance().post(new InfoMessageEvent("Available Bitcoin balance: " + Utility.formatBTCFriendlyString(bitcoinService.getBalance())));
            InternalEventBus.getInstance().post(new InfoMessageEvent("Send Bitcoin to: " + bitcoinService.currentReceiveAddress()));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Subscribe public void onNodeCoreHealthy(NodeCoreHealthyEvent event) {
        try {
            addReadyCondition(PoPMinerDependencies.NODECORE_CONNECTED);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Subscribe public void onNodeCoreUnhealthy(NodeCoreUnhealthyEvent event) {
        try {
            removeReadyCondition(PoPMinerDependencies.NODECORE_CONNECTED);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Subscribe public void onConfigurationChanged(ConfigurationChangedEvent event) {
        try {
            ensureSufficientFunds();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Subscribe public void onNewVeriBlockFound(NewVeriBlockFoundEvent event) {
        for (String key : new HashSet<>(operations.keySet())) {
            PoPMiningOperationState operationState = operations.get(key);
            if (operationState != null &&
                    operationState.getTransactionStatus() == TransactionStatus.UNCONFIRMED &&
                    operationState.getBlockNumber() < (event.getBlock().getHeight() - Constants.POP_SETTLEMENT_INTERVAL)) {
                operationState.fail(String.format("Endorsement of block %d is no longer relevant", operationState.getBlockNumber()));
            }
        }
    }
}
