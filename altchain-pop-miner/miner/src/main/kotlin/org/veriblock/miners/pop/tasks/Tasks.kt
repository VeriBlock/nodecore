// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.tasks

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import org.veriblock.alt.plugins.HttpException
import org.veriblock.core.altchain.checkForValidEndorsement
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.lite.core.TransactionMeta
import org.veriblock.miners.pop.Miner
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.miners.pop.core.OperationState
import org.veriblock.miners.pop.core.info
import org.veriblock.miners.pop.securityinheriting.AltchainBlockHeightListener
import org.veriblock.miners.pop.securityinheriting.AltchainTransactionListener
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingMonitor
import org.veriblock.miners.pop.util.VTBDebugUtility
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.models.AltPublication
import org.veriblock.sdk.models.BlockStoreException
import org.veriblock.sdk.models.Sha256Hash
import org.veriblock.sdk.services.SerializeDeserializeService
import org.veriblock.sdk.util.Utils

private val logger = createLogger {}

// FIXME
inline fun failOperation(operation: MiningOperation, reason: String): Nothing {
    operation.fail(reason)
    throw ProcessException(reason)
}

// FIXME
inline fun failTask(reason: String): Nothing {
    throw TaskException(reason)
}

suspend fun runTasks(
    miner: Miner,
    nodeCoreLiteKit: NodeCoreLiteKit,
    securityInheritingChain: SecurityInheritingChain,
    securityInheritingMonitor: SecurityInheritingMonitor
    operation: MiningOperation
) {
    // GetPublicationDataTask
    run {
        logger.info(operation) { "Getting the publication data..." }
        val state = operation.state
        if (state is OperationState.PublicationData) {
            logger.info(operation) { "Successfully retrieved the publication data!" }
        } else {
            try {
                val publicationData = securityInheritingChain.getPublicationData(operation.blockHeight)
                operation.setPublicationDataWithContext(publicationData)
                logger.info(operation) { "Successfully added the publication data!" }
                return@run
            } catch (e: HttpException) {
                failOperation(
                    operation,
                    "Http error (${e.responseStatusCode}) while trying to get PoP publication data from the ${operation.chainId} daemon: ${e.message}"
                )
            } catch (e: Exception) {
                failOperation(operation, "Error while trying to get PoP publication data from the ${operation.chainId} daemon: ${e.message}")
            }
        }
    }

    // CreateProofTransactionTask
    run {
        val state = operation.state
        if (state is OperationState.EndorsementTransaction) {
            logger.info(operation) { "Successfully retrieved the VBK transaction: ${state.transaction.id}!" }
            return@run
        }

        if (state !is OperationState.PublicationData) {
            failTask("CreateProofTransactionTask called without publication data!")
        }

        // Something to fill in all the gaps
        logger.info(operation) { "Submitting endorsement VBK transaction..." }
        val transaction = try {
            val endorsementData = SerializeDeserializeService.serialize(state.publicationDataWithContext.publicationData)
            endorsementData.checkForValidEndorsement {
                logger.error(it) { "Invalid endorsement data" }
                failOperation(operation, "Invalid endorsement data: ${endorsementData.toHex()}")
            }
            nodeCoreLiteKit.network.submitEndorsement(
                endorsementData,
                miner.feePerByte,
                miner.maxFee
            )
        } catch (e: Exception) {
            failOperation(operation, "Could not create endorsement VBK transaction: ${e.message}")
        }

        val walletTransaction = nodeCoreLiteKit.transactionMonitor.getTransaction(transaction.id)
        operation.setTransaction(walletTransaction)
        logger.info(operation) { "Successfully added the VBK transaction: ${walletTransaction.id}!" }
        logger.info(operation) { "Waiting for the transaction to be included in VeriBlock block..." }

        // We will wait for the transaction to be confirmed, which will trigger DetermineBlockOfProofTask
        val txMetaChannel = walletTransaction.transactionMeta.stateChangedBroadcastChannel.openSubscription()
        txMetaChannel.receive() // Skip first state change (PENDING)
        do {
            val metaState = txMetaChannel.receive()
            if (metaState === TransactionMeta.MetaState.PENDING) {
                operation.onReorganized()
                return
            }
        } while (metaState !== TransactionMeta.MetaState.CONFIRMED)
        txMetaChannel.cancel()

        // Transaction has been confirmed!
        operation.setConfirmed()
    }

    // DetermineBlockOfProofTask
    run {
        val state = operation.state
        val transaction = (state as? OperationState.EndorsementTransaction)?.transaction
            ?: failTask("The operation has no transaction set!")

        val blockHash = transaction.transactionMeta.appearsInBestChainBlock
            ?: failTask("Unable to retrieve block of proof from transaction")

        try {
            val block = nodeCoreLiteKit.blockChain.get(blockHash)
                ?: failTask("Unable to retrieve VBK block $blockHash")
            operation.setBlockOfProof(block)
            logger.info(operation) { "Successfully added the VBK block of proof!" }
        } catch (e: BlockStoreException) {
            failTask("Error when retrieving VBK block $blockHash")
        }
    }

    // ProveTransactionTask
    run {
        val state = operation.state
        if (state !is OperationState.BlockOfProof) {
            failTask("ProveTransactionTask called without VBK block of proof!")
        }
        val walletTransaction = state.transaction

        logger.info(operation) { "Getting the merkle path for the transaction: ${walletTransaction.id}..." }
        val merklePath = walletTransaction.merklePath
            ?: failOperation(operation, "No merkle path found for ${walletTransaction.id}")
        logger.info(operation) { "Successfully retrieved the merkle path for the transaction: ${walletTransaction.id}!" }

        val vbkMerkleRoot = merklePath.merkleRoot.trim(Sha256Hash.VERIBLOCK_MERKLE_ROOT_LENGTH)
        val verified = vbkMerkleRoot == state.blockOfProof.merkleRoot
        if (!verified) {
            failOperation(
                operation,
                "Unable to verify merkle path! VBK Transaction's merkle root: $vbkMerkleRoot; Block of proof's merkle root: ${state.blockOfProof.merkleRoot}"
            )
        }

        operation.setMerklePath(merklePath)
        logger.info(operation) { "Successfully added the verified merkle path!" }
    }

    // RegisterKeystoneListenersTask
    run {
        val state = operation.state
        if (state !is OperationState.BlockOfProof) {
            failTask("RegisterKeystoneListenersTask called without block of proof!")
        }

        val blockOfProof = state.blockOfProof

        logger.info(operation) { "Waiting for the next VBK Keystone..." }
        val keystoneOfProof = nodeCoreLiteKit.blockChain.newBestBlockChannel.asFlow().first {
            it.height == blockOfProof.height / 20 * 20 + 20
        }
        operation.setKeystoneOfProof(keystoneOfProof)
    }

    // RegisterVeriBlockPublicationPollingTask
    run {
        val state = operation.state
        if (state !is OperationState.KeystoneOfProof) {
            failTask("RegisterVeriBlockPublicationPollingTask called without keystone of proof!")
        }
        // We will be waiting for this operation's veriblock publication, which will trigger the SubmitProofOfProofTask
        val publications = nodeCoreLiteKit.network.getVeriBlockPublications(
            operation.id, state.keystoneOfProof.hash.toString(),
            Utils.encodeHex(state.publicationDataWithContext.context[0]),
            Utils.encodeHex(state.publicationDataWithContext.btcContext[0])
        )
        operation.setVeriBlockPublications(publications)
    }

    // SubmitProofOfProofTask
    run {
        val state = operation.state
        if (state !is OperationState.VeriBlockPublications) {
            failTask("SubmitProofOfProofTask called without VeriBlock publications!")
        }
        try {
            val proofOfProof = AltPublication(
                state.transaction,
                state.merklePath,
                state.blockOfProof,
                emptyList()
            )

            val veriBlockPublications = state.veriBlockPublications

            try {
                val context = state.publicationDataWithContext
                val btcContext = context.btcContext
                // List<byte[]> vbkContext = context.getContext();

                // Check that the first VTB connects somewhere in the BTC context
                val firstPublication = veriBlockPublications[0]

                val serializedAltchainBTCContext = btcContext.joinToString("\n") { Utility.bytesToHex(it) }

                val serializedBTCHashesInPoPTransaction = VTBDebugUtility.serializeBitcoinBlockHashList(
                    VTBDebugUtility.extractOrderedBtcBlocksFromPopTransaction(
                        firstPublication.transaction
                    )
                )

                if (!VTBDebugUtility.vtbConnectsToBtcContext(btcContext, firstPublication)) {
                    logger.error {
                        """Error: the first VeriBlock Publication with PoP TxID ${firstPublication.transaction.id} does not connect to the altchain context!
                           Altchain Bitcoin Context:
                           $serializedAltchainBTCContext
                           PoP Transaction Bitcoin blocks: $serializedBTCHashesInPoPTransaction""".trimIndent()
                    }
                } else {
                    logger.debug {
                        """Success: the first VeriBlock Publication with PoP TxID ${firstPublication.transaction.id} connects to the altchain context!
                           Altchain Bitcoin Context:
                           $serializedAltchainBTCContext
                           PoP Transaction Bitcoin blocks: $serializedBTCHashesInPoPTransaction""".trimIndent()
                    }
                }

                // Check that every VTB connects to the previous one
                for (i in 1 until veriBlockPublications.size) {
                    val anchor = veriBlockPublications[i - 1]
                    val toConnect = veriBlockPublications[i]

                    val anchorBTCBlocks = VTBDebugUtility.extractOrderedBtcBlocksFromPopTransaction(anchor.transaction)
                    val toConnectBTCBlocks = VTBDebugUtility.extractOrderedBtcBlocksFromPopTransaction(toConnect.transaction)

                    val serializedAnchorBTCBlocks = VTBDebugUtility.serializeBitcoinBlockHashList(anchorBTCBlocks)
                    val serializedToConnectBTCBlocks = VTBDebugUtility.serializeBitcoinBlockHashList(toConnectBTCBlocks)

                    if (!VTBDebugUtility.doVtbsConnect(anchor, toConnect)) {
                        logger.error {
                            """Error: VTB at index $i does not connect to the previous VTB!
                               VTB #${i - 1} BTC blocks:
                               $serializedAnchorBTCBlocks
                               VTB #$i BTC blocks:
                               $serializedToConnectBTCBlocks""".trimIndent()
                        }
                    } else {
                        logger.debug { "Success, VTB at index $i connects to VTB at index ${i - 1}!" }
                    }
                }
            } catch (e: Exception) {
                logger.error("An error occurred checking VTB connection and continuity!", e)
            }

            val siTxId = securityInheritingChain.submit(proofOfProof, veriBlockPublications)

            val chainSymbol = operation.chainId.toUpperCase()
            logger.info(operation) { "VTB submitted to $chainSymbol! $chainSymbol PoP TxId: $siTxId" }

            nodeCoreLiteKit.network.removeVeriBlockPublicationSubscription(operation.id)
            logger.info(operation) { "Successfully removed the publication subscription!" }

            operation.setProofOfProofId(siTxId)
            logger.info(operation) { "Waiting for $chainSymbol PoP Transaction to be confirmed..." }
        } catch (e: Exception) {
            logger.error("Error submitting proof of proof", e)
            failTask("Error submitting proof of proof")
        }
    }

    // DeregisterVeriBlockPublicationPollingTask
    run {
        nodeCoreLiteKit.network.removeVeriBlockPublicationSubscription(operation.id)
        logger.info(operation) { "Successfully removed the publication subscription!" }
    }
}

