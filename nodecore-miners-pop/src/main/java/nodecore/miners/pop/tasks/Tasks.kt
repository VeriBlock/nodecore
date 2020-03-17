package nodecore.miners.pop.tasks

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import io.grpc.StatusRuntimeException
import nodecore.miners.pop.Threading
import nodecore.miners.pop.common.BitcoinMerklePath
import nodecore.miners.pop.common.BitcoinMerkleTree
import nodecore.miners.pop.common.MerkleProof
import nodecore.miners.pop.common.Utility
import nodecore.miners.pop.core.MiningOperation
import nodecore.miners.pop.core.OperationState
import nodecore.miners.pop.events.EventBus
import nodecore.miners.pop.model.ApplicationExceptions
import nodecore.miners.pop.model.PopMiningTransaction
import nodecore.miners.pop.model.TaskResult
import nodecore.miners.pop.services.BitcoinService
import nodecore.miners.pop.services.NodeCoreService
import org.bitcoinj.core.Block
import org.bitcoinj.core.FilteredBlock
import org.bitcoinj.core.InsufficientMoneyException
import org.bitcoinj.core.Transaction
import org.bitcoinj.wallet.Wallet
import java.util.ArrayList
import java.util.Arrays

/**
 * First task that will be executed in a mining operation
 */
