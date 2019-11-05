// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.services;

import com.google.common.eventbus.Subscribe;
import com.google.protobuf.ByteString;
import nodecore.miners.pop.InternalEventBus;
import nodecore.miners.pop.contracts.PoPMiningInstruction;
import nodecore.miners.pop.contracts.PoPMiningOperationState;
import nodecore.miners.pop.contracts.PoPRepository;
import nodecore.miners.pop.events.PoPMiningOperationStateChangedEvent;
import nodecore.miners.pop.storage.OperationStateData;
import nodecore.miners.pop.storage.ProofOfProof;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.utilities.Utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class PoPStateService {
    private static final Logger logger = LoggerFactory.getLogger(PoPStateService.class);

    private final PoPRepository repository;

    public PoPStateService(PoPRepository repository) {
        this.repository = repository;
        InternalEventBus.getInstance().register(this);
    }

    public List<PoPMiningOperationState> getActiveOperations() {
        List<PoPMiningOperationState> operations = new ArrayList<>();

        Iterator<OperationStateData> activeOperations = repository.getActiveOperations();
        if (activeOperations != null) {
            while (activeOperations.hasNext()) {
                OperationStateData stateData = activeOperations.next();
                try {
                    PoPMiningOperationState state = reconstitute(ProofOfProof.OperationState.parseFrom(stateData.state));
                    if (state != null) {
                        operations.add(state);
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        return operations;
    }

    public PoPMiningOperationState getOperation(String id) {
        OperationStateData stateData = repository.getOperation(id);
        try {
            PoPMiningOperationState state = reconstitute(ProofOfProof.OperationState.parseFrom(stateData.state));
            if (state != null) {
                return state;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Subscribe
    public void onPoPMiningOperationStateChanged(PoPMiningOperationStateChangedEvent event) {
        try {
            PoPMiningOperationState operationState = event.getState();
            byte[] serializedState = serialize(event.getState());

            OperationStateData stateData = new OperationStateData();
            stateData.id = operationState.getOperationId();
            stateData.status = operationState.getStatus().name();
            stateData.action = operationState.getCurrentActionAsString();
            stateData.transactionStatus = operationState.getTransactionStatus() != null ?
                    operationState.getTransactionStatus().name() :
                    "";
            stateData.message = operationState.getMessage();
            stateData.state = serializedState;
            stateData.isDone = PoPMiningOperationState.Action.DONE.equals(operationState.getCurrentAction());
            stateData.lastUpdated = Utility.getCurrentTimeSeconds();

            if (operationState.getMiningInstruction() != null) {
                stateData.endorsedBlockHash = operationState.getMiningInstruction().getBlockHashAsString();
                stateData.endorsedBlockNumber = operationState.getBlockNumber();
            }

            repository.saveOperationState(stateData);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private byte[] serialize(PoPMiningOperationState operationState) {
        ProofOfProof.OperationState.Builder builder = ProofOfProof.OperationState.newBuilder();

        builder.setId(operationState.getOperationId());
        builder.setStatus(operationState.getStatus().name());
        builder.setAction(operationState.getCurrentAction().name());

        if (operationState.getBlockNumber() != null) {
            builder.setEndorsedBlockNumber(operationState.getBlockNumber());
        } else {
            builder.setEndorsedBlockNumber(-1);
        }

        if (operationState.getMiningInstruction() != null) {
            builder.setMiningInstructions(ProofOfProof.MiningInstruction.newBuilder()
                    .setPublicationData(ByteString.copyFrom(operationState.getMiningInstruction().publicationData))
                    .setEndorsedBlockHeader(ByteString.copyFrom(operationState.getMiningInstruction().endorsedBlockHeader))
                    .setLastBitcoinBlock(ByteString.copyFrom(operationState.getMiningInstruction().lastBitcoinBlock))
                    .setMinerAddress(ByteString.copyFrom(operationState.getMiningInstruction().minerAddress))
                    .addAllBitcoinContextAtEndorsed(operationState.getMiningInstruction().endorsedBlockContextHeaders.stream()
                            .map(ByteString::copyFrom)
                            .collect(Collectors.toList()))
                    .build());
        }

        if (operationState.getTransactionBytes() != null) {
            builder.setTransaction(ByteString.copyFrom(operationState.getTransactionBytes()));
            builder.setBitcoinTxId(operationState.getSubmittedTransactionId());
        }

        if (operationState.getBitcoinBlockHeaderOfProofBytes() != null) {
            builder.setBlockOfProof(ByteString.copyFrom(operationState.getBitcoinBlockHeaderOfProofBytes()));
        }

        if (operationState.getBitcoinContextBlocksBytes() != null) {
            builder.addAllBitcoinContext(operationState.getBitcoinContextBlocksBytes().stream()
                    .map(ByteString::copyFrom)
                    .collect(Collectors.toList()));
        }

        if (operationState.getMerklePath() != null) {
            builder.setMerklePath(operationState.getMerklePath());
        }

        if (operationState.getPopTransactionId() != null) {
            builder.setPopTxId(operationState.getPopTransactionId());
        }

        if (operationState.getMessage() != null) {
            builder.setMessage(operationState.getMessage());
        }

        if (operationState.getTransactionStatus() != null) {
            builder.setTransactionStatus(operationState.getTransactionStatus().name());
        }

        return serialize(builder.build());
    }

    private byte[] serialize(ProofOfProof.OperationState state) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            state.writeTo(stream);

            return stream.toByteArray();
        } catch (IOException ignored) {}

        return null;
    }

    private PoPMiningOperationState reconstitute(ProofOfProof.OperationState state) {
        if (state == null) return null;

        logger.info("Reconstituting operation {}", state.getId());

        PoPMiningOperationState.PoPMiningOperationStateBuilder builder = PoPMiningOperationState.newBuilder()
                .setOperationId(state.getId())
                .parseStatus(state.getStatus())
                .parseCurrentAction(state.getAction());

        if (state.getEndorsedBlockNumber() >= 0) {
            builder.setBlockNumber(state.getEndorsedBlockNumber());
        }

        if (state.getMiningInstructions() != null) {
            PoPMiningInstruction miningInstruction = new PoPMiningInstruction();
            miningInstruction.publicationData = state.getMiningInstructions().getPublicationData().toByteArray();
            miningInstruction.endorsedBlockHeader = state.getMiningInstructions().getEndorsedBlockHeader().toByteArray();
            miningInstruction.lastBitcoinBlock = state.getMiningInstructions().getLastBitcoinBlock().toByteArray();
            miningInstruction.minerAddress = state.getMiningInstructions().getMinerAddress().toByteArray();
            miningInstruction.endorsedBlockContextHeaders = state.getMiningInstructions().getBitcoinContextAtEndorsedList()
                    .stream()
                    .map(ByteString::toByteArray)
                    .collect(Collectors.toList());

            builder.setMiningInstruction(miningInstruction);
        }

        if (state.getTransaction() != null && state.getTransaction().size() > 0) {
            builder.setTransaction(state.getTransaction().toByteArray());
            builder.setSubmittedTransactionId(state.getBitcoinTxId());
        }

        if (state.getBlockOfProof() != null && state.getBlockOfProof().size() > 0) {
            builder.setBitcoinBlockHeaderOfProof(state.getBlockOfProof().toByteArray());
        }

        if (state.getBitcoinContextList() != null && state.getBitcoinContextCount() > 0) {
            builder.setBitcoinContextBlocks(state.getBitcoinContextList().stream()
                    .map(ByteString::toByteArray)
                    .collect(Collectors.toList()));
        }

        if (state.getMerklePath() != null) {
            builder.setMerklePath(state.getMerklePath());
        }

        if (state.getPopTxId() != null) {
            builder.setPopTransactionId(state.getPopTxId());
        }

        if (state.getMessage() != null) {
            builder.setMessage(state.getMessage());
        }

        if (state.getTransactionStatus() != null) {
            builder.parseTransactionStatus(state.getTransactionStatus());
        }

        return builder.build();
    }
}