/*
TODO
class AltEndorsementTransactionConfirmationTask(
    miner: Miner,
    nodeCoreLiteKit: NodeCoreLiteKit,
    securityInheritingChain: SecurityInheritingChain,
    securityInheritingMonitor: SecurityInheritingMonitor
) : BaseTask(
    miner, nodeCoreLiteKit, securityInheritingChain, securityInheritingMonitor
) {
    override val next: BaseTask?
        get() = null

    override fun executeImpl(operation: MiningOperation) {
        val state = operation.state
        if (state !is OperationState.SubmittedPopData) {
            failTask("AltEndorsementTransactionConfirmationTask called without proof of proof txId!")
        }

        securityInheritingMonitor.registerTransactionListener(AltchainTransactionListener(
            txId = state.proofOfProofId,
            onComplete = { _ ->
                // Successfully confirmed endorsement
                val chainSymbol = operation.chainId.toUpperCase()
                logger.info(operation) { "Successfully confirmed $chainSymbol endorsement transaction!" }
                operation.setAltEndorsementTransactionConfirmed()
                logger.info(operation) { "Waiting for $chainSymbol endorsed block to be confirmed..." }
            },
            onError = {
                failOperation(operation, it.message ?: "")
            }
        ))
    }
}

class AltEndorsedBlockConfirmationTask(
    miner: Miner,
    nodeCoreLiteKit: NodeCoreLiteKit,
    securityInheritingChain: SecurityInheritingChain,
    securityInheritingMonitor: SecurityInheritingMonitor
) : BaseTask(
    miner, nodeCoreLiteKit, securityInheritingChain, securityInheritingMonitor
) {
    override val next: BaseTask?
        get() = null

    override fun executeImpl(operation: MiningOperation) {
        val state = operation.state
        if (state !is OperationState.AltEndorsementTransactionConfirmed) {
            failTask("AltEndorsedBlockConfirmationTask called without having confirmed the transaction!")
        }

        val endorsedBlockHeight = state.publicationDataWithContext.endorsedBlockHeight
        securityInheritingMonitor.registerBlockHeightListener(AltchainBlockHeightListener(
            blockHeight = endorsedBlockHeight,
            onComplete = { endorsedBlock ->
                val endorsedBlockHeader = state.publicationDataWithContext.publicationData.header
                val belongsToMainChain = securityInheritingChain.checkBlockIsOnMainChain(endorsedBlockHeight, endorsedBlockHeader)
                if (!belongsToMainChain) {
                    failOperation(
                        operation,
                        "Endorsed block header ${endorsedBlockHeader.toHex()} @ $endorsedBlockHeight is not in ${operation.chainId.toUpperCase()}'s main chain"
                    )
                }

                // Successfully confirmed endorsing block
                val chainSymbol = operation.chainId.toUpperCase()
                logger.info(operation) { "Successfully confirmed $chainSymbol endorsed block ${endorsedBlock.hash}!" }
                operation.setAltEndorsedBlockHash(endorsedBlock.hash)
                logger.info(operation) { "Waiting for $chainSymbol payout..." }
            },
            onError = {
                failOperation(operation, it.message ?: "")
            }
        ))
    }
}

class PayoutDetectionTask(
    miner: Miner,
    nodeCoreLiteKit: NodeCoreLiteKit,
    securityInheritingChain: SecurityInheritingChain,
    securityInheritingMonitor: SecurityInheritingMonitor
) : BaseTask(
    miner, nodeCoreLiteKit, securityInheritingChain, securityInheritingMonitor
) {
    override val next: BaseTask?
        get() = null

    override fun executeImpl(operation: MiningOperation) {
        val state = operation.state
        if (state !is OperationState.SubmittedPopData) {
            failTask("PayoutDetectionTask called without having confirmed the endorsed block!")
        }

        val endorsedBlockHeight = state.publicationDataWithContext.endorsedBlockHeight
        securityInheritingMonitor.registerBlockHeightListener(AltchainBlockHeightListener(
            blockHeight = endorsedBlockHeight + securityInheritingChain.getPayoutInterval(),
            onComplete = { payoutBlock ->
                val coinbaseTransaction = securityInheritingChain.getTransaction(payoutBlock.coinbaseTransactionId)
                    ?: error("Unable to find transaction ${payoutBlock.coinbaseTransactionId}")
                val rewardVout = coinbaseTransaction.vout.find {
                    it.addressHash == state.publicationDataWithContext.publicationData.payoutInfo.toHex()
                }
                if (rewardVout != null) {
                    logger.info(operation) {
                        "${operation.chainId.toUpperCase()} PoP Payout detected! Amount: ${rewardVout.value} ${operation.chainId.toUpperCase()}"
                    }
                    operation.complete(payoutBlock.hash, rewardVout.value)
                } else {
                    failOperation(
                        operation,
                        "Unable to find ${operation.chainId.toUpperCase()} PoP payout transaction in the expected block's coinbase!" +
                            " Expected payout block: ${payoutBlock.hash} @ ${payoutBlock.height}"
                    )
                }
            },
            onError = {
                failOperation(operation, it.message ?: "")
            }
        ))
    }
}
 */
