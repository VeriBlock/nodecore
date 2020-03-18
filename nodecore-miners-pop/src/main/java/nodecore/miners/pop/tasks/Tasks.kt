package nodecore.miners.pop.tasks

import io.grpc.StatusRuntimeException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import nodecore.miners.pop.common.BitcoinMerklePath
import nodecore.miners.pop.common.BitcoinMerkleTree
import nodecore.miners.pop.common.MerkleProof
import nodecore.miners.pop.common.Utility
import nodecore.miners.pop.core.MiningOperation
import nodecore.miners.pop.core.OperationState
import nodecore.miners.pop.core.OperationStateType
import nodecore.miners.pop.core.info
import nodecore.miners.pop.core.warn
import nodecore.miners.pop.events.EventBus
import nodecore.miners.pop.model.ApplicationExceptions
import nodecore.miners.pop.model.ExpTransaction
import nodecore.miners.pop.model.PopMiningTransaction
import nodecore.miners.pop.services.BitcoinService
import nodecore.miners.pop.services.NodeCoreService
import org.bitcoinj.core.Block
import org.bitcoinj.core.InsufficientMoneyException
import org.bitcoinj.core.TransactionConfidence
import org.bitcoinj.wallet.Wallet
import org.veriblock.core.utilities.createLogger
import java.util.ArrayList
import java.util.Arrays

private val logger = createLogger {}

