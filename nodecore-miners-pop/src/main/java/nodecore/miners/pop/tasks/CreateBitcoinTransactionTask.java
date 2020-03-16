// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.tasks;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import nodecore.miners.pop.Threading;
import nodecore.miners.pop.core.MiningOperation;
import nodecore.miners.pop.core.OperationState;
import nodecore.miners.pop.events.EventBus;
import nodecore.miners.pop.model.ApplicationExceptions;
import nodecore.miners.pop.model.PopMiningInstruction;
import nodecore.miners.pop.model.TaskResult;
import nodecore.miners.pop.services.BitcoinService;
import nodecore.miners.pop.services.NodeCoreService;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.Wallet;

import javax.annotation.Nullable;

/**
 * Second task that will be executed in a mining operation
 */
public class CreateBitcoinTransactionTask extends BaseTask {
    @Override
    public BaseTask getNext() {
        return null;
    }

    public CreateBitcoinTransactionTask(NodeCoreService nodeCoreService, BitcoinService bitcoinService) {
        super(nodeCoreService, bitcoinService);
    }

    @Override
    protected TaskResult executeImpl(MiningOperation operation) {
        if (operation.getState() instanceof OperationState.EndorsementTransaction) {
            return TaskResult.succeed(operation, getNext());
        }

        PopMiningInstruction miningInstruction = ((OperationState.Instruction) operation.getState()).getMiningInstruction();

        Script opReturnScript = bitcoinService.generatePoPScript(miningInstruction.publicationData);

        try {
            ListenableFuture<Transaction> txFuture = bitcoinService.createPoPTransaction(opReturnScript);
            Futures.addCallback(txFuture, new FutureCallback<Transaction>() {
                @Override
                public void onSuccess(@Nullable Transaction result) {
                    if (result != null) {
                        logger.info("Successfully broadcast transaction {}", result.getTxId());
                        operation.setTransaction(result);
                    } else {
                        logger.error("Create Bitcoin transaction returned no transaction");
                        failProcess(operation, "Create Bitcoin transaction returned no transaction");
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    logger.error("A problem occurred broadcasting the transaction to the peer group", t);
                    failProcess(operation, "A problem occurred broadcasting the transaction to the peer group");
                }
            }, Threading.TASK_POOL);
        } catch (ApplicationExceptions.SendTransactionException e) {
            handleSendTransactionExceptions(e, operation);
        }

        return TaskResult.succeed(operation, getNext());
    }

    private void handleSendTransactionExceptions(ApplicationExceptions.SendTransactionException container, MiningOperation state) {
        for (Throwable e : container.getSuppressed()) {
            if (e instanceof ApplicationExceptions.UnableToAcquireTransactionLock) {
                logger
                    .info("A previous transaction has not yet completed broadcasting to peers and new transactions would result in double spending");
                failProcess(
                    state,
                    "A previous transaction has not yet completed broadcasting to peers and new transactions would result in double spending. Wait a few seconds and try again."
                );
            } else if (e instanceof InsufficientMoneyException) {
                logger.info(e.getMessage());
                failProcess(state, "PoP wallet does not contain sufficient funds to create PoP transaction");
                EventBus.INSTANCE.getInsufficientFundsEvent().trigger();
            } else if (e instanceof ApplicationExceptions.ExceededMaxTransactionFee) {
                failProcess(state, "Calculated fee exceeded configured maximum transaction fee");
            } else if (e instanceof ApplicationExceptions.DuplicateTransactionException) {
                failProcess(state,
                        "Transaction appears identical to a previously broadcast transaction. Often this occurs when there is a 'too-long-mempool-chain'.");
            } else if (e instanceof Wallet.CompletionException) {
                logger.error(e.getClass().getSimpleName(), e);
                failProcess(state, "Unable to complete transaction: " + e.getClass().getSimpleName());
            } else {
                logger.error(e.getMessage(), e);
                failProcess(state, "Unable to send transaction: " + e.getMessage());
            }
        }
    }
}
