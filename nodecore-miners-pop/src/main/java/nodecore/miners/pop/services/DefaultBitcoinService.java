// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.services;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import nodecore.miners.pop.Constants;
import nodecore.miners.pop.InternalEventBus;
import nodecore.miners.pop.Threading;
import nodecore.miners.pop.common.BitcoinNetwork;
import nodecore.miners.pop.contracts.ApplicationExceptions.DuplicateTransactionException;
import nodecore.miners.pop.contracts.ApplicationExceptions.UnableToAcquireTransactionLock;
import nodecore.miners.pop.contracts.ApplicationExceptions.CorruptSPVChain;
import nodecore.miners.pop.contracts.ApplicationExceptions.ExceededMaxTransactionFee;
import nodecore.miners.pop.contracts.ApplicationExceptions.SendTransactionException;
import nodecore.miners.pop.contracts.BitcoinService;
import nodecore.miners.pop.contracts.Configuration;
import nodecore.miners.pop.events.*;
import nodecore.miners.pop.shims.WalletShim;
import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.*;
import org.bitcoinj.core.listeners.BlocksDownloadedEventListener;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptOpCodes;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class DefaultBitcoinService implements BitcoinService, BlocksDownloadedEventListener {
    private static final Logger logger = LoggerFactory.getLogger(DefaultBitcoinService.class);

    private final Configuration configuration;
    private final Context context;
    private final BitcoinBlockCache blockCache;

    private WalletAppKit kit;
    private BlockChain blockChain;
    private BlockStore blockStore;
    private Wallet wallet;
    private PeerGroup peerGroup;
    private final BitcoinSerializer serializer;
    private final Semaphore txGate = new Semaphore(1, true);

    private static final Object EMPTY_OBJECT = new Object();
    private final LinkedHashMap<String, Object> txBroadcastAudit = new LinkedHashMap<String, Object>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
            return size() > 50;
        }
    };

    private BitcoinNetwork bitcoinNetwork;
    private boolean isBlockchainDownloaded;
    private boolean isServiceReady;
    private String receiveAddress;
    private Address changeAddress;

    private Coin getMaximumTransactionFee() {
        return Coin.valueOf(configuration.getMaxTransactionFee());
    }

    private Coin getTransactionFeePerKB() {
        return Coin.valueOf(configuration.getTransactionFeePerKB());
    }

    public void setServiceReady(boolean value) {
        if (value == isServiceReady) return;

        this.isServiceReady = value;
        if (this.isServiceReady) {
            InternalEventBus.getInstance().post(new BitcoinServiceReadyEvent());
        } else {
            InternalEventBus.getInstance().post(new BitcoinServiceNotReadyEvent());
        }
    }

    @Inject
    public DefaultBitcoinService(Configuration configuration, Context context, BitcoinBlockCache cache) {
        this.configuration = configuration;
        this.context = context;
        this.blockCache = cache;
        this.bitcoinNetwork = configuration.getBitcoinNetwork();

        InternalEventBus.getInstance().post(new InfoMessageEvent(
                String.format("Using Bitcoin %s network", this.bitcoinNetwork.toString())));

        this.serializer = new BitcoinSerializer(context.getParams(), true);
        this.kit = createWalletAppKit(context, getFilePrefix(bitcoinNetwork), null);

        logger.info("DefaultBitcoinService constructor finished");
    }

    private WalletAppKit createWalletAppKit(Context context, String filePrefix, DeterministicSeed seed) {
        isBlockchainDownloaded = false;

        DefaultBitcoinService self = this;

        WalletAppKit kit = new WalletAppKit(context.getParams(), Script.ScriptType.P2WPKH, null, new File("."), filePrefix) {
            @Override
            protected void onSetupCompleted() {
                super.onSetupCompleted();

                blockStore = this.store();
                wallet = this.wallet();
                wallet.setAcceptRiskyTransactions(true);
                blockChain = this.chain();
                peerGroup = this.peerGroup();

                wallet.addCoinsReceivedEventListener((wallet, tx, prevBalance, newBalance) ->
                        InternalEventBus.getInstance().post(new CoinsReceivedEvent(tx, prevBalance, newBalance)));

                peerGroup.addBlocksDownloadedEventListener(self);

                setServiceReady(true);
            }
        };

        kit.setBlockingStartup(false);
        kit.setDownloadListener(new DownloadProgressTracker() {
            @Override
            protected void doneDownload() {
                if (!isBlockchainDownloaded) {
                    isBlockchainDownloaded = true;
                    InternalEventBus.getInstance().post(new BlockchainDownloadedEvent("Bitcoin blockchain finished downloading"));
                }
            }

            @Override
            protected void progress(double pct, int blocksSoFar, Date date) {
                super.progress(pct, blocksSoFar, date);

                // Don't report progress at the end, doneDownload() will handle that
                if (blocksSoFar < 10) return;

                if ((int) pct % 5 == 0) {
                    InternalEventBus.getInstance().post(new InfoMessageEvent(String.format("Blockchain downloading: %d%%", (int)pct)));
                }
                if (pct > 95.0 && blocksSoFar % 10 == 0) {
                    InternalEventBus.getInstance().post(new InfoMessageEvent(String.format("Blockchain downloading: %d blocks to go", blocksSoFar)));
                }
            }
        });

        if (seed != null) {
            kit = kit.restoreWalletFromSeed(seed);
        }

        return kit;
    }

    @Override
    public void initialize() throws CorruptSPVChain {
        if (bitcoinNetwork == BitcoinNetwork.RegTest)
            kit.connectToLocalHost();

        kit.startAsync();

        try {
            kit.awaitRunning();
        } catch (IllegalStateException e) {
            File spvchain = new File(getFilePrefix(bitcoinNetwork) + ".spvchain");
            boolean successfulDelete = spvchain.delete();
            logger.error("An exception has occurred while waiting for the wallet kit to begin running!", e);
            if (successfulDelete) {
                logger.info("Deleted corrupt SPV chain...");
                throw new CorruptSPVChain("A corrupt SPV chain has been detected and deleted. Please restart the PoP miner, and run 'resetwallet'!");
            } else {
                logger.info("Unable to delete corrupt SPV chain, please delete " + spvchain.getAbsolutePath() + "!");
                throw new CorruptSPVChain("A corrupt SPV chain has been detected but could not be " +
                        "deleted. Please delete " + spvchain.getAbsolutePath() + ", restart the PoP miner, and run 'resetwallet'!");
            }
        }
    }

    @Override
    public boolean serviceReady() {
        return isBlockchainDownloaded && isServiceReady;
    }

    @Override
    public String currentReceiveAddress() {
        if (receiveAddress == null) {
            receiveAddress = wallet.currentReceiveAddress().toString();
        }
        return receiveAddress;
    }

    private Address currentChangeAddress() {
        if (changeAddress == null) {
            changeAddress = wallet.currentChangeAddress();
        }
        return changeAddress;
    }

    @Override
    public Coin getBalance() {
        return wallet.getBalance(Wallet.BalanceType.AVAILABLE_SPENDABLE);
    }

    @Override
    public void resetWallet() {
        receiveAddress = null;
        setServiceReady(false);

        wallet.reset();
        shutdown();

        this.kit = createWalletAppKit(context, getFilePrefix(bitcoinNetwork), null);
        initialize();
    }

    @Override
    public boolean blockchainDownloaded() {
        return isBlockchainDownloaded;
    }

    @Override
    public Script generatePoPScript(byte[] opReturnData) {
        return new ScriptBuilder()
                .op(ScriptOpCodes.OP_RETURN)
                .data(opReturnData)
                .build();
    }

    @Override
    public ListenableFuture<Transaction> createPoPTransaction(Script opReturnScript) throws SendTransactionException {
        return sendTxRequest(() -> {
            Transaction tx = new Transaction(kit.params());
            tx.addOutput(Coin.ZERO, opReturnScript);

            SendRequest request = SendRequest.forTx(tx);
            request.ensureMinRequiredFee = configuration.isMinimumRelayFeeEnforced();
            request.changeAddress = wallet.currentChangeAddress();
            request.feePerKb = getTransactionFeePerKB();
            request.changeAddress = currentChangeAddress();

            return request;
        });
    }

    @Override
    public Block getBlock(Sha256Hash hash) {
        try {
            StoredBlock block = blockStore.get(hash);
            if (block == null) {
                logger.warn("Unable to retrieve block {}", hash.toString());
                return null;
            }

            return block.getHeader();
        } catch (BlockStoreException e) {
            logger.error("Unable to get block {} from store", hash.toString(), e);
            return null;
        }
    }
    
    @Override
    public StoredBlock getLastBlock() {
        return blockChain.getChainHead();
    }

    @Override
    public Block getBestBlock(Collection<Sha256Hash> hashes) {
        HashMap<Sha256Hash, StoredBlock> storedBlocks = new HashMap<>();
        for (Sha256Hash hash : hashes) {
            try {
                storedBlocks.put(hash, blockStore.get(hash));
            } catch (BlockStoreException e) {
                logger.error("Unable to get block from store", e);
            }
        }

        StoredBlock cursor = blockChain.getChainHead();
        do {
            if (storedBlocks.containsKey(cursor.getHeader().getHash())) {
                return cursor.getHeader();
            }

            try {
                cursor = cursor.getPrev(blockStore);
            } catch (BlockStoreException e) {
                logger.error("Unable to get block from store", e);
                break;
            }
        } while (cursor != null);

        return null;
    }

    private final Object downloadLock = new Object();
    private final ConcurrentHashMap<String, ListenableFuture<Block>> blockDownloader = new ConcurrentHashMap<>();


    @Override
    public ListenableFuture<FilteredBlock> getFilteredBlockFuture(Sha256Hash hash) {
        return blockCache.getAsync(hash.toString());
    }

    @Override
    public PartialMerkleTree getPartialMerkleTree(Sha256Hash hash) {
        try {
            logger.info("Awaiting block {}...", hash.toString());
            FilteredBlock block = blockCache.getAsync(hash.toString())
                    .get(configuration.getActionTimeout(), TimeUnit.SECONDS);

            if (block != null) {
                return block.getPartialMerkleTree();
            }
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            logger.error("Unable to download Bitcoin block", e);
        }

        return null;
    }

    private Block downloadBlock(Sha256Hash hash) {
        logger.info("Attempting to download block with hash {}", hash.toString());

        ListenableFuture<Block> blockFuture;
        // Lock for read to see if we've got a download already started
        synchronized (downloadLock) {
            blockFuture = blockDownloader.get(hash.toString());
            if (blockFuture == null) {
                logger.info("Starting download of block {} from peer group", hash.toString());
                blockFuture = peerGroup.getDownloadPeer().getBlock(hash);
                blockDownloader.putIfAbsent(hash.toString(), blockFuture);
            } else {
                logger.info("Found existing download of block {}", hash.toString());
            }
        }

        Block block = null;
        try {
            logger.info("Waiting for block {} to finish downloading", hash.toString());
            block = blockFuture.get(configuration.getActionTimeout(), TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            logger.error("Unable to download Bitcoin block", e);
        }

        if (block != null) {
            logger.info("Finished downloading block with hash {}", hash.toString());
        }

        return block;
    }

    @Override
    public Block makeBlock(byte[] raw) {
        if (raw == null) return null;

        return serializer.makeBlock(raw);
    }

    @Override
    public Collection<Block> makeBlocks(Collection<byte[]> raw) {
        if (raw == null) return null;

        return raw.stream().map(serializer::makeBlock).collect(Collectors.toSet());
    }

    @Override
    public Transaction makeTransaction(byte[] raw) {
        if (raw == null) return null;

        Transaction rawTx = serializer.makeTransaction(raw);

        // Try to get the transaction from the wallet first
        Transaction reconstitutedTx = wallet.getTransaction(rawTx.getTxId());
        if (reconstitutedTx == null) {
            logger.debug("Could not find transaction {} in wallet", rawTx.getTxId().toString());
            try {
                reconstitutedTx = peerGroup.getDownloadPeer().getPeerMempoolTransaction(rawTx.getTxId()).get();
            } catch (Exception e) {
                logger.error("Unable to download mempool transaction", e);
            }
        }

        return reconstitutedTx;
    }

    @Override
    public Transaction sendCoins(String address, Coin amount) throws SendTransactionException {
        try {
            return sendTxRequest(() -> {
                SendRequest sendRequest = SendRequest.to(Address.fromString(kit.params(), address), amount);
                sendRequest.changeAddress = wallet.currentChangeAddress();
                sendRequest.feePerKb = getTransactionFeePerKB();

                return sendRequest;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new SendTransactionException(e);
        }
    }

    @Override
    public Pair<Integer, Long> calculateFeesFromLatestBlock() {
        try {
            StoredBlock chainHead = blockChain.getChainHead();
            Block block = downloadBlock(chainHead.getHeader().getHash());


            if (block != null && block.getTransactions() != null && block.getTransactions().size() > 0) {
                Coin fees = block.getTransactions().get(0).getOutputSum().minus(block.getBlockInflation(chainHead.getHeight()));

                long averageFees = fees.longValue() / block.getOptimalEncodingMessageSize();

                return Pair.of(chainHead.getHeight(), averageFees);
            }
        } catch (Exception e) {
            logger.error("Unable to calculate fees from latest block", e);
        }

        return null;
    }

    @Override
    public List<String> getMnemonicSeed() {
        DeterministicSeed seed = wallet.getKeyChainSeed();
        if (seed != null) {
            List<String> mnemonicCode = seed.getMnemonicCode();
            if (mnemonicCode != null) {
                List<String> result = new ArrayList<>(seed.getMnemonicCode());
                result.add(0, Long.toString(seed.getCreationTimeSeconds()));
                return result;
            }
        }

        return Collections.emptyList();
    }

    @Override
    public boolean importWallet(String seedWords, Long creationTime) {
        if (creationTime == null) {
            creationTime = Constants.DEFAULT_WALLET_CREATION_DATE;
        }
        try {
            shutdown();

            DeterministicSeed seed = new DeterministicSeed(seedWords, null, "", creationTime);

            this.kit = createWalletAppKit(context, getFilePrefix(bitcoinNetwork), seed);
            initialize();
            return true;
        } catch (Exception e) {
            logger.error("Could not import wallet", e);
            return false;
        }
    }

    @Override
    public List<String> exportPrivateKeys() {
        List<DeterministicKey> keys = wallet.getActiveKeyChain().getLeafKeys();
        List<String> wifKeys = keys.stream()
                .map(key -> key.getPrivateKeyAsWiF(wallet.getNetworkParameters()))
                .collect(Collectors.toList());
        return wifKeys;
    }

    @Override
    public void shutdown() {
        setServiceReady(false);
        receiveAddress = null;

        kit.stopAsync();
        kit.awaitTerminated();
    }

    private static String getFilePrefix(BitcoinNetwork bitcoinNetwork) {
        String filePrefix = "bitcoin-pop";
        switch (bitcoinNetwork) {
            case MainNet:
                break;
            case TestNet:
                filePrefix = filePrefix + "-testnet";
                break;
            case RegTest:
                filePrefix = filePrefix + "-regtest";
        }

        return filePrefix;
    }

    @Override
    public void onBlocksDownloaded(Peer peer, Block block, @Nullable FilteredBlock filteredBlock, int blocksLeft) {
        if (filteredBlock != null) {
            logger.info("FilteredBlock {} downloaded", block.getHashAsString());
            blockCache.put(block.getHashAsString(), filteredBlock);
        }
    }

    private ListenableFuture<Transaction> sendTxRequest(Supplier<SendRequest> requestBuilder) throws SendTransactionException {
        try {
            acquireTxLock();
        } catch (UnableToAcquireTransactionLock e) {
            throw new SendTransactionException(e);
        }

        SendRequest request = requestBuilder.get();
        try {
            /* WalletShim is a temporary solution until improvements present in bitcoinj's master branch
                are packaged into a published release
             */
            WalletShim.completeTx(wallet, request);
            if (request.tx.getFee().isGreaterThan(getMaximumTransactionFee())) {
                throw new ExceededMaxTransactionFee();
            }
            if (txBroadcastAudit.containsKey(request.tx.getTxId().toString())) {
                throw new DuplicateTransactionException();
            }

            logger.info("Created transaction spending " + request.tx.getInputs().size() + " inputs:");
            for (int i = 0; i < request.tx.getInputs().size(); i++) {
                logger.info("\t" + request.tx.getInputs().get(i).getOutpoint().getHash().toString() + ":" + request.tx.getInputs().get(i).getOutpoint().getIndex());
            }
        } catch (Exception e) {
            releaseTxLock();
            throw new SendTransactionException(e);
        }

        // Broadcast the transaction to the network peer group
        // BitcoinJ adds a listener that will commit the transaction to the wallet when a
        // sufficient number of peers have announced receipt
        logger.info("Broadcasting tx {} to peer group", request.tx.getTxId());
        TransactionBroadcast broadcast = kit.peerGroup().broadcastTransaction(request.tx);
        txBroadcastAudit.put(request.tx.getTxId().toString(), EMPTY_OBJECT);

        // Add a callback that releases the semaphore permit
        Futures.addCallback(broadcast.future(), new FutureCallback<Transaction>() {
            @Override
            public void onSuccess(@Nullable Transaction result) {
                releaseTxLock();
            }

            @Override
            public void onFailure(Throwable t) {
                releaseTxLock();
            }
        }, Threading.TASK_POOL);

        logger.info("Awaiting confirmation of broadcast of Tx {}", request.tx.getTxId().toString());

        return broadcast.future();
    }

    private void acquireTxLock() throws UnableToAcquireTransactionLock {
        logger.info("Waiting to acquire lock to create transaction");
        try {
            boolean permitted = txGate.tryAcquire(10, TimeUnit.SECONDS);
            if (!permitted) {
                throw new UnableToAcquireTransactionLock();
            }
        } catch (InterruptedException e) {
            throw new UnableToAcquireTransactionLock();
        }

        logger.info("Acquired lock to create transaction");
    }

    private void releaseTxLock() {
        logger.info("Releasing create transaction lock");
        txGate.release();
    }
}
