package org.veriblock.miners.pop.tasks

import io.grpc.StatusRuntimeException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import org.bitcoinj.core.TransactionConfidence
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal
import org.veriblock.miners.pop.common.serializeHeader
import org.veriblock.miners.pop.core.VpmOperation
import org.veriblock.miners.pop.core.VpmOperationState
import org.veriblock.miners.pop.core.debug
import org.veriblock.miners.pop.core.info
import org.veriblock.miners.pop.core.trace
import org.veriblock.miners.pop.model.ApplicationExceptions
import org.veriblock.miners.pop.model.ExpTransaction
import org.veriblock.miners.pop.model.PopMiningTransaction
import org.veriblock.miners.pop.model.merkle.BitcoinMerklePath
import org.veriblock.miners.pop.model.merkle.BitcoinMerkleTree
import org.veriblock.miners.pop.model.merkle.MerkleProof
import org.veriblock.miners.pop.service.BitcoinService
import org.veriblock.miners.pop.service.NodeCoreGateway
import org.veriblock.miners.pop.service.NodeCoreService
import org.veriblock.miners.pop.service.PopSubmitRejected
import org.veriblock.miners.pop.service.TaskException
import org.veriblock.miners.pop.service.TaskService
import org.veriblock.miners.pop.service.failOperation
import org.veriblock.miners.pop.service.failTask
import org.veriblock.miners.pop.service.hr
import org.veriblock.miners.pop.service.min
import org.veriblock.miners.pop.service.sec
import org.veriblock.sdk.models.getSynchronizedMessage
import java.util.ArrayList
import kotlin.math.roundToInt

private val logger = createLogger {}

