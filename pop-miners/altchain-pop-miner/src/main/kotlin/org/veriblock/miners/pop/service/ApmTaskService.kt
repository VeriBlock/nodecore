// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.veriblock.core.altchain.checkForValidEndorsement
import org.veriblock.core.utilities.AddressUtility
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugError
import org.veriblock.core.utilities.debugWarn
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.miners.pop.core.TransactionMeta
import org.veriblock.miners.pop.core.ApmOperation
import org.veriblock.miners.pop.core.ApmSpTransaction
import org.veriblock.miners.pop.core.debug
import org.veriblock.miners.pop.core.info
import org.veriblock.sdk.alt.model.SecurityInheritingBlock
import org.veriblock.sdk.alt.model.SecurityInheritingTransaction
import org.veriblock.sdk.models.AltPublication
import org.veriblock.sdk.models.BlockStoreException
import org.veriblock.core.crypto.asVbkHash
import org.veriblock.miners.pop.core.ApmOperationState
import org.veriblock.miners.pop.MinerConfig
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingMonitor
import org.veriblock.sdk.models.getSynchronizedMessage
import org.veriblock.sdk.services.SerializeDeserializeService

private val logger = createLogger {}

class ApmTaskService(
    private val minerConfig: MinerConfig
) : TaskService<ApmOperation>(), KoinComponent {

    private val miner: AltchainPopMinerService by inject()

    @ExperimentalCoroutinesApi
    override suspend fun runTasksInternal(operation: ApmOperation) {
        operation.runTask(
            taskName = "Retrieve Mining Instruction from ${operation.chain.name}",
            targetState = ApmOperationState.INSTRUCTION,
            timeout = 90.sec
        ) {
            verifyAltchainStatus(operation.chain.name, operation.chainMonitor)
            verifySpvStatus()
            logger.info(operation, "Getting the mining instruction...")
            val publicationData = try {
                operation.chain.getMiningInstruction(operation.endorsedBlockHeight)
            } catch (e: Exception) {
                failOperation("Error while trying to get PoP Mining Instruction from ${operation.chain.name}: ${e.message}")
            }
            operation.setMiningInstruction(publicationData)
            logger.info(operation, "Successfully retrieved the mining instruction!")
            val vbkContextBlockHash = publicationData.context[0]
            miner.network.getBlock(vbkContextBlockHash.asVbkHash())
                ?: failOperation("Unable to find the mining instruction's VBK context block ${vbkContextBlockHash.toHex()}")
        }

        operation.runTask(
            taskName = "Create Endorsement Transaction",
            targetState = ApmOperationState.ENDORSEMENT_TRANSACTION,
            timeout = 90.sec
        ) {
            verifySpvStatus()
            val miningInstruction = operation.miningInstruction
                ?: failTask("CreateEndorsementTransactionTask called without mining instruction!")
            // Something to fill in all the gaps
            logger.info(operation, "Submitting endorsement VBK transaction...")
            val transaction = try {
                val endorsementData = SerializeDeserializeService.serialize(miningInstruction.publicationData)
                endorsementData.checkForValidEndorsement {
                    logger.debugError(it) { "Invalid endorsement data" }
                    failOperation("Invalid endorsement data: ${endorsementData.toHex()}")
                }
                miner.network.submitEndorsement(
                    endorsementData,
                    minerConfig.feePerByte,
                    minerConfig.maxFee
                )
            } catch (e: Exception) {
                failOperation("Could not create endorsement VBK transaction: ${e.message}")
            }

            val valid = AddressUtility.isSignatureValid(
                transaction.id.bytes, transaction.signature, transaction.publicKey, transaction.sourceAddress.address
            )
            if (!valid) {
                failOperation("Endorsement VBK transaction signature is not valid")
            }

            val walletTransaction = miner.transactionMonitor.getTransaction(transaction.id)
            operation.setTransaction(ApmSpTransaction(walletTransaction))
            logger.info(operation, "Successfully added the VBK transaction: ${walletTransaction.id}!")
            logger.debug(
                operation, "Transaction ${transaction.id} networkByte=${transaction.networkByte} signatureIndex=${transaction.signatureIndex}"
            )
            logger.debug(operation, "Transaction ${transaction.id} raw data: ${SerializeDeserializeService.serialize(transaction).toHex()}")
        }

        operation.runTask(
            taskName = "Confirm transaction",
            targetState = ApmOperationState.ENDORSEMENT_TX_CONFIRMED,
            timeout = 1.hr
        ) {
            verifySpvStatus()
            val endorsementTransaction = operation.endorsementTransaction
                ?: failTask("ConfirmTransactionTask called without wallet transaction!")

            logger.info(operation, "Waiting for the transaction to be included in VeriBlock block...")
            // Wait for the transaction to be confirmed
            endorsementTransaction.transaction.transactionMeta.stateFlow.first {
                it == TransactionMeta.MetaState.CONFIRMED
            }

            // Transaction has been confirmed!
            operation.setConfirmed()
        }

        operation.runTask(
            taskName = "Determine Block of Proof",
            targetState = ApmOperationState.BLOCK_OF_PROOF,
            timeout = 5.min
        ) {
            val transaction = operation.endorsementTransaction?.transaction
                ?: failTask("The operation has no transaction set!")

            val blockHash = transaction.transactionMeta.appearsInBestChainBlock
                ?: failTask("Unable to retrieve block of proof from transaction")

            try {
                val block = miner.gateway.getBlock(blockHash)
                    ?: failTask("Unable to retrieve VBK block $blockHash")
                operation.setBlockOfProof(block)
            } catch (e: BlockStoreException) {
                failTask("Error when retrieving VBK block $blockHash")
            }
            logger.info(operation, "Successfully added the VBK block of proof!")
        }

        operation.runTask(
            taskName = "Prove Transaction",
            targetState = ApmOperationState.PROVEN,
            timeout = 20.sec
        ) {
            val endorsementTransaction = operation.endorsementTransaction
                ?: failTask("ProveTransactionTask called without VBK endorsement transaction!")
            val blockOfProof = operation.blockOfProof
                ?: failTask("ProveTransactionTask called without VBK block of proof!")

            val walletTransaction = endorsementTransaction.transaction

            logger.info(operation, "Getting the merkle path for the transaction: ${walletTransaction.id}...")
            val merklePath = walletTransaction.merklePath
                ?: failOperation("No merkle path found for ${walletTransaction.id}")
            logger.info(operation, "Successfully retrieved the merkle path for the transaction: ${walletTransaction.id}!")

            val vbkMerkleRoot = merklePath.merkleRoot.truncate()
            val verified = vbkMerkleRoot == blockOfProof.merkleRoot
            if (!verified) {
                failOperation(
                    "Unable to verify merkle path! VBK Transaction's merkle root: $vbkMerkleRoot;" +
                        " Block of proof's merkle root: ${blockOfProof.merkleRoot}"
                )
            }

            operation.setMerklePath(merklePath)
            logger.info(operation, "Successfully added the verified merkle path!")
        }

        operation.runTask(
            taskName = "Submit Proof of Proof",
            targetState = ApmOperationState.SUBMITTED_POP_DATA,
            timeout = 240.hr
        ) {
            verifyAltchainStatus(operation.chain.name, operation.chainMonitor)
            val endorsementTransaction = operation.endorsementTransaction
                ?: failTask("SubmitProofOfProofTask called without endorsement transaction!")
            val blockOfProof = operation.blockOfProof
                ?: failTask("SubmitProofOfProofTask called without block of proof!")
            val merklePath = operation.merklePath
                ?: failTask("SubmitProofOfProofTask called without merkle path!")

            try {
                val proofOfProof = AltPublication(
                    endorsementTransaction.transaction,
                    merklePath,
                    blockOfProof
                )

                operation.chain.submitAtvs(listOf(proofOfProof))
                val atvId = proofOfProof.getId().toHex()

                val chainName = operation.chain.name
                logger.info(operation, "ATV submitted to $chainName! $chainName ATV Id: $atvId")

                operation.setAtvId(atvId)
            } catch (e: Exception) {
                logger.debugWarn(e) { "Error submitting proof of proof" }
                failTask("Error submitting proof of proof to ${operation.chain.name}: ${e.message}")
            }
        }

        operation.runTask(
            taskName = "Payout Detection",
            targetState = ApmOperationState.PAYOUT_DETECTED,
            timeout = 10.days
        ) {
            verifyAltchainStatus(operation.chain.name, operation.chainMonitor)
            val miningInstruction = operation.miningInstruction
                ?: failTask("PayoutDetectionTask called without mining instruction!")
            val atvId = operation.atvId
                ?: failTask("Can't get ATV id")

            val chainName = operation.chain.name
            val endorsedBlockHeight = miningInstruction.endorsedBlockHeight
            val payoutDelay = operation.chain.getPayoutDelay()
            val payoutBlockHeight = endorsedBlockHeight + payoutDelay

            logger.debug(
                operation,
                "$chainName computed payout block height: $payoutBlockHeight ($endorsedBlockHeight + $payoutDelay)"
            )

            logger.info(operation, "Waiting for $chainName payout block ($payoutBlockHeight)...")

            operation.chainMonitor.getAtv(atvId) { atv ->
                if (atv.confirmations == 0) {
                    // atv is in mempool, continue waiting...
                    operation.atvReorganized()
                    false
                } else {
                    // atv is in a block
                    val containingBlock = operation.chain.getBlock(atv.containingBlock)
                    if (containingBlock == null) {
                        // if this happens, then this may be concurrency issue - when at the moment of 'getAtv' execution ATV was in a block,
                        // then suddenly block reorganized and 'getBlock' returned null.
                        // on next iteration we will find out.
                        operation.atvReorganized()
                        false
                    } else {
                        // we expect to get at least this amount of confirmations to wait for payout block
                        val requiredConfirmations = payoutBlockHeight - containingBlock.height
                        operation.requiredConfirmations = requiredConfirmations
                        operation.currentConfirmations = atv.confirmations
                        operation.atvBlock = containingBlock
                        atv.confirmations >= requiredConfirmations
                    }
                }
            }

            // this should be instant
            val payoutBlock = operation.chainMonitor.getBlockAtHeight(payoutBlockHeight)
            // we know payout block hash, so give `getTransaction` a hint where to search.
            // in reality, this is required for BTC-plugin, because not all nodes may have 'txindex=1' enabled,
            // and if it is disabled (by default), this call fails immediately. A hint resolves this situation
            // by providing a block hash where node has to search for this particular TX.
            val coinbaseTransaction = operation.chain.getTransaction(payoutBlock.coinbaseTransactionId, payoutBlock.hash)
                ?: failTask("Unable to find transaction ${payoutBlock.coinbaseTransactionId}")
            val rewardVout = coinbaseTransaction.vout.find {
                it.addressHex.asHexBytes().contentEquals(miningInstruction.publicationData.payoutInfo)
            }
            if (rewardVout != null) {
                // TODO: this looks like something hacky, figure out how to handle
                val payoutName = if (operation.chain.key.startsWith("v", true)) {
                    operation.chain.key.toUpperCase().decapitalize()
                } else {
                    operation.chain.key.toUpperCase()
                }
                logger.info(
                    operation,
                    "$chainName PoP Payout detected! Amount: ${rewardVout.value.formatAtomicLongWithDecimal()} $payoutName"
                )
                logger.info(operation, "Completed!")
                operation.setPayoutData(payoutBlock.hash, rewardVout.value)
            } else {
                failOperation(
                    "Unable to find ${operation.chain.name} PoP payout transaction in the expected block's coinbase!" +
                        " Expected payout block: ${payoutBlock.hash} @ ${payoutBlock.height}"
                )
            }
        }

        operation.complete()
    }

    private fun verifySpvStatus() {
        if (!miner.network.isReady()) {
            throw TaskException(
                "SPV is not ready: Connection ${miner.network.isAccessible()}," +
                    " Synchronized: ${miner.network.isSynchronized()}" +
                    " (${miner.network.latestSpvStateInfo.getSynchronizedMessage()})"
            )
        }
    }

    private fun verifyAltchainStatus(chainName: String, chainMonitor: SecurityInheritingMonitor) {
        if (!chainMonitor.isReady()) {
            throw TaskException(
                "$chainName is not ready: Connection: ${chainMonitor.isAccessible()}," +
                    " SameNetwork: ${chainMonitor.isOnSameNetwork()}," +
                    " Synchronized: ${chainMonitor.isSynchronized()}" +
                    " (${chainMonitor.latestBlockChainInfo.getSynchronizedMessage()})"
            )
        }
    }
}

class AltchainBlockReorgException(
    val block: SecurityInheritingBlock
) : IllegalStateException("There was a reorg leaving block ${block.hash} out of the main chain!")

class AltchainTransactionReorgException(
    val transaction: SecurityInheritingTransaction
) : IllegalStateException("There was a reorg leaving transaction ${transaction.txId} out of the main chain!")
