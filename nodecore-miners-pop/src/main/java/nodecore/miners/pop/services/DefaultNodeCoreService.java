// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.services;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.*;
import nodecore.miners.pop.InternalEventBus;
import nodecore.miners.pop.common.Utility;
import nodecore.miners.pop.contracts.PoPEndorsementInfo;
import nodecore.miners.pop.contracts.Result;
import nodecore.miners.pop.contracts.*;
import nodecore.miners.pop.events.*;
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

public class DefaultNodeCoreService implements NodeCoreService {
    private static final Logger _logger = LoggerFactory.getLogger(DefaultNodeCoreService.class);

    private final Configuration configuration;
    private final ChannelBuilder channelBuilder;
    private final BlockStore blockStore;
    private final AtomicBoolean healthy = new AtomicBoolean(false);

    private ManagedChannel _channel;
    private AdminGrpc.AdminBlockingStub _blockingStub;
    private ScheduledExecutorService scheduledExecutorService;

    public boolean isHealthy() {
        return healthy.get();
    }

    @Inject
    public DefaultNodeCoreService(Configuration configuration, ChannelBuilder channelBuilder, BlockStore blockStore) {
        this.configuration = configuration;
        this.channelBuilder = channelBuilder;
        this.blockStore = blockStore;

        initializeClient();

        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("nc-poll").build());
        scheduledExecutorService.scheduleWithFixedDelay(this::poll, 5, 1, TimeUnit.SECONDS);

        InternalEventBus.getInstance().register(this);
    }

    private void initializeClient() {
        InternalEventBus.getInstance().post(new InfoMessageEvent(
                String.format("Connecting to NodeCore at %s:%d %s", configuration.getNodeCoreHost(),
                        configuration.getNodeCorePort(),
                        configuration.getNodeCoreUseSSL() ? "over SSL" : "")));

        try {
            _channel = channelBuilder.buildManagedChannel();
            _blockingStub = AdminGrpc.newBlockingStub(channelBuilder.attachPasswordInterceptor(_channel));
        } catch (SSLException e) {
            _logger.error(e.getMessage(), e);
            InternalEventBus.getInstance().post(new ErrorMessageEvent("NodeCore SSL configuration error, see log file for detail"));
        }
    }

    @Override
    public void shutdown() throws InterruptedException {
        scheduledExecutorService.shutdown();
        _channel.shutdown().awaitTermination(15, TimeUnit.SECONDS);
    }

    @Override
    public boolean ping() {
        if (_blockingStub == null) return false;

        try {
            _blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).ping(PingRequest.newBuilder().build());
            return true;
        } catch (StatusRuntimeException e) {
            _logger.warn("Unable to connect ping NodeCore at this time");
            return false;
        }
    }

    @Override
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
                instruction.endorsedBlockContextHeaders = reply.getLastKnownBlockContextList().stream()
                        .map(header -> header.getHeader().toByteArray())
                        .collect(Collectors.toList());
            } else {
                instruction.endorsedBlockContextHeaders = new ArrayList<>();
            }

            result.setResult(instruction);
        } else {
            result.setSuccess(false);

            StringBuilder message = new StringBuilder();
            for (nodecore.api.grpc.Result r : reply.getResultsList()) {
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

    @Override
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

    @Override
    public List<PoPEndorsementInfo> getPoPEndorsementInfo() {
        GetPoPEndorsementsInfoRequest request = GetPoPEndorsementsInfoRequest.newBuilder()
                .setSearchLength(750)
                .build();

        GetPoPEndorsementsInfoReply reply = _blockingStub.getPoPEndorsementsInfo(request);
        return reply.getPopEndorsementsList()
                .stream()
                .map(PoPEndorsementInfo::new)
                .collect(Collectors.toList());
    }

    @Override
    public Integer getBitcoinBlockIndex(byte[] blockHeader) {
        GetBitcoinBlockIndexRequest request = GetBitcoinBlockIndexRequest.newBuilder()
                .setBlockHeader(ByteString.copyFrom(blockHeader))
                .setSearchLength(20)
                .build();

        ProtocolReply reply = _blockingStub.getBitcoinBlockIndex(request);
        if (reply.getSuccess() && reply.getResultsCount() > 0) {
            return Integer.parseInt(reply.getResults(0).getDetails());
        }

        return null;
    }

    @Override
    public String getMinerAddress() {
        GetInfoRequest request = GetInfoRequest.newBuilder().build();
        GetInfoReply reply = _blockingStub.getInfo(request);

        return Utility.bytesToBase58(reply.getDefaultAddress().getAddress().toByteArray());
    }

    @Override
    public VeriBlockHeader getLastBlock() {
        GetLastBlockReply reply = _blockingStub.withDeadlineAfter(10, TimeUnit.SECONDS)
                .getLastBlock(GetLastBlockRequest.newBuilder().build());

        return new VeriBlockHeader(reply.getHeader().getHeader().toByteArray());
    }

    @Override
    public Result unlockWallet(String passphrase) {
        UnlockWalletRequest request = UnlockWalletRequest.newBuilder()
                .setPassphrase(passphrase)
                .build();

        ProtocolReply protocolReply = _blockingStub.unlockWallet(request);
        DefaultResult result = new DefaultResult();
        if (!protocolReply.getSuccess()) {
            result.fail();
        }
        for (nodecore.api.grpc.Result r : protocolReply.getResultsList()) {
            result.addMessage(r.getCode(), r.getMessage(), r.getDetails(), r.getError());
        }

        return result;
    }

    @Override
    public Result lockWallet() {
        LockWalletRequest request = LockWalletRequest.newBuilder().build();

        ProtocolReply protocolReply = _blockingStub.lockWallet(request);
        DefaultResult result = new DefaultResult();
        if (!protocolReply.getSuccess()) {
            result.fail();
        }
        for (nodecore.api.grpc.Result r : protocolReply.getResultsList()) {
            result.addMessage(r.getCode(), r.getMessage(), r.getDetails(), r.getError());
        }

        return result;
    }

    @Subscribe public void onNodeCoreConfigurationChanged(NodeCoreConfigurationChangedEvent event) {
        try {
            initializeClient();
        } catch (Exception e) {
            _logger.error(e.getMessage(), e);
        }
    }

    private void poll() {
        if (isHealthy()) {
            VeriBlockHeader latestBlock;
            try {
                latestBlock = getLastBlock();
            } catch (Exception e) {
                _logger.error("NodeCore Error", e);
                healthy.set(false);
                InternalEventBus.getInstance().post(new NodeCoreUnhealthyEvent());
                return;
            }
            VeriBlockHeader chainHead = blockStore.getChainHead();
            if (!latestBlock.equals(chainHead)) {
                blockStore.setChainHead(latestBlock);
                InternalEventBus.getInstance().post(new NewVeriBlockFoundEvent(latestBlock, chainHead));
            }
        } else {
            if (ping()) {
                if (!isHealthy()) {
                    InternalEventBus.getInstance().post(new NodeCoreHealthyEvent());
                }
                healthy.set(true);
            } else {
                if (isHealthy()) {
                    InternalEventBus.getInstance().post(new NodeCoreUnhealthyEvent());
                }
                healthy.set(false);
            }
        }
    }
}