class GetPoPInstructionsTask(
    nodeCoreService: NodeCoreService,
    bitcoinService: BitcoinService
) : BaseTask(
    nodeCoreService, bitcoinService
) {
    override val next: BaseTask
        get() = CreateBitcoinTransactionTask(nodeCoreService, bitcoinService)

    override fun executeImpl(operation: MiningOperation): TaskResult {
        if (operation.state is OperationState.Instruction) {
            return TaskResult.succeed(operation, next)
        }

        /* Get the PoPMiningInstruction, consisting of the 80 bytes of data that VeriBlock will pay the PoP miner
         * to publish to Bitcoin (includes 64-byte VB header and 16-byte miner ID) as well as the
         * PoP miner's address
         */
        return try {
            val popReply = nodeCoreService.getPop(operation.blockHeight)
            if (popReply.success) {
                operation.setMiningInstruction(popReply.result!!)
                TaskResult.succeed(operation, next)
            } else {
                failProcess(operation, popReply.resultMessage!!)
            }
        } catch (e: StatusRuntimeException) {
            failProcess(operation, "Failed to get PoP publication data from NodeCore: " + e.status)
        }
    }
}

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
            return TaskResult.succeed(operation, next)
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
        } catch (e: ApplicationExceptions.SendTransactionException) {
            handleSendTransactionExceptions(e, operation)
        }
        return TaskResult.succeed(operation, next)
    }

    private fun handleSendTransactionExceptions(container: ApplicationExceptions.SendTransactionException, state: MiningOperation) {
        for (e in container.suppressed) {
            if (e is ApplicationExceptions.UnableToAcquireTransactionLock) {
                logger
                    .info("A previous transaction has not yet completed broadcasting to peers and new transactions would result in double spending")
                failProcess(
                    state,
                    "A previous transaction has not yet completed broadcasting to peers and new transactions would result in double spending. Wait a few seconds and try again."
                )
            } else if (e is InsufficientMoneyException) {
                logger.info(e.message)
                failProcess(state, "PoP wallet does not contain sufficient funds to create PoP transaction")
                EventBus.insufficientFundsEvent.trigger()
            } else if (e is ApplicationExceptions.ExceededMaxTransactionFee) {
                failProcess(state, "Calculated fee exceeded configured maximum transaction fee")
            } else if (e is ApplicationExceptions.DuplicateTransactionException) {
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

/**
 * Third task that will be executed in a mining operation
 */
class DetermineBlockOfProofTask(
    nodeCoreService: NodeCoreService,
    bitcoinService: BitcoinService
) : BaseTask(
    nodeCoreService, bitcoinService
) {
    override val next: BaseTask?
        get() = null

    override fun executeImpl(operation: MiningOperation): TaskResult {
        if (operation.state !is OperationState.EndorsementTransaction) {
            return TaskResult.fail(operation)
        }
        val state = operation.state as OperationState.EndorsementTransaction

        /*
         * This mechanism is a quick hack to bypass instances where the FilteredBlockAvailableEvent is never
         * passed to the internal event bus, preventing the PoP transaction from progressing.
         */
        Thread(Runnable {
            try {
                Thread.sleep(30000L) // Delay 30 seconds
                if (operation.state is OperationState.BlockOfProof) {
                    // State still hasn't progressed past the PROOF action, meaning a FilteredBlockAvailableEvent
                    // Probably never occurred.
                    logger.info("Forcibly posting false filtered block available event...")
                    EventBus.filteredBlockAvailableEvent.trigger(operation)
                } else {
                    logger.info("Not forcibly posting filtered block available event; state action is not PROOF.")
                }
            } catch (e: Exception) {
                logger.info("Exception occurred in the backup timer for providing alternate proof!")
            }
        }).start()
        val blockAppearances = state.endorsementTransaction.appearsInHashes
        if (blockAppearances != null) {
            val bestBlock = bitcoinService.getBestBlock(blockAppearances.keys)
            if (bestBlock != null) {
                operation.setBlockOfProof(bestBlock)
                Futures.addCallback(
                    bitcoinService.getFilteredBlockFuture(bestBlock.hash),
                    object : FutureCallback<FilteredBlock?> {
                        override fun onSuccess(result: FilteredBlock?) {
                            EventBus.filteredBlockAvailableEvent.trigger(operation)
                        }

                        override fun onFailure(t: Throwable) {
                            operation.fail(t.message!!)
                        }
                    },
                    Threading.TASK_POOL
                )
                return TaskResult.succeed(operation, next)
            }
        }
        return TaskResult.fail(operation)
    }
}

/**
 * Fourth task that will be executed in a mining operation
 */
class ProveTransactionTask(
    nodeCoreService: NodeCoreService,
    bitcoinService: BitcoinService
) : BaseTask(
    nodeCoreService, bitcoinService
) {
    override val next: BaseTask
        get() = BuildContextTask(nodeCoreService, bitcoinService)

    override fun executeImpl(operation: MiningOperation): TaskResult {
        return try {
            val state = operation.state as OperationState.BlockOfProof
            val block = state.blockOfProof
            val partialMerkleTree = bitcoinService.getPartialMerkleTree(block.hash)
            val failureReason = if (partialMerkleTree != null) {
                val proof = MerkleProof.parse(partialMerkleTree)
                if (proof != null) {
                    val path = proof.getCompactPath(state.endorsementTransaction.txId)
                    logger.info("Merkle Proof Compact Path: {}", path)
                    logger.info("Merkle Root: {}", block.merkleRoot.toString())
                    try {
                        val merklePath = BitcoinMerklePath(path)
                        logger.info("Computed Merkle Root: {}", merklePath.getMerkleRoot())
                        if (merklePath.getMerkleRoot().equals(block.merkleRoot.toString(), ignoreCase = true)) {
                            operation.setMerklePath(merklePath.getCompactFormat())
                            return TaskResult.succeed(operation, next)
                        } else {
                            "Block Merkle root does not match computed Merkle root"
                        }
                    } catch (e: Exception) {
                        logger.error("Unable to validate Merkle path for transaction", e)
                        "Unable to prove transaction " + state.endorsementTransaction.txId + " in block " + block.hashAsString
                    }
                } else {
                    "Unable to construct Merkle proof for block " + block.hashAsString
                }
            } else {
                "Unable to retrieve the Merkle tree from the block " + block.hashAsString
            }

            // Retrieving the Merkle path from the PartialMerkleTree failed, try creating manually from the whole block
            logger.info(
                "Unable to calculate the correct Merkle path for transaction " + state.endorsementTransaction.txId.toString() + " in block "
                    + state.blockOfProof.hashAsString + " from a FilteredBlock, trying a fully downloaded block!"
            )
            val fullBlock = bitcoinService.downloadBlock(state.blockOfProof.hash)
            val allTransactions = fullBlock!!.transactions
            val txids: MutableList<String> = ArrayList()
            for (i in allTransactions!!.indices) {
                txids.add(allTransactions[i].txId.toString())
            }
            val bmt = BitcoinMerkleTree(true, txids)
            val merklePath = bmt.getPathFromTxID(state.endorsementTransaction.txId.toString())
            if (merklePath!!.getMerkleRoot().equals(state.blockOfProof.merkleRoot.toString(), ignoreCase = true)) {
                operation.setMerklePath(merklePath.getCompactFormat())
                TaskResult.succeed(operation, next)
            } else {
                logger.error(
                    "Unable to calculate the correct Merkle path for transaction " + state.endorsementTransaction.txId.toString()
                        + " in block " + state.blockOfProof.hashAsString + " from a FilteredBlock or a fully downloaded block!"
                )
                failProcess(operation, failureReason)
            }
        } catch (e: Exception) {
            logger.error(e.message, e)
            failProcess(operation, "Error proving transaction, see log for more detail.")
        }
    }
}

/**
 * Fifth task that will be executed in a mining operation
 */
class BuildContextTask(
    nodeCoreService: NodeCoreService,
    bitcoinService: BitcoinService
) : BaseTask(
    nodeCoreService, bitcoinService
) {
    override val next: BaseTask
        get() = SubmitPoPEndorsementTask(nodeCoreService, bitcoinService)

    override fun executeImpl(operation: MiningOperation): TaskResult {
        return try {
            val state = operation.state as OperationState.Proven
            val contextChainProvided = (state.miningInstruction.endorsedBlockContextHeaders != null
                && state.miningInstruction.endorsedBlockContextHeaders.size > 0)
            val context = ArrayList<Block>()
            var found: Boolean
            var current = state.blockOfProof
            do {
                val previous = bitcoinService.getBlock(current.prevBlockHash)
                    ?: throw Exception(String.format("Could not retrieve block '%s'", current.prevBlockHash))
                if (contextChainProvided) {
                    found = state.miningInstruction.endorsedBlockContextHeaders.stream()
                        .anyMatch { header: ByteArray? ->
                            Arrays.equals(
                                header, Utility.serializeBlock(previous)
                            )
                        }
                    logger.info(
                        "{} block {} in endorsed block context headers", if (found) "Found" else "Did not find", previous.hashAsString
                    )
                } else {
                    val blockIndex = nodeCoreService.getBitcoinBlockIndex(Utility.serializeBlock(previous))
                    found = blockIndex != null
                    logger.info(
                        "{} block {} in search of current NodeCore view", if (found) "Found" else "Did not find", previous.hashAsString
                    )
                }
                if (!found) {
                    context.add(0, previous)
                }
                current = previous
            } while (!found)
            operation.setContext(context)
            TaskResult.succeed(operation, next)
        } catch (e: Exception) {
            logger.error(e.message, e)
            failTask(operation, "Error building Bitcoin context, see log for more detail. Operation can be resubmitted.")
        }
    }
}

/**
 * Sixth and last task that will be executed in a mining operation
 */
class SubmitPoPEndorsementTask(
    nodeCoreService: NodeCoreService,
    bitcoinService: BitcoinService
) : BaseTask(
    nodeCoreService, bitcoinService
) {
    override val next: BaseTask?
        get() = null

    override fun executeImpl(operation: MiningOperation): TaskResult {
        return try {
            val state = operation.state as OperationState.Context
            val popMiningTransaction = PopMiningTransaction(
                state.miningInstruction, state.endorsementTransaction.bitcoinSerialize(), state.merklePath,
                state.blockOfProof, state.bitcoinContextBlocks
            )
            operation.setProofOfProofId(nodeCoreService.submitPop(popMiningTransaction))
            TaskResult.succeed(operation, next)
        } catch (e: ApplicationExceptions.PoPSubmitRejected) {
            logger.error("NodeCore rejected PoP submission")
            failTask(operation, "NodeCore rejected PoP submission. Check NodeCore logs for detail.")
        } catch (e: StatusRuntimeException) {
            logger.error("Failed to submit PoP transaction to NodeCore: {}", e.status)
            failTask(operation, "Unable to submit PoP transaction to NodeCore. Check that NodeCore RPC is available and resubmit operation.")
        }
    }
}