suspend fun runTasks(
    nodeCoreService: NodeCoreService,
    bitcoinService: BitcoinService,
    operation: MiningOperation
) {
    if (operation.state is OperationState.Failed) {
        logger.warn(operation) { "Attempted to run tasks for a failed operation!" }
        return
    }

    try {
        operation.runTask("Retrieve Mining Instruction from NodeCore", OperationStateType.INSTRUCTION) {
            /* Get the PoPMiningInstruction, consisting of the 80 bytes of data that VeriBlock will pay the PoP miner
             * to publish to Bitcoin (includes 64-byte VB header and 16-byte miner ID) as well as the
             * PoP miner's address
             */
            try {
                val popReply = nodeCoreService.getPop(operation.blockHeight)
                if (popReply.success) {
                    operation.setMiningInstruction(popReply.result!!)
                } else {
                    error(popReply.resultMessage!!)
                }
            } catch (e: StatusRuntimeException) {
                error("Failed to get PoP publication data from NodeCore: ${e.status}")
            }
        }
        operation.runTask("Create Bitcoin Endorsement Transaction", OperationStateType.ENDORSEMEMT_TRANSACTION) {
            val miningInstruction = (operation.state as OperationState.Instruction).miningInstruction
            val opReturnScript = bitcoinService.generatePoPScript(miningInstruction.publicationData)
            try {
                try {
                    val transaction = bitcoinService.createPoPTransaction(opReturnScript)
                    if (transaction != null) {
                        logger.info { "Successfully broadcast transaction ${transaction.txId}" }

                        val exposedTransaction = ExpTransaction(
                            bitcoinService.context.params,
                            transaction.unsafeBitcoinSerialize()
                        )

                        operation.setTransaction(transaction, exposedTransaction.getFilteredTransaction())
                    } else {
                        error("Create Bitcoin transaction returned no transaction")
                    }
                } catch (t: Throwable) {
                    error("A problem occurred broadcasting the transaction to the peer group")
                }
            } catch (e: ApplicationExceptions.SendTransactionException) {
                for (t in e.suppressed) {
                    when (t) {
                        is ApplicationExceptions.UnableToAcquireTransactionLock -> {
                            error(
                                "A previous transaction has not yet completed broadcasting to peers and new transactions would result in double spending. Wait a few seconds and try again."
                            )
                        }
                        is InsufficientMoneyException -> {
                            logger.info(t.message)
                            EventBus.insufficientFundsEvent.trigger()
                            error("PoP wallet does not contain sufficient funds to create PoP transaction")
                        }
                        is ApplicationExceptions.ExceededMaxTransactionFee -> {
                            error("Calculated fee exceeded configured maximum transaction fee")
                        }
                        is ApplicationExceptions.DuplicateTransactionException -> {
                            error(
                                "Transaction appears identical to a previously broadcast transaction. Often this occurs when there is a 'too-long-mempool-chain'."
                            )
                        }
                        is Wallet.CompletionException -> {
                            logger.error(t.javaClass.simpleName, t)
                            error("Unable to complete transaction: ${t.javaClass.simpleName}")
                        }
                        else -> {
                            logger.error(t.message, t)
                            error("Unable to send transaction: ${t.message}")
                        }
                    }
                }
            }
        }
        operation.runTask("Confirm Transaction", OperationStateType.CONFIRMED) {
            val state = operation.state as? OperationState.EndorsementTransaction
                ?: failTask("The operation has no transaction set!")

            if (state.endorsementTransaction.confidence.confidenceType != TransactionConfidence.ConfidenceType.BUILDING) {
                // Wait for the transaction to be ready
                do {
                    val transactionConfidence = operation.transactionConfidenceEventChannel.receive()
                } while (transactionConfidence != TransactionConfidence.ConfidenceType.BUILDING)
            }

            operation.setConfirmed()
        }
        operation.runTask("Determine Block of Proof", OperationStateType.BLOCK_OF_PROOF) {
            val state = operation.state as? OperationState.Confirmed
                ?: failTask("The operation has no transaction set!")

            val blockAppearances = state.endorsementTransaction.appearsInHashes
                ?: failTask("The transaction does not appear in any hashes!")

            val bestBlock = bitcoinService.getBestBlock(blockAppearances.keys)
                ?: failTask("Unable to retrieve block of proof from transaction!")

            operation.setBlockOfProof(bestBlock)

            // Wait for the actual block appearing in the blockchain
            bitcoinService.getFilteredBlock(bestBlock.hash)
        }
        operation.runTask("Prove Transaction", OperationStateType.PROVEN) {
            val state = operation.state as? OperationState.BlockOfProof
                ?: failTask("Trying to prove transaction without block of proof!")

            val block = state.blockOfProof
            val partialMerkleTree = bitcoinService.getPartialMerkleTree(block.hash)
            val failureReason = if (partialMerkleTree != null) {
                val proof = MerkleProof.parse(partialMerkleTree)
                if (proof != null) {
                    val path = proof.getCompactPath(state.endorsementTransaction.txId)
                    logger.info(operation) { "Merkle Proof Compact Path: $path" }
                    logger.info(operation) { "Merkle Root: ${block.merkleRoot}" }
                    try {
                        val merklePath = BitcoinMerklePath(path)
                        logger.info(operation) { "Computed Merkle Root: ${merklePath.getMerkleRoot()}" }
                        if (merklePath.getMerkleRoot().equals(block.merkleRoot.toString(), ignoreCase = true)) {
                            operation.setMerklePath(merklePath.getCompactFormat())
                            return@runTask
                        } else {
                            "Block Merkle root does not match computed Merkle root"
                        }
                    } catch (e: Exception) {
                        logger.error("Unable to validate Merkle path for transaction", e)
                        "Unable to prove transaction ${state.endorsementTransaction.txId} in block ${block.hashAsString}"
                    }
                } else {
                    "Unable to construct Merkle proof for block ${block.hashAsString}"
                }
            } else {
                "Unable to retrieve the Merkle tree from the block ${block.hashAsString}"
            }

            // Retrieving the Merkle path from the PartialMerkleTree failed, try creating manually from the whole block
            logger.info(operation) {
                "Unable to calculate the correct Merkle path for transaction ${state.endorsementTransaction.txId} in " +
                    "block ${state.blockOfProof.hashAsString} from a FilteredBlock, trying a fully downloaded block!"
            }
            val fullBlock = bitcoinService.downloadBlock(state.blockOfProof.hash)
            val allTransactions = fullBlock!!.transactions
            val txids: MutableList<String> = ArrayList()
            for (i in allTransactions!!.indices) {
                txids.add(allTransactions[i].txId.toString())
            }
            val bmt = BitcoinMerkleTree(true, txids)
            val merklePath = bmt.getPathFromTxID(state.endorsementTransaction.txId.toString())!!
            if (merklePath.getMerkleRoot().equals(state.blockOfProof.merkleRoot.toString(), ignoreCase = true)) {
                operation.setMerklePath(merklePath.getCompactFormat())
            } else {
                logger.error(
                    "Unable to calculate the correct Merkle path for transaction ${state.endorsementTransaction.txId}" +
                        " in block ${state.blockOfProof.hashAsString} from a FilteredBlock or a fully downloaded block!"
                )
                error(failureReason)
            }
        }
        operation.runTask("Build Publication Context", OperationStateType.CONTEXT) {
            val state = operation.state as? OperationState.Proven
                ?: error("Trying to build context without having proven the transaction!")

            val contextChainProvided = state.miningInstruction.endorsedBlockContextHeaders != null &&
                state.miningInstruction.endorsedBlockContextHeaders.size > 0
            val context = ArrayList<Block>()
            var found: Boolean
            var current = state.blockOfProof
            do {
                val previous = bitcoinService.getBlock(current.prevBlockHash)
                    ?: error("Could not retrieve block '${current.prevBlockHash}'")
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
        }
        operation.runTask("Submit PoP Endorsement", OperationStateType.SUBMITTED_POP_DATA) {
            try {
                val state = operation.state as? OperationState.Context
                    ?: error("Trying to submit PoP endorsement without a publication context")
                val popMiningTransaction = PopMiningTransaction(
                    state.miningInstruction, state.endorsementTransactionBytes, state.merklePath,
                    state.blockOfProof, state.bitcoinContextBlocks
                )
                val popTxId = nodeCoreService.submitPop(popMiningTransaction)
                operation.setProofOfProofId(popTxId)
            } catch (e: ApplicationExceptions.PoPSubmitRejected) {
                logger.error("NodeCore rejected PoP submission")
                failTask("NodeCore rejected PoP submission. Check NodeCore logs for detail.")
            } catch (e: StatusRuntimeException) {
                logger.error("Failed to submit PoP transaction to NodeCore: {}", e.status)
                failTask("Unable to submit PoP transaction to NodeCore. Check that NodeCore RPC is available and resubmit operation.")
            }
        }

        // TODO: more tasks

        EventBus.popMiningOperationCompletedEvent.trigger(operation.id)
    } catch (e: CancellationException) {
        logger.info(operation) { "Reorg detected! Tasks cancelled. A new job will be started." }
    } catch (t: Throwable) {
        operation.fail(t.message ?: "Unknown reason")
    }
}

private const val MAX_TASK_RETRIES = 10

private suspend inline fun MiningOperation.runTask(
    taskName: String,
    targetStateType: OperationStateType,
    block: () -> Unit
) {
    // Check if this operation needs to run this task first
    if (state hasType targetStateType) {
        return
    }

    var success = false
    var attempts = 1
    do {
        try {
            block()
            success = true
        } catch (e: TaskException) {
            logger.warn(this) { "Task '$taskName' has failed: ${e.message}" }
            if (attempts < MAX_TASK_RETRIES) {
                attempts++
                val secondsToWait = attempts * attempts * 10
                logger.info(this) { "Will try again in $secondsToWait seconds..." }
                delay(secondsToWait * 1000L)
                logger.info(this) { "Performing attempt #$attempts to $taskName..." }
            } else {
                logger.warn(this) { "Maximum reattempt amount exceeded for task '$taskName'" }
                throw e
            }
        }
    } while (!success)
}

class TaskException(message: String) : RuntimeException(message)

/**
 *  Throw an exception as the task failed. It is inline so that call stack is not polluted.
 */
private inline fun failTask(reason: String): Nothing {
    throw TaskException(reason)
}