class VpmTaskService(
    private val nodeCoreService: NodeCoreService,
    private val nodeCoreGateway: NodeCoreGateway,
    private val bitcoinService: BitcoinService
) : TaskService<VpmOperation>() {
    override suspend fun runTasksInternal(operation: VpmOperation) {

        operation.runTask(
            taskName = "Retrieve Mining Instruction from NodeCore",
            targetState = VpmOperationState.INSTRUCTION,
            timeout = 10.sec
        ) {
            verifyNodeCoreStatus()
            // Get the PoPMiningInstruction, consisting of the 80 bytes of data that VeriBlock will pay the PoP miner
            // to publish to Bitcoin (includes 64-byte VB header and 16-byte miner ID) as well as the
            // PoP miner's address
            try {
                val popReply = nodeCoreGateway.getPop(operation.endorsedBlockHeight)
                operation.setMiningInstruction(popReply)
            } catch (e: StatusRuntimeException) {
                failOperation("Failed to get PoP publication data from NodeCore: ${e.status}")
            }
        }

        operation.runTask(
            taskName = "Create Bitcoin Endorsement Transaction",
            targetState = VpmOperationState.ENDORSEMENT_TRANSACTION,
            timeout = 15.min
        ) {
            val miningInstruction = operation.miningInstruction
                ?: failOperation(
                    "Trying to create endorsement transaction without the mining instruction!"
                )

            val opReturnScript = bitcoinService.generatePoPScript(miningInstruction.publicationData)
            try {
                val transaction = bitcoinService.createPoPTransaction(opReturnScript)
                if (transaction != null) {
                    val txSizeKb = transaction.unsafeBitcoinSerialize().size / 1000.0
                    val feeSats = transaction.fee.value
                    val feePerKb = (feeSats / txSizeKb).roundToInt()
                    logger.info(
                        operation,
                        "Created BTC transaction '${transaction.txId}'. Fee: $feeSats Sat. Size: $txSizeKb KB. Fee per KB: $feePerKb Sat/KB."
                    )

                    val exposedTransaction = ExpTransaction(
                        bitcoinService.context.params,
                        transaction.unsafeBitcoinSerialize()
                    )

                    operation.setTransaction(transaction, exposedTransaction.getFilteredTransaction())
                } else {
                    failOperation("Create Bitcoin transaction returned no transaction")
                }
            } catch (e: ApplicationExceptions.UnableToAcquireTransactionLock) {
                failTask(e.message)
            }
        }

        operation.runTask(
            taskName = "Confirm Bitcoin Endorsement Transaction",
            targetState = VpmOperationState.CONFIRMED,
            //timeout = 2.hr
            timeout = 10.hr // Very long timeout to make sure we never leave a dangling transaction
        ) {
            val endorsementTransaction = operation.endorsementTransaction
                ?: failTask("The operation has no transaction set!")

            if (endorsementTransaction.confidence.confidenceType == TransactionConfidence.ConfidenceType.DEAD) {
                failOperation("The transaction ${endorsementTransaction.txId} couldn't be confirmed")
            }

            if (endorsementTransaction.confidence.confidenceType != TransactionConfidence.ConfidenceType.BUILDING) {
                // Wait for the transaction to be ready
                operation.transactionConfidence.onEach {
                    if (it == TransactionConfidence.ConfidenceType.DEAD) {
                        failOperation("The transaction couldn't be confirmed")
                    }
                }.first {
                    it == TransactionConfidence.ConfidenceType.BUILDING
                }
            }

            logger.info(operation, "BTC endorsement transaction has been confirmed!")
            operation.setConfirmed()
        }

        operation.runTask(
            taskName = "Determine Block of Proof",
            targetState = VpmOperationState.BLOCK_OF_PROOF,
            //timeout = 90.sec
            timeout = 2.hr // Very long timeout to make sure we never leave a dangling transaction
        ) {
            val endorsementTransaction = operation.endorsementTransaction
                ?: failTask("The operation has no transaction set!")

            val blockAppearances = endorsementTransaction.appearsInHashes
                ?: failTask("The transaction does not appear in any hashes!")

            val bestBlock = bitcoinService.getBestBlock(blockAppearances.keys)
                ?: failTask("Unable to retrieve block of proof from transaction!")

            logger.info(operation, "Block of proof: ${bestBlock.hash}")

            // Wait for the actual block appearing in the blockchain (it should already be there given the transaction is confirmed)
            bitcoinService.getFilteredBlock(bestBlock.hash)

            operation.setBlockOfProof(bestBlock)
        }

        operation.runTask(
            taskName = "Prove Transaction",
            targetState = VpmOperationState.PROVEN,
            timeout = 30.min
        ) {
            val endorsementTransaction = operation.endorsementTransaction
                ?: failTask("Trying to prove transaction without the actual transaction!")
            val blockOfProof = operation.blockOfProof
                ?: failTask("Trying to prove transaction without block of proof!")

            val partialMerkleTree = bitcoinService.getPartialMerkleTree(blockOfProof.hash)
            val failureReason = if (partialMerkleTree != null) {
                val proof = MerkleProof.parse(partialMerkleTree)
                if (proof != null) {
                    val path = proof.getCompactPath(endorsementTransaction.txId)
                    logger.debug(operation, "Merkle Proof Compact Path: $path")
                    logger.debug(operation, "Merkle Root: ${blockOfProof.merkleRoot}")
                    try {
                        val merklePath = BitcoinMerklePath(path)
                        logger.debug(operation, "Computed Merkle Root: ${merklePath.getMerkleRoot()}")
                        if (merklePath.getMerkleRoot().equals(blockOfProof.merkleRoot.toString(), ignoreCase = true)) {
                            operation.setMerklePath(merklePath.getCompactFormat())
                            return@runTask
                        } else {
                            "Block Merkle root does not match computed Merkle root"
                        }
                    } catch (e: Exception) {
                        logger.error("Unable to validate Merkle path for transaction", e)
                        "Unable to prove transaction ${endorsementTransaction.txId} in block ${blockOfProof.hashAsString}"
                    }
                } else {
                    "Unable to construct Merkle proof for block ${blockOfProof.hashAsString}"
                }
            } else {
                "Unable to retrieve the Merkle tree from the block ${blockOfProof.hashAsString}"
            }

            // Retrieving the Merkle path from the PartialMerkleTree failed, try creating manually from the whole block
            logger.info(
                operation,
                "Unable to calculate the correct Merkle path for transaction ${endorsementTransaction.txId} in " +
                    "block ${blockOfProof.hash} from a FilteredBlock, trying a fully downloaded block!"
            )
            val fullBlock = bitcoinService.downloadBlock(blockOfProof.hash)
            val allTransactions = fullBlock!!.transactions
            val txids: MutableList<String> = ArrayList()
            for (i in allTransactions!!.indices) {
                txids.add(allTransactions[i].txId.toString())
            }
            val bmt = BitcoinMerkleTree(true, txids)
            val merklePath = bmt.getPathFromTxID(endorsementTransaction.txId.toString())!!
            if (merklePath.getMerkleRoot().equals(blockOfProof.merkleRoot.toString(), ignoreCase = true)) {
                operation.setMerklePath(merklePath.getCompactFormat())
            } else {
                logger.error(
                    "Unable to calculate the correct Merkle path for transaction ${endorsementTransaction.txId}" +
                        " in block ${blockOfProof.hashAsString} from a FilteredBlock or a fully downloaded block!"
                )
                failOperation(failureReason)
            }
        }

        operation.runTask(
            taskName = "Build Publication Context",
            targetState = VpmOperationState.CONTEXT,
            timeout = 10.min
        ) {
            verifyNodeCoreStatus()
            val miningInstruction = operation.miningInstruction
                ?: failOperation("Trying to build context without the mining instruction!")
            val blockOfProof = operation.blockOfProof
                ?: failOperation("Trying to build context without the block of proof!")

            val contextChainProvided = miningInstruction.endorsedBlockContextHeaders.isNotEmpty()
            // Get all the previous blocks until we find
            val context = generateSequence(blockOfProof) { block ->
                bitcoinService.getBlock(block.prevBlockHash)
                    ?: failOperation("Could not retrieve block '${block.prevBlockHash}'")
            }.drop(1).takeWhile { block ->
                val found = if (contextChainProvided) {
                    miningInstruction.endorsedBlockContextHeaders.any {
                        it.contentEquals(block.serializeHeader())
                    }
                } else {
                    val blockIndex = nodeCoreGateway.getBitcoinBlockIndex(block.serializeHeader())
                    blockIndex != null
                }
                val prefix = if (found) "Found" else "Did not find"
                val where = if (contextChainProvided) "endorsed block context headers" else "search of current NodeCore view"
                logger.trace(operation, "$prefix block ${block.hashAsString} in $where")
                !found
            }.toList().reversed()
            operation.setContext(context)
        }

        operation.runTask(
            taskName = "Submit PoP Endorsement",
            targetState = VpmOperationState.SUBMITTED_POP_DATA,
            timeout = 10.min
        ) {
            verifyNodeCoreStatus()
            val miningInstruction = operation.miningInstruction
                ?: failOperation("Trying to submit PoP endorsement without the mining instruction!")
            val endorsementTransactionBytes = operation.endorsementTransactionBytes
                ?: failOperation("Trying to submit PoP endorsement without the endorsement transaction!")
            val blockOfProof = operation.blockOfProof
                ?: failOperation("Trying to submit PoP endorsement without the block of proof!")
            val merklePath = operation.merklePath
                ?: failOperation("Trying to submit PoP endorsement without the merkle path!")
            val context = operation.context
                ?: failOperation("Trying to submit PoP endorsement without the publication context!")

            try {
                val popMiningTransaction = PopMiningTransaction(
                    miningInstruction, endorsementTransactionBytes, merklePath,
                    blockOfProof, context
                )
                val popTxId = nodeCoreGateway.submitPop(popMiningTransaction)
                logger.info(operation, "PoP endorsement submitted! PoP transaction id: $popTxId")
                operation.setProofOfProofId(popTxId)
            } catch (e: PopSubmitRejected) {
                logger.error("NodeCore rejected PoP submission")
                failTask("NodeCore rejected PoP submission. Check NodeCore logs for detail.")
            } catch (e: StatusRuntimeException) {
                logger.error("Failed to submit PoP transaction to NodeCore: {}", e.status.code)
                failTask(
                    "Unable to submit PoP transaction to NodeCore. Check that NodeCore RPC is available and resubmit operation."
                )
            }
        }

        operation.runTask(
            taskName = "Confirm Payout",
            targetState = VpmOperationState.PAYOUT_DETECTED,
            timeout = 20.hr
        ) {
            verifyNodeCoreStatus()
            val miningInstruction = operation.miningInstruction
                ?: failOperation("Trying to confirm Payout without the mining instruction!")

            val bitcoinTransactionId = operation.endorsementTransaction?.txId?.toString()
                ?: failOperation("Trying to confirm Payout without the endorsement transaction!")

            val endorsedBlockHeight = operation.endorsedBlockHeight
                ?: failOperation(
                    "Trying to wait for the payout block without having the endorsed block height set"
                )

            // Wait for the endorsement transaction to have enough confirmations
            // DISABLED: it took too much NodeCore power
            //do {
            //    delay(30000)
            //    val confirmations = try {
            //        nodeCoreService.getTransactionConfirmationsById(state.proofOfProofId)
            //    } catch (e: Exception) {
            //        failTask("Transaction retrieval by id has failed: ${e.message}")
            //    }
            //} while (confirmations == null || confirmations < 50)

            val payoutBlockHeight = endorsedBlockHeight + 500
            val payoutAddress = miningInstruction.minerAddress

            // Wait for the payout block
            var payoutBlockHash: String?
            do {
                delay(30000)
                payoutBlockHash = try {
                    nodeCoreGateway.getBlockHash(payoutBlockHeight)
                } catch (e: Exception) {
                    failTask("Block retrieval by height has failed: ${e.message}")
                }
            } while (payoutBlockHash == null)

            logger.debug(operation, "Payout block hash: $payoutBlockHash")

            val endorsementInfo = nodeCoreGateway.getPopEndorsementInfo().find {
                it.endorsedBlockNumber == endorsedBlockHeight && it.minerAddress == payoutAddress &&
                    it.bitcoinTransactionId.equals(bitcoinTransactionId, ignoreCase = true)
            } ?: failOperation(
                "Could not find PoP endorsement reward in the payout block $payoutBlockHash @ $payoutBlockHeight!"
            )

            operation.setPayoutData(payoutBlockHash, endorsementInfo.reward)
            logger.info(operation, "Operation has completed! Payout: ${endorsementInfo.reward.formatAtomicLongWithDecimal()} VBK")
        }

        operation.complete()
    }

    private fun verifyNodeCoreStatus() {
        if (!nodeCoreService.isReady()) {
            throw TaskException(
                "NodeCore is not ready: Connection ${nodeCoreService.isAccessible()}," +
                    " SameNetwork: ${nodeCoreService.isOnSameNetwork()}," +
                    " Synchronized: ${nodeCoreService.isSynchronized()}" +
                    " (${nodeCoreService.latestNodeCoreStateInfo.getSynchronizedMessage()})"
            )
        }
    }
}
