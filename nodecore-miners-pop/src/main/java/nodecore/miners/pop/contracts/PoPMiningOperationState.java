// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import nodecore.miners.pop.InternalEventBus;
import nodecore.miners.pop.Threading;
import nodecore.miners.pop.common.Utility;
import nodecore.miners.pop.events.FilteredBlockAvailableEvent;
import nodecore.miners.pop.events.PoPMiningOperationCompletedEvent;
import nodecore.miners.pop.events.PoPMiningOperationStateChangedEvent;
import nodecore.miners.pop.events.TransactionConfirmedEvent;
import org.bitcoinj.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.crypto.Crypto;
import org.veriblock.core.utilities.BlockUtility;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PoPMiningOperationState {
    private static final Logger logger = LoggerFactory.getLogger(PoPMiningOperationState.class);

    public enum Action {
        READY,
        PUBLICATION_DATA,
        TRANSACTION,
        WAIT,
        PROOF,
        CONTEXT,
        SUBMIT,
        CONFIRM,
        DONE
    }

    public PoPMiningOperationState(String operationId) {
        this.operationId = operationId;
    }

    public PoPMiningOperationState(String operationId, Integer blockNumber) {
        this.operationId = operationId;
        this.blockNumber = blockNumber;
    }

    public boolean isCompleted() {
        return PoPMiningOperationStatus.COMPLETE.equals(status) || PoPMiningOperationStatus.FAILED.equals(status);
    }

    public void begin() {
        List<String> messages = new ArrayList<>();

        messages.add(setStatus(PoPMiningOperationStatus.RUNNING));
        messages.add(setCurrentAction(Action.PUBLICATION_DATA));

        broadcast(messages);
    }

    public void onReceivedMiningInstructions(PoPMiningInstruction instructions) {
        List<String> messages = new ArrayList<>();

        setMiningInstruction(instructions);
        messages.add("Received PoP mining instructions, securing block: " + instructions.getBlockHashAsString());

        setBlockNumber(BlockUtility.extractBlockHeightFromBlockHeader(instructions.endorsedBlockHeader));
        messages.add("Using miner address: " + instructions.getMinerAddress());

        messages.add(setCurrentAction(Action.TRANSACTION));

        broadcast(messages);
    }

    public void onTransactionCreated(Transaction transaction) {
        List<String> messages = new ArrayList<>();

        ExpTransaction exposedTransaction = new ExpTransaction(NetworkParameters.fromID(NetworkParameters.ID_TESTNET), transaction.unsafeBitcoinSerialize());

        setTransaction(transaction);

        byte[] txBytes = exposedTransaction.getFilteredTransaction();

        logger.info("TxID: " + Utility.bytesToHex(transaction.getTxId().getBytes()));
        logger.info("wTxID: " + Utility.bytesToHex(transaction.getWTxId().getBytes()));
        logger.info("Recalculated TxID: " + Utility.bytesToHex(Utility.flip(new Crypto().SHA256D(txBytes))));

        logger.info("Unfiltered Tx: " + Utility.bytesToHex(transaction.unsafeBitcoinSerialize()));
        logger.info("Filtered Tx: " + Utility.bytesToHex(txBytes));


        setTransactionBytes(txBytes);
        messages.add("Signed Bitcoin transaction: " + Utility.bytesToHex(txBytes));

        setSubmittedTransactionId(transaction.getTxId().toString());
        messages.add("Submitted Bitcoin transaction: " + transaction.getTxId().toString());

        setTransactionStatus(TransactionStatus.UNCONFIRMED);
        messages.add("Transaction status: UNCONFIRMED");

        messages.add(setCurrentAction(Action.WAIT));

        registerListeners(transaction);

        broadcast(messages);
    }

    public void onTransactionAppearedInBestChainBlock(Block block) {
        List<String> messages = new ArrayList<>();

        setBitcoinBlockHeaderOfProof(block);
        setBitcoinBlockHeaderOfProofBytes(block.bitcoinSerialize());
        messages.add(String.format("Found transaction in Bitcoin block '%s'", block.getHashAsString()));

        setTransactionStatus(TransactionStatus.CONFIRMED);
        messages.add("Transaction status: CONFIRMED");

        messages.add(setCurrentAction(Action.PROOF));

        broadcast(messages);
    }

    public void registerFilteredBlockListener(ListenableFuture<FilteredBlock> filteredBlockFuture) {
        PoPMiningOperationState self = this;
        Futures.addCallback(filteredBlockFuture, new FutureCallback<FilteredBlock>() {
            @Override
            public void onSuccess(@Nullable FilteredBlock result) {
                InternalEventBus.getInstance().post(new FilteredBlockAvailableEvent(self));
            }

            @Override
            public void onFailure(Throwable t) {
                fail(t.getMessage());
            }
        }, Threading.TASK_POOL);
    }

    public void onBitcoinReorganize() {
        setTransactionStatus(TransactionStatus.UNCONFIRMED);
        setMerklePath(null);
        setBitcoinBlockHeaderOfProof(null);
        setBitcoinBlockHeaderOfProofBytes(null);
        setBitcoinContextBlocks(null);
        setBitcoinContextBlocksBytes(null);
        setPopTransactionId(null);

        List<String> messages = new ArrayList<>();
        messages.add(setCurrentAction(Action.WAIT));

        broadcast(messages);
    }

    public void onTransactionProven(String compactMerklePath) {
        List<String> messages = new ArrayList<>();

        setMerklePath(compactMerklePath);
        messages.add("Found Merkle path to transaction: " + compactMerklePath);

        messages.add(setCurrentAction(Action.CONTEXT));

        broadcast(messages);
    }

    public void onBitcoinContextDetermined(List<Block> context) {
        List<String> messages = new ArrayList<>();

        setBitcoinContextBlocks(context);
        setBitcoinContextBlocksBytes(context.stream().map(Block::bitcoinSerialize).collect(Collectors.toList()));
        messages.add(String.format("Added %d context headers", context.size()));

        messages.add(setCurrentAction(Action.SUBMIT));

        broadcast(messages);
    }

    public void onPoPTransactionSubmitted(String popTransactionId) {
        List<String> messages = new ArrayList<>();

        setPopTransactionId(popTransactionId);
        messages.add("Submitted PoP transaction: " + popTransactionId);

        messages.add(setCurrentAction(Action.CONFIRM));

        broadcast(messages);
    }

    public void complete() {
        unregisterListeners(getTransaction());

        List<String> messages = new ArrayList<>();

        messages.add(setCurrentAction(Action.DONE));
        messages.add(setStatus(PoPMiningOperationStatus.COMPLETE));

        broadcast(messages);
        InternalEventBus.getInstance().post(new PoPMiningOperationCompletedEvent(_this.getOperationId()));
    }

    public void fail(String reason) {
        unregisterListeners(getTransaction());

        List<String> messages = new ArrayList<>();

        setMessage(reason);
        messages.add(reason);
        messages.add(setCurrentAction(Action.DONE));
        messages.add(setStatus(PoPMiningOperationStatus.FAILED));

        broadcast(messages);
    }

    public static PoPMiningOperationStateBuilder newBuilder() {
        return new PoPMiningOperationStateBuilder();
    }

    private void broadcast(List<String> messages) {
        PoPMiningOperationStateChangedEvent event = new PoPMiningOperationStateChangedEvent(this, messages);
        InternalEventBus.getInstance().post(event);
    }

    public void registerListeners(Transaction transaction) {
        transaction.getConfidence().addEventListener(txConfidenceListener);

        futureDepthListener = transaction.getConfidence().getDepthFuture(20);
        Futures.addCallback(futureDepthListener, new FutureCallback<TransactionConfidence>() {
            @Override
            public void onSuccess(@Nullable TransactionConfidence result) {
                if (result != null) {
                    logger.info("[{}] Transaction has reached depth: {}", _this.getOperationId(), result.getDepthInBlocks());
                }
                _this.complete();
            }

            @Override
            public void onFailure(Throwable t) {
                logger.error("Failure in depth future callback", t);
                _this.complete();
            }
        }, Threading.TASK_POOL);
    }

    private void unregisterListeners(Transaction transaction) {
        if (transaction == null) {
            return;
        }

        transaction.getConfidence().removeEventListener(txConfidenceListener);

        if (futureDepthListener != null && !futureDepthListener.isDone()) {
            futureDepthListener.cancel(false);
        }
    }

    // Setters and Getters for data

    private PoPMiningOperationState _this = this;
    private TransactionConfidence.Listener txConfidenceListener = (confidence, reason) -> {
        logger.info("[{}] TransactionConfidence listener called: {} - {}",
                _this.getOperationId(),
                reason.name(),
                confidence.getConfidenceType().name());
        if (reason != TransactionConfidence.Listener.ChangeReason.TYPE) {
            return;
        }

        if (confidence.getConfidenceType() == TransactionConfidence.ConfidenceType.PENDING) {
            onBitcoinReorganize();
        } else if (confidence.getConfidenceType() == TransactionConfidence.ConfidenceType.BUILDING) {
            InternalEventBus.getInstance().post(new TransactionConfirmedEvent(_this));
        }
    };
    private ListenableFuture<TransactionConfidence> futureDepthListener;

    private String operationId;

    public String getOperationId() {
        return operationId;
    }

    private Integer blockNumber;

    public Integer getBlockNumber() {
        return blockNumber;
    }

    private void setBlockNumber(Integer value) {
        blockNumber = value;
    }

    private PoPMiningOperationStatus status;

    public PoPMiningOperationStatus getStatus() {
        return status;
    }

    private String setStatus(PoPMiningOperationStatus value) {
        if (value != null && !value.equals(status)) {
            status = value;
        }
        return "Mining operation is now: " + value;
    }

    private PoPMiningOperationState.Action currentAction;

    public Action getCurrentAction() {
        return currentAction;
    }

    public String getCurrentActionAsString() {
        switch (currentAction) {
            case READY:
                return "Ready";
            case PUBLICATION_DATA:
                return "Getting PoP publication data";
            case TRANSACTION:
                return "Creating Bitcoin transaction containing PoP publication data";
            case WAIT:
                return "Waiting for transaction to be included in Bitcoin block";
            case PROOF:
                return "Proving transaction included in Bitcoin block";
            case CONTEXT:
                return "Building Bitcoin context";
            case SUBMIT:
                return "Submitting completed PoP transaction";
            case CONFIRM:
                return "Confirming Bitcoin transaction";
            case DONE:
                return "Done";
        }

        return "";
    }

    private String setCurrentAction(Action value) {
        if (currentAction == null || !currentAction.equals(value)) {
            currentAction = value;
        }
        return "Mining operation current action: " + getCurrentActionAsString();
    }

    private PoPMiningInstruction miningInstruction;

    public PoPMiningInstruction getMiningInstruction() {
        return miningInstruction;
    }

    private void setMiningInstruction(PoPMiningInstruction value) {
        miningInstruction = value;
    }

    private byte[] transactionBytes;

    public byte[] getTransactionBytes() {
        return transactionBytes;
    }

    private void setTransactionBytes(byte[] value) {
        transactionBytes = value;
    }

    private Transaction transaction;

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction value) {
        transaction = value;
    }

    private String submittedTransactionId;

    public String getSubmittedTransactionId() {
        return submittedTransactionId;
    }

    private void setSubmittedTransactionId(String value) {
        submittedTransactionId = value;
    }

    private TransactionStatus transactionStatus;

    public TransactionStatus getTransactionStatus() {
        return transactionStatus;
    }

    private void setTransactionStatus(TransactionStatus value) {
        transactionStatus = value;
    }

    private byte[] bitcoinBlockHeaderOfProofBytes;

    public byte[] getBitcoinBlockHeaderOfProofBytes() {
        return bitcoinBlockHeaderOfProofBytes;
    }

    private void setBitcoinBlockHeaderOfProofBytes(byte[] value) {
        bitcoinBlockHeaderOfProofBytes = value;
    }

    private Block bitcoinBlockHeaderOfProof;

    public Block getBitcoinBlockHeaderOfProof() {
        return bitcoinBlockHeaderOfProof;
    }

    public void setBitcoinBlockHeaderOfProof(Block value) {
        bitcoinBlockHeaderOfProof = value;
    }

    private List<byte[]> bitcoinContextBlocksBytes;

    public List<byte[]> getBitcoinContextBlocksBytes() {
        return bitcoinContextBlocksBytes;
    }

    private void setBitcoinContextBlocksBytes(List<byte[]> value) {
        this.bitcoinContextBlocksBytes = value;
    }

    private List<Block> bitcoinContextBlocks;

    public List<Block> getBitcoinContextBlocks() {
        return bitcoinContextBlocks;
    }

    public void setBitcoinContextBlocks(List<Block> value) {
        bitcoinContextBlocks = value;
    }

    private String merklePath;

    public String getMerklePath() {
        return merklePath;
    }

    private void setMerklePath(String value) {
        merklePath = value;
    }

    private String popTransactionId;

    public String getPopTransactionId() {
        return popTransactionId;
    }

    private void setPopTransactionId(String value) {
        popTransactionId = value;
    }

    private String message;

    public String getMessage() {
        if (message == null) {
            return "";
        }

        return message;
    }

    private void setMessage(String value) {
        message = value;
    }

    public static class PoPMiningOperationStateBuilder {
        private String operationId_;
        private PoPMiningOperationStatus status_;
        private PoPMiningOperationState.Action currentAction_;
        private Integer blockNumber_;
        private PoPMiningInstruction miningInstruction_;
        private byte[] transaction_;
        private String submittedTransactionId_;
        private byte[] bitcoinBlockHeaderOfProof_;
        private List<byte[]> bitcoinContextBlocks_;
        private String merklePath_;
        private String popTransactionId_;
        private String message_;
        private TransactionStatus transactionStatus_;

        public PoPMiningOperationStateBuilder setOperationId(String value) {
            operationId_ = value;
            return this;
        }

        public PoPMiningOperationStateBuilder setStatus(PoPMiningOperationStatus value) {
            status_ = value;
            return this;
        }

        public PoPMiningOperationStateBuilder parseStatus(String value) {
            switch (value) {
                case "RUNNING":
                    status_ = PoPMiningOperationStatus.RUNNING;
                    break;
                case "COMPLETE":
                    status_ = PoPMiningOperationStatus.COMPLETE;
                    break;
                case "FAILED":
                    status_ = PoPMiningOperationStatus.FAILED;
                    break;
            }
            return this;
        }

        public PoPMiningOperationStateBuilder setCurrentAction(Action value) {
            currentAction_ = value;
            return this;
        }

        public PoPMiningOperationStateBuilder parseCurrentAction(String value) {
            switch (value) {
                case "READY":
                    currentAction_ = Action.READY;
                    break;
                case "PUBLICATION_DATA":
                    currentAction_ = Action.PUBLICATION_DATA;
                    break;
                case "TRANSACTION":
                    currentAction_ = Action.TRANSACTION;
                    break;
                case "WAIT":
                    currentAction_ = Action.WAIT;
                    break;
                case "PROOF":
                    currentAction_ = Action.PROOF;
                    break;
                case "CONTEXT":
                    currentAction_ = Action.CONTEXT;
                    break;
                case "SUBMIT":
                    currentAction_ = Action.SUBMIT;
                    break;
                case "CONFIRM":
                    currentAction_ = Action.CONFIRM;
                    break;
                case "DONE":
                    currentAction_ = Action.DONE;
                    break;
            }
            return this;
        }

        public PoPMiningOperationStateBuilder setBlockNumber(Integer value) {
            blockNumber_ = value;
            return this;
        }

        public PoPMiningOperationStateBuilder setMiningInstruction(PoPMiningInstruction value) {
            miningInstruction_ = value;
            return this;
        }

        public PoPMiningOperationStateBuilder setTransaction(byte[] value) {
            transaction_ = value;
            return this;
        }

        public PoPMiningOperationStateBuilder setSubmittedTransactionId(String value) {
            submittedTransactionId_ = value;
            return this;
        }

        public PoPMiningOperationStateBuilder setBitcoinBlockHeaderOfProof(byte[] value) {
            bitcoinBlockHeaderOfProof_ = value;
            return this;
        }

        public PoPMiningOperationStateBuilder setBitcoinContextBlocks(List<byte[]> value) {
            bitcoinContextBlocks_ = value;
            return this;
        }

        public PoPMiningOperationStateBuilder setMerklePath(String value) {
            merklePath_ = value;
            return this;
        }

        public PoPMiningOperationStateBuilder setPopTransactionId(String value) {
            popTransactionId_ = value;
            return this;
        }

        public PoPMiningOperationStateBuilder setMessage(String value) {
            message_ = value;
            return this;
        }

        public PoPMiningOperationStateBuilder parseTransactionStatus(String value) {
            switch (value) {
                case "UNCONFIRMED":
                    transactionStatus_ = TransactionStatus.UNCONFIRMED;
                    break;
                case "CONFIRMED":
                    transactionStatus_ = TransactionStatus.CONFIRMED;
                    break;
                case "DEAD":
                    transactionStatus_ = TransactionStatus.DEAD;
                    break;
            }

            return this;
        }

        public PoPMiningOperationState build() {
            PoPMiningOperationState state = new PoPMiningOperationState(operationId_);
            state.setStatus(status_);
            state.setCurrentAction(currentAction_);
            state.setBlockNumber(blockNumber_);
            state.setMiningInstruction(miningInstruction_);
            state.setTransactionBytes(transaction_);
            state.setSubmittedTransactionId(submittedTransactionId_);
            state.setBitcoinBlockHeaderOfProofBytes(bitcoinBlockHeaderOfProof_);
            state.setBitcoinContextBlocksBytes(bitcoinContextBlocks_);
            state.setMerklePath(merklePath_);
            state.setPopTransactionId(popTransactionId_);
            state.setMessage(message_);
            state.setTransactionStatus(transactionStatus_);

            return state;
        }
    }
}
