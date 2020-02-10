// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.tasks;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import nodecore.miners.pop.InternalEventBus;
import nodecore.miners.pop.Threading;
import nodecore.miners.pop.contracts.ApplicationExceptions;
import nodecore.miners.pop.contracts.BaseTask;
import nodecore.miners.pop.contracts.PoPMiningOperationState;
import nodecore.miners.pop.contracts.TaskResult;
import nodecore.miners.pop.events.InsufficientFundsEvent;
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
    protected TaskResult executeImpl(PoPMiningOperationState state) {
        if (state.getTransaction() != null) {
            return TaskResult.succeed(state, getNext());
        }

        Script opReturnScript = bitcoinService.generatePoPScript(state.getMiningInstruction().publicationData);

        try {
            ListenableFuture<Transaction> txFuture = bitcoinService.createPoPTransaction(opReturnScript);
            Futures.addCallback(txFuture, new FutureCallback<Transaction>() {
                @Override
                public void onSuccess(@Nullable Transaction result) {
                    if (result != null) {
                        logger.info("Successfully broadcast transaction {}", result.getTxId());
                        state.onTransactionCreated(result);
                    } else {
                        logger.error("Create Bitcoin transaction returned no transaction");
                        failProcess(state, "Create Bitcoin transaction returned no transaction");
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    logger.error("A problem occurred broadcasting the transaction to the peer group", t);
                    failProcess(state, "A problem occurred broadcasting the transaction to the peer group");
                }
            }, Threading.TASK_POOL);
        } catch (ApplicationExceptions.SendTransactionException e) {
            handleSendTransactionExceptions(e, state);
        }

        return TaskResult.succeed(state, getNext());
    }

    private void handleSendTransactionExceptions(ApplicationExceptions.SendTransactionException container, PoPMiningOperationState state) {
        for (Throwable e : container.getSuppressed()) {
            if (e instanceof ApplicationExceptions.UnableToAcquireTransactionLock) {
                logger.info("A previous transaction has not yet completed broadcasting to peers and new transactions would result in double spending");
                failProcess(state,
                        "A previous transaction has not yet completed broadcasting to peers and new transactions would result in double spending. Wait a few seconds and try again.");
            } else if (e instanceof InsufficientMoneyException) {
                logger.info(e.getMessage());
                failProcess(state, "PoP wallet does not contain sufficient funds to create PoP transaction");
                InternalEventBus.getInstance().post(new InsufficientFundsEvent());
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
