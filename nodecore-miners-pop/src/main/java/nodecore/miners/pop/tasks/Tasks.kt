package nodecore.miners.pop.tasks

import io.grpc.StatusRuntimeException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.yield
import nodecore.miners.pop.EventBus
import nodecore.miners.pop.common.BitcoinMerklePath
import nodecore.miners.pop.common.BitcoinMerkleTree
import nodecore.miners.pop.common.MerkleProof
import nodecore.miners.pop.common.Utility
import nodecore.miners.pop.core.MiningOperation
import nodecore.miners.pop.core.OperationState
import nodecore.miners.pop.core.OperationStateType
import nodecore.miners.pop.core.debug
import nodecore.miners.pop.core.info
import nodecore.miners.pop.core.warn
import nodecore.miners.pop.model.ApplicationExceptions
import nodecore.miners.pop.model.ExpTransaction
import nodecore.miners.pop.model.PopMiningTransaction
import nodecore.miners.pop.services.BitcoinService
import nodecore.miners.pop.services.NodeCoreService
import org.bitcoinj.core.TransactionConfidence
import org.veriblock.core.utilities.createLogger
import java.time.Duration
import java.util.ArrayList

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
        operation.runTask(
            taskName = "Retrieve Mining Instruction from NodeCore",
            targetState = OperationStateType.INSTRUCTION,
            timeout = 10.sec
        ) {
            // Get the PoPMiningInstruction, consisting of the 80 bytes of data that VeriBlock will pay the PoP miner
            // to publish to Bitcoin (includes 64-byte VB header and 16-byte miner ID) as well as the
            // PoP miner's address
            try {
                val popReply = nodeCoreService.getPop(operation.endorsedBlockHeight)
                if (popReply.success) {
                    operation.setMiningInstruction(popReply.result!!)
                } else {
                    error(popReply.resultMessage!!)
                }
            } catch (e: StatusRuntimeException) {
                error("Failed to get PoP publication data from NodeCore: ${e.status}")
            }
        }
        operation.runTask(
            taskName = "Create Bitcoin Endorsement Transaction",
            targetState = OperationStateType.ENDORSEMENT_TRANSACTION,
            timeout = 10.min
        ) {
            val miningInstruction = (operation.state as OperationState.Instruction).miningInstruction
            val opReturnScript = bitcoinService.generatePoPScript(miningInstruction.publicationData)
            try {
                val transaction = bitcoinService.createPoPTransaction(opReturnScript)
                if (transaction != null) {
                    logger.debug { "Successfully broadcast transaction ${transaction.txId}" }

                    val exposedTransaction = ExpTransaction(
                        bitcoinService.context.params,
                        transaction.unsafeBitcoinSerialize()
                    )

                    operation.setTransaction(transaction, exposedTransaction.getFilteredTransaction())
                } else {
                    error("Create Bitcoin transaction returned no transaction")
                }
            } catch (e: ApplicationExceptions.UnableToAcquireTransactionLock) {
                failTask(e.message)
            }
        }
        operation.runTask(
            taskName = "Confirm Transaction",
            targetState = OperationStateType.CONFIRMED,
            timeout = 1.hr
        ) {
            val state = operation.state as? OperationState.EndorsementTransaction
                ?: failTask("The operation has no transaction set!")

            if (state.endorsementTransaction.confidence.confidenceType != TransactionConfidence.ConfidenceType.BUILDING) {
                // Wait for the transaction to be ready
                operation.transactionConfidenceEventChannel.asFlow().first { it == TransactionConfidence.ConfidenceType.BUILDING }
            }

            operation.setConfirmed()
        }
        operation.runTask(
            taskName = "Determine Block of Proof",
            targetState = OperationStateType.BLOCK_OF_PROOF,
            timeout = 90.sec
        ) {
            val state = operation.state as? OperationState.Confirmed
                ?: failTask("The operation has no transaction set!")

            val blockAppearances = state.endorsementTransaction.appearsInHashes
                ?: failTask("The transaction does not appear in any hashes!")

            val bestBlock = bitcoinService.getBestBlock(blockAppearances.keys)
                ?: failTask("Unable to retrieve block of proof from transaction!")

            // Wait for the actual block appearing in the blockchain (it should already be there given the transaction is confirmed)
            bitcoinService.getFilteredBlock(bestBlock.hash)

            operation.setBlockOfProof(bestBlock)
        }
        operation.runTask(
            taskName = "Prove Transaction",
            targetState = OperationStateType.PROVEN,
            timeout = 90.sec
        ) {
            val state = operation.state as? OperationState.BlockOfProof
                ?: failTask("Trying to prove transaction without block of proof!")

            val block = state.blockOfProof
            val partialMerkleTree = bitcoinService.getPartialMerkleTree(block.hash)
            val failureReason = if (partialMerkleTree != null) {
                val proof = MerkleProof.parse(partialMerkleTree)
                if (proof != null) {
                    val path = proof.getCompactPath(state.endorsementTransaction.txId)
                    logger.debug(operation) { "Merkle Proof Compact Path: $path" }
                    logger.debug(operation) { "Merkle Root: ${block.merkleRoot}" }
                    try {
                        val merklePath = BitcoinMerklePath(path)
                        logger.debug(operation) { "Computed Merkle Root: ${merklePath.getMerkleRoot()}" }
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
        operation.runTask(
            taskName = "Build Publication Context",
            targetState = OperationStateType.CONTEXT,
            timeout = 2.min
        ) {
            val state = operation.state as? OperationState.Proven
                ?: error("Trying to build context without having proven the transaction!")

            val contextChainProvided = state.miningInstruction.endorsedBlockContextHeaders.isNotEmpty()
            // Get all the previous blocks until we find
            val context = generateSequence(state.blockOfProof) { block ->
                bitcoinService.getBlock(block.prevBlockHash)
                    ?: error("Could not retrieve block '${block.prevBlockHash}'")
            }.drop(1).takeWhile { block ->
                val found = if (contextChainProvided) {
                    state.miningInstruction.endorsedBlockContextHeaders.any {
                        it.contentEquals(Utility.serializeBlock(block))
                    }
                } else {
                    val blockIndex = nodeCoreService.getBitcoinBlockIndex(Utility.serializeBlock(block))
                    blockIndex != null
                }
                logger.trace {
                    val prefix = if (found) "Found" else "Did not find"
                    val where = if (contextChainProvided) "endorsed block context headers" else "search of current NodeCore view"
                    "$prefix block ${block.hashAsString} in $where"
                }
                !found
            }.toList().reversed()
            operation.setContext(context)
        }
        operation.runTask(
            taskName = "Submit PoP Endorsement",
            targetState = OperationStateType.SUBMITTED_POP_DATA,
            timeout = 30.sec
        ) {
            val state = operation.state as? OperationState.Context
                ?: error("Trying to submit PoP endorsement without a publication context")

            try {
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
        operation.runTask(
            taskName = "Confirm VBK Endorsement Transaction",
            targetState = OperationStateType.VBK_ENDORSEMENT_TRANSACTION_CONFIRMED,
            timeout = 1.hr
        ) {
            val state = operation.state as? OperationState.SubmittedPopData
                ?: error("Trying to confirm VBK Endorsement Transaction without having submitted it")

            // Wait for the endorsement transaction to have enough confirmations
            do {
                delay(30000)
                val confirmations = try {
                    nodeCoreService.getTransactionConfirmationsById(state.proofOfProofId)
                } catch (e: Exception) {
                    failTask("Transaction retrieval by id has failed: ${e.message}")
                }
            } while (confirmations == null || confirmations < 10)

            operation.setVbkEndorsementTransactionConfirmed()
        }
        operation.runTask(
            taskName = "Confirm Payout Block",
            targetState = OperationStateType.COMPLETED,
            timeout = 10.hr
        ) {
            val state = operation.state as? OperationState.SubmittedPopData
                ?: error("Trying to confirm Payout without having submitted PoP Data")

            val endorsedBlockHeight = operation.endorsedBlockHeight
                ?: error("Trying to wait for the payout block without having the endorsed block height set")

            val payoutBlockHeight = endorsedBlockHeight + 500
            val payoutAddress = state.miningInstruction.minerAddress

            // Wait for the payout block
            var payoutBlockHash: String?
            do {
                delay(30000)
                payoutBlockHash = try {
                    nodeCoreService.getBlockHash(payoutBlockHeight)
                } catch (e: Exception) {
                    failTask("Block retrieval by height has failed: ${e.message}")
                }
            } while (payoutBlockHash == null)

            logger.debug(operation) { "Payout block hash: $payoutBlockHash" }

            // FIXME: Retrieve the reward from the payout block itself
            val endorsementInfo = nodeCoreService.getPopEndorsementInfo().find {
                it.endorsedBlockNumber == endorsedBlockHeight && it.minerAddress == payoutAddress
            } ?: error("Could not find PoP endorsement reward in the payout block $payoutBlockHash @ $payoutBlockHeight!")

            operation.complete(payoutBlockHash, endorsementInfo.reward)
        }

        EventBus.popMiningOperationCompletedEvent.trigger(operation.id)
    } catch (e: CancellationException) {
        logger.info(operation) { "Job was cancelled" }
    } catch (t: Throwable) {
        logger.debug(t) { t.message }
        operation.fail(t.message ?: "Unknown reason")
    }
}

private const val MAX_TASK_RETRIES = 10

private suspend inline fun MiningOperation.runTask(
    taskName: String,
    targetState: OperationStateType,
    timeout: Duration,
    crossinline block: suspend () -> Unit
) {
    // Check if this operation needs to run this task first
    if (state hasType targetState) {
        return
    }

    var success = false
    var attempts = 1
    do {
        try {
            withTimeout(timeout) {
                block()
            }
            success = true
        } catch (e: TaskException) {
            logger.warn(this) { "Task '$taskName' has failed: ${e.message}" }
            if (attempts < MAX_TASK_RETRIES) {
                attempts++
                // Check if the task was cancelled before performing any reattempts
                yield()
                // Wait a growing amount of time before every reattempt
                val secondsToWait = attempts * attempts * 10
                logger.info(this) { "Will try again in $secondsToWait seconds..." }
                delay(secondsToWait * 1000L)
                logger.info(this) { "Performing attempt #$attempts to $taskName..." }
            } else {
                logger.warn(this) { "Maximum reattempt amount exceeded for task '$taskName'" }
                throw e
            }
        } catch (e: TimeoutCancellationException) {
            error("Operation has been cancelled for taking too long during task '$taskName'.")
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

private inline val Int.sec get() = Duration.ofSeconds(this.toLong())
private inline val Int.min get() = Duration.ofMinutes(this.toLong())
private inline val Int.hr get() = Duration.ofHours(this.toLong())
