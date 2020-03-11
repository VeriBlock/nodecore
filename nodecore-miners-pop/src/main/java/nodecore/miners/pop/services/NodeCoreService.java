// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.services;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import kotlin.Unit;
import nodecore.api.grpc.AdminGrpc;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.VeriBlockMessages.BitcoinBlockHeader;
import nodecore.api.grpc.VeriBlockMessages.GetBitcoinBlockIndexRequest;
import nodecore.api.grpc.VeriBlockMessages.GetInfoReply;
import nodecore.api.grpc.VeriBlockMessages.GetInfoRequest;
import nodecore.api.grpc.VeriBlockMessages.GetLastBlockReply;
import nodecore.api.grpc.VeriBlockMessages.GetLastBlockRequest;
import nodecore.api.grpc.VeriBlockMessages.GetPoPEndorsementsInfoReply;
import nodecore.api.grpc.VeriBlockMessages.GetPoPEndorsementsInfoRequest;
import nodecore.api.grpc.VeriBlockMessages.GetPopReply;
import nodecore.api.grpc.VeriBlockMessages.GetPopRequest;
import nodecore.api.grpc.VeriBlockMessages.LockWalletRequest;
import nodecore.api.grpc.VeriBlockMessages.PingRequest;
import nodecore.api.grpc.VeriBlockMessages.ProtocolReply;
import nodecore.api.grpc.VeriBlockMessages.SubmitPopRequest;
import nodecore.api.grpc.VeriBlockMessages.UnlockWalletRequest;
import nodecore.miners.pop.Configuration;
import nodecore.miners.pop.common.Utility;
import nodecore.miners.pop.contracts.ApplicationExceptions;
import nodecore.miners.pop.contracts.BlockStore;
import nodecore.miners.pop.contracts.NodeCoreReply;
import nodecore.miners.pop.contracts.PoPEndorsementInfo;
import nodecore.miners.pop.contracts.PoPMiningInstruction;
import nodecore.miners.pop.contracts.PoPMiningTransaction;
import nodecore.miners.pop.contracts.VeriBlockHeader;
import nodecore.miners.pop.contracts.result.Result;
import nodecore.miners.pop.events.EventBus;
import nodecore.miners.pop.events.NewVeriBlockFoundEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class NodeCoreService {
    private static final Logger _logger = LoggerFactory.getLogger(NodeCoreService.class);

    private final Configuration configuration;
    private final ChannelBuilder channelBuilder;
    private final BlockStore blockStore;
    private final AtomicBoolean healthy = new AtomicBoolean(false);
    private final AtomicBoolean _synchronized = new AtomicBoolean(false);

    private ManagedChannel _channel;
    private AdminGrpc.AdminBlockingStub _blockingStub;
    private ScheduledExecutorService scheduledExecutorService;

    public boolean isHealthy() {
        return healthy.get();
    }

    private boolean isSynchronized() {
        return _synchronized.get();
    }

    public NodeCoreService(Configuration configuration, ChannelBuilder channelBuilder, BlockStore blockStore) {
        this.configuration = configuration;
        this.channelBuilder = channelBuilder;
        this.blockStore = blockStore;

        initializeClient();

        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("nc-poll").build());
        scheduledExecutorService.scheduleWithFixedDelay(this::poll, 5, 1, TimeUnit.SECONDS);

        EventBus.INSTANCE.getConfigurationChangedEvent().register(this, this::onNodeCoreConfigurationChanged);
    }

    private void initializeClient() {
        _logger.info("Connecting to NodeCore at {}:{} {}", configuration.getNodeCoreHost(), configuration.getNodeCorePort(),
            configuration.getNodeCoreUseSSL() ? "over SSL" : ""
        );

        try {
            _channel = channelBuilder.buildManagedChannel();
            _blockingStub = AdminGrpc.newBlockingStub(channelBuilder.attachPasswordInterceptor(_channel));
        } catch (SSLException e) {
            _logger.error("NodeCore SSL configuration error", e);
        }
    }

    public void shutdown() throws InterruptedException {
        scheduledExecutorService.shutdown();
        _channel.shutdown().awaitTermination(15, TimeUnit.SECONDS);
    }

    public boolean ping() {
        if (_blockingStub == null) {
            return false;
        }

        try {
            _blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).ping(PingRequest.newBuilder().build());
            return true;
        } catch (StatusRuntimeException e) {
            _logger.debug("Unable to connect ping NodeCore at this time");
            return false;
        }
    }

    /**
     * Verify if the connected NodeCore is synchronized with the network (the block difference between the networkHeight and the localBlockchainHeight
     * should be smaller than 4 blocks)
     *
     * This function might return false (StatusRuntimeException) if NodeCore is not accessible or if NodeCore still loading (networkHeight = 0)
     */
    private boolean isNodeCoreSynchronized() {
        if (_blockingStub == null) {
            return false;
        }

        try {
            final VeriBlockMessages.GetStateInfoReply request = _blockingStub
                    .withDeadlineAfter(5L, TimeUnit.SECONDS)
                    .getStateInfo(VeriBlockMessages.GetStateInfoRequest.newBuilder().build());
            final int blockDifference = Math.abs(request.getNetworkHeight() - request.getLocalBlockchainHeight());
            return request.getNetworkHeight() > 0 && blockDifference < 4;
        } catch (StatusRuntimeException e) {
            _logger.warn("Unable to perform GetStateInfoRequest to NodeCore");
            return false;
        }
    }

    public NodeCoreReply<PoPMiningInstruction> getPop(Integer blockNumber) {
        GetPopRequest.Builder requestBuilder = GetPopRequest.newBuilder();
        if (blockNumber != null && blockNumber > 0) {
            requestBuilder.setBlockNum(blockNumber);
        }
        GetPopRequest request = requestBuilder.build();
        GetPopReply reply = _blockingStub.withDeadlineAfter(15, TimeUnit.SECONDS).getPop(request);

        NodeCoreReply<PoPMiningInstruction> result = new NodeCoreReply<>();
        if (reply.getSuccess()) {
            result.setSuccess(true);

            PoPMiningInstruction instruction = new PoPMiningInstruction();
            instruction.publicationData = reply.getFullPop().toByteArray();
            instruction.minerAddress = reply.getPopMinerAddress().toByteArray();
            instruction.lastBitcoinBlock = reply.getLastKnownBlock().getHeader().toByteArray();
            instruction.endorsedBlockHeader = Arrays.copyOfRange(instruction.publicationData, 0, 64);

            if (reply.getLastKnownBlockContextCount() > 0) {
                instruction.endorsedBlockContextHeaders =
                        reply.getLastKnownBlockContextList().stream().map(header -> header.getHeader().toByteArray()).collect(Collectors.toList());
            } else {
                instruction.endorsedBlockContextHeaders = new ArrayList<>();
            }

            result.setResult(instruction);
        } else {
            result.setSuccess(false);

            StringBuilder message = new StringBuilder();
            for (VeriBlockMessages.Result r : reply.getResultsList()) {
                if (r.getMessage() != null) {
                    message.append(r.getMessage()).append("\n");
                }
                if (r.getDetails() != null) {
                    message.append("\t").append(r.getDetails()).append("\n");
                }
            }

            result.setResultMessage(message.toString());
        }

        return result;
    }

    public String submitPop(PoPMiningTransaction popMiningTransaction) {
        BitcoinBlockHeader.Builder blockOfProofBuilder = BitcoinBlockHeader.newBuilder();
        blockOfProofBuilder.setHeader(ByteString.copyFrom(popMiningTransaction.getBitcoinBlockHeaderOfProof()));

        SubmitPopRequest.Builder request = SubmitPopRequest.newBuilder()
                .setEndorsedBlockHeader(ByteString.copyFrom(popMiningTransaction.getEndorsedBlockHeader()))
                .setBitcoinTransaction(ByteString.copyFrom(popMiningTransaction.getBitcoinTransaction()))
                .setBitcoinMerklePathToRoot(ByteString.copyFrom(popMiningTransaction.getBitcoinMerklePathToRoot()))
                .setBitcoinBlockHeaderOfProof(blockOfProofBuilder)
                .setAddress(ByteString.copyFrom(popMiningTransaction.getPopMinerAddress()));

        for (int i = 0; i < popMiningTransaction.getBitcoinContextBlocks().length; i++) {
            byte[] contextBlockHeader = popMiningTransaction.getBitcoinContextBlocks()[i];
            BitcoinBlockHeader.Builder contextBlockBuilder = BitcoinBlockHeader.newBuilder();
            contextBlockBuilder.setHeader(ByteString.copyFrom(contextBlockHeader));
            BitcoinBlockHeader header = contextBlockBuilder.build();
            request.addContextBitcoinBlockHeaders(header);
        }

        ProtocolReply reply = _blockingStub.submitPop(request.build());
        if (reply.getSuccess()) {
            return reply.getResults(0).getDetails();
        }

        throw new ApplicationExceptions.PoPSubmitRejected();
    }

    public List<PoPEndorsementInfo> getPoPEndorsementInfo() {
        GetPoPEndorsementsInfoRequest request = GetPoPEndorsementsInfoRequest.newBuilder().setSearchLength(750).build();

        GetPoPEndorsementsInfoReply reply = _blockingStub.getPoPEndorsementsInfo(request);
        return reply.getPopEndorsementsList().stream().map(PoPEndorsementInfo::new).collect(Collectors.toList());
    }

    public Integer getBitcoinBlockIndex(byte[] blockHeader) {
        GetBitcoinBlockIndexRequest request =
                GetBitcoinBlockIndexRequest.newBuilder().setBlockHeader(ByteString.copyFrom(blockHeader)).setSearchLength(20).build();

        ProtocolReply reply = _blockingStub.getBitcoinBlockIndex(request);
        if (reply.getSuccess() && reply.getResultsCount() > 0) {
            return Integer.parseInt(reply.getResults(0).getDetails());
        }

        return null;
    }

    public String getMinerAddress() {
        GetInfoRequest request = GetInfoRequest.newBuilder().build();
        GetInfoReply reply = _blockingStub.getInfo(request);

        return Utility.bytesToBase58(reply.getDefaultAddress().getAddress().toByteArray());
    }

    public VeriBlockHeader getLastBlock() {
        GetLastBlockReply reply = _blockingStub
                .withDeadlineAfter(10, TimeUnit.SECONDS)
                .getLastBlock(GetLastBlockRequest.newBuilder().build());

        return new VeriBlockHeader(reply.getHeader().getHeader().toByteArray());
    }

    public Result unlockWallet(String passphrase) {
        UnlockWalletRequest request = UnlockWalletRequest.newBuilder().setPassphrase(passphrase).build();

        ProtocolReply protocolReply = _blockingStub.unlockWallet(request);
        Result result = new Result();
        if (!protocolReply.getSuccess()) {
            result.fail();
        }
        for (VeriBlockMessages.Result r : protocolReply.getResultsList()) {
            result.addMessage(r.getCode(), r.getMessage(), r.getDetails(), r.getError());
        }

        return result;
    }

    public Result lockWallet() {
        LockWalletRequest request = LockWalletRequest.newBuilder().build();

        ProtocolReply protocolReply = _blockingStub.lockWallet(request);
        Result result = new Result();
        if (!protocolReply.getSuccess()) {
            result.fail();
        }
        for (VeriBlockMessages.Result r : protocolReply.getResultsList()) {
            result.addMessage(r.getCode(), r.getMessage(), r.getDetails(), r.getError());
        }

        return result;
    }

    public Unit onNodeCoreConfigurationChanged() {
        try {
            initializeClient();
        } catch (Exception e) {
            _logger.error(e.getMessage(), e);
        }
        return Unit.INSTANCE;
    }

    private void poll() {
        try {
            if (isHealthy() && isSynchronized()) {
                if (!isNodeCoreSynchronized()) {
                    _synchronized.set(false);
                    _logger.info("The connected node is not synchronized");
                    EventBus.INSTANCE.getNodeCoreDesynchronizedEvent().trigger();
                    return;
                }

                VeriBlockHeader latestBlock;
                try {
                    latestBlock = getLastBlock();
                } catch (Exception e) {
                    _logger.error("Unable to get the last block from NodeCore");
                    healthy.set(false);
                    EventBus.INSTANCE.getNodeCoreUnhealthyEvent().trigger();
                    return;
                }

                VeriBlockHeader chainHead = blockStore.getChainHead();
                if (!latestBlock.equals(chainHead)) {
                    blockStore.setChainHead(latestBlock);
                    EventBus.INSTANCE.getNewVeriBlockFoundEvent().trigger(new NewVeriBlockFoundEventDto(latestBlock, chainHead));
                }
            } else {
                if (ping()) {
                    if (!isHealthy()) {
                        _logger.info("Connected to NodeCore");
                        EventBus.INSTANCE.getNodeCoreHealthyEvent().trigger();
                    }
                    healthy.set(true);

                    if (isNodeCoreSynchronized()) {
                        if (!isSynchronized()) {
                            _logger.info("The connected node is synchronized");
                            EventBus.INSTANCE.getNodeCoreSynchronizedEvent().trigger();
                        }
                        _synchronized.set(true);
                    } else {
                        if (isSynchronized()) {
                            _logger.info("The connected node is not synchronized");
                            EventBus.INSTANCE.getNodeCoreDesynchronizedEvent().trigger();
                        }
                        _synchronized.set(false);
                    }
                } else {
                    if (isHealthy()) {
                        EventBus.INSTANCE.getNodeCoreUnhealthyEvent().trigger();
                    }
                    if (isSynchronized()) {
                        _logger.info("The connected node is not synchronized");
                        EventBus.INSTANCE.getNodeCoreDesynchronizedEvent().trigger();
                    }
                    healthy.set(false);
                    _synchronized.set(false);
                }
            }
        } catch (Exception e) {
            _logger.error("Error while polling NodeCore", e);
        }
    }
}
