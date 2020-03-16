// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.tasks

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import nodecore.miners.pop.Threading
import nodecore.miners.pop.core.MiningOperation
import nodecore.miners.pop.core.OperationState
import nodecore.miners.pop.events.EventBus.insufficientFundsEvent
import nodecore.miners.pop.model.ApplicationExceptions.DuplicateTransactionException
import nodecore.miners.pop.model.ApplicationExceptions.ExceededMaxTransactionFee
import nodecore.miners.pop.model.ApplicationExceptions.SendTransactionException
import nodecore.miners.pop.model.ApplicationExceptions.UnableToAcquireTransactionLock
import nodecore.miners.pop.model.TaskResult
import nodecore.miners.pop.model.TaskResult.Companion.succeed
import nodecore.miners.pop.services.BitcoinService
import nodecore.miners.pop.services.NodeCoreService
import org.bitcoinj.core.InsufficientMoneyException
import org.bitcoinj.core.Transaction
import org.bitcoinj.wallet.Wallet

/**
 * Second task that will be executed in a mining operation
 */
class CreateBitcoinTransactionTask(
    nodeCoreService: NodeCoreService,
    bitcoinService: BitcoinService
) : BaseTask(
    nodeCoreService, bitcoinService
) {
    override val next: BaseTask?
        get() = null

    override fun executeImpl(operation: MiningOperation): TaskResult {
        if (operation.state is OperationState.EndorsementTransaction) {
            return succeed(operation, next)
        }
        val miningInstruction = (operation.state as OperationState.Instruction).miningInstruction
        val opReturnScript = bitcoinService.generatePoPScript(miningInstruction.publicationData)
        try {
            val txFuture = bitcoinService.createPoPTransaction(
                opReturnScript
            )
            Futures.addCallback(
                txFuture, object : FutureCallback<Transaction?> {
                override fun onSuccess(result: Transaction?) {
                    if (result != null) {
                        logger.info("Successfully broadcast transaction {}", result.txId)
                        operation.setTransaction(result)
                    } else {
                        logger.error("Create Bitcoin transaction returned no transaction")
                        failProcess(operation, "Create Bitcoin transaction returned no transaction")
                    }
                }

                override fun onFailure(t: Throwable) {
                    logger.error("A problem occurred broadcasting the transaction to the peer group", t)
                    failProcess(operation, "A problem occurred broadcasting the transaction to the peer group")
                }
            }, Threading.TASK_POOL
            )
        } catch (e: SendTransactionException) {
            handleSendTransactionExceptions(e, operation)
        }
        return succeed(operation, next)
    }

    private fun handleSendTransactionExceptions(container: SendTransactionException, state: MiningOperation) {
        for (e in container.suppressed) {
            if (e is UnableToAcquireTransactionLock) {
                logger
                    .info("A previous transaction has not yet completed broadcasting to peers and new transactions would result in double spending")
                failProcess(
                    state,
                    "A previous transaction has not yet completed broadcasting to peers and new transactions would result in double spending. Wait a few seconds and try again."
                )
            } else if (e is InsufficientMoneyException) {
                logger.info(e.message)
                failProcess(state, "PoP wallet does not contain sufficient funds to create PoP transaction")
                insufficientFundsEvent.trigger()
            } else if (e is ExceededMaxTransactionFee) {
                failProcess(state, "Calculated fee exceeded configured maximum transaction fee")
            } else if (e is DuplicateTransactionException) {
                failProcess(
                    state,
                    "Transaction appears identical to a previously broadcast transaction. Often this occurs when there is a 'too-long-mempool-chain'."
                )
            } else if (e is Wallet.CompletionException) {
                logger.error(e.javaClass.simpleName, e)
                failProcess(state, "Unable to complete transaction: " + e.javaClass.simpleName)
            } else {
                logger.error(e.message, e)
                failProcess(state, "Unable to send transaction: " + e.message)
            }
        }
    }
}
