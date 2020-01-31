// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.tasks

import org.veriblock.alt.plugins.HttpException
import org.veriblock.core.altchain.checkForValidEndorsement
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.lite.core.FullBlock
import org.veriblock.lite.core.PublicationSubscription
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.miners.pop.core.OperationState
import org.veriblock.miners.pop.core.info
import org.veriblock.miners.pop.minerConfig
import org.veriblock.miners.pop.util.VTBDebugUtility
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.models.AltPublication
import org.veriblock.sdk.models.BlockStoreException
import org.veriblock.sdk.models.Sha256Hash
import org.veriblock.sdk.services.SerializeDeserializeService
import org.veriblock.sdk.util.Utils

private val logger = createLogger {}

class GetPublicationDataTask(
    nodeCoreLiteKit: NodeCoreLiteKit,
    securityInheritingChain: SecurityInheritingChain
) : BaseTask(
    nodeCoreLiteKit, securityInheritingChain
) {
    override val next: BaseTask?
        get() = CreateProofTransactionTask(nodeCoreLiteKit, securityInheritingChain)

    override fun executeImpl(operation: MiningOperation) {
        logger.info(operation) { "Getting the publication data..." }
        val state = operation.state
        if (state is OperationState.PublicationData) {
            logger.info(operation) { "Successfully retrieved the publication data!" }
            return
        }

        try {
            val publicationData = securityInheritingChain.getPublicationData(operation.blockHeight)
            operation.setPublicationDataWithContext(publicationData)
            logger.info(operation) { "Successfully added the publication data!" }
            return
        } catch (e: HttpException) {
            failOperation(operation, "Http error (${e.responseStatusCode}) while trying to get PoP publication data from the ${operation.chainId} daemon: ${e.message}")
        } catch (e: Exception) {
            failOperation(operation, "Error while trying to get PoP publication data from the ${operation.chainId} daemon: ${e.message}")
        }
    }
}

class CreateProofTransactionTask(
    nodeCoreLiteKit: NodeCoreLiteKit,
    securityInheritingChain: SecurityInheritingChain
) : BaseTask(
    nodeCoreLiteKit, securityInheritingChain
) {
    override val next: BaseTask?
        get() = null // We will wait for the transaction to be confirmed, which will trigger DetermineBlockOfProofTask

    override fun executeImpl(operation: MiningOperation) {
        val state = operation.state
        if (state is OperationState.EndorsementTransaction) {
            logger.info(operation) { "Successfully retrieved the VBK transaction: ${state.transaction.id}!" }
            return
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
                minerConfig.feePerByte,
                minerConfig.maxFee
            )
        } catch (e: Exception) {
            failOperation(operation, "Could not create endorsement VBK transaction")
        }

        val walletTransaction = nodeCoreLiteKit.transactionMonitor.getTransaction(transaction.id)
        operation.setTransaction(walletTransaction)
        logger.info(operation) { "Successfully added the VBK transaction: ${walletTransaction.id}!" }
        logger.info(operation) { "Waiting for the transaction to be included in VeriBlock block..." }
    }
}

class DetermineBlockOfProofTask(
    nodeCoreLiteKit: NodeCoreLiteKit,
    securityInheritingChain: SecurityInheritingChain
) : BaseTask(
    nodeCoreLiteKit, securityInheritingChain
) {
    override val next: BaseTask?
        get() = ProveTransactionTask(nodeCoreLiteKit, securityInheritingChain)

    override fun executeImpl(operation: MiningOperation) {
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
}

class ProveTransactionTask(
    nodeCoreLiteKit: NodeCoreLiteKit,
    securityInheritingChain: SecurityInheritingChain
) : BaseTask(
    nodeCoreLiteKit, securityInheritingChain
) {
    override val next: BaseTask?
        get() = RegisterKeystoneListenersTask(nodeCoreLiteKit, securityInheritingChain)

    override fun executeImpl(operation: MiningOperation) {
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
            failOperation(operation, "Unable to verify merkle path! VBK Transaction's merkle root: $vbkMerkleRoot; Block of proof's merkle root: ${state.blockOfProof.merkleRoot}")
        }

        operation.setMerklePath(merklePath)
        logger.info(operation) { "Successfully added the verified merkle path!" }
    }
}

class RegisterKeystoneListenersTask(
    nodeCoreLiteKit: NodeCoreLiteKit,
    securityInheritingChain: SecurityInheritingChain
) : BaseTask(
    nodeCoreLiteKit, securityInheritingChain
) {
    override val next: BaseTask?
        get() = null // We are going to wait for the next Keystone, which will trigger RegisterVeriBlockPublicationPollingTask

    override fun executeImpl(operation: MiningOperation) {
        val state = operation.state
        if (state !is OperationState.BlockOfProof) {
            failTask("RegisterKeystoneListenersTask called without block of proof!")
        }
        // Register to nodecore's block events
        nodeCoreLiteKit.blockChain.newBestBlockEvent.register(operation) { newBlock ->
            val blockOfProof = state.blockOfProof
            // Wait for block of proof's keystone
            if (newBlock.height == blockOfProof.height / 20 * 20 + 20) {
                // Found!
                handleFoundKeystone(operation, newBlock)
            }
        }
        nodeCoreLiteKit.blockChain.blockChainReorganizedEvent.register(operation) {
            val blockOfProof = state.blockOfProof
            // Consider looking for keystone when blockchain reorganizes as well
            for (newBlock in it.newBlocks) {
                if (newBlock.height == blockOfProof.height / 20 * 20 + 20) {
                    handleFoundKeystone(operation, newBlock)
                    break
                }
            }
        }
        logger.info(operation) { "Successfully subscribed to VBK's new best block and blockchain reorg events!" }
        logger.info(operation) { "Waiting for the next VBK Keystone..." }
    }

    private fun handleFoundKeystone(state: MiningOperation, newBlock: FullBlock) {
        state.setKeystoneOfProof(newBlock)
        nodeCoreLiteKit.blockChain.newBestBlockEvent.remove(state)
        nodeCoreLiteKit.blockChain.blockChainReorganizedEvent.remove(state)
    }
}

class RegisterVeriBlockPublicationPollingTask(
    nodeCoreLiteKit: NodeCoreLiteKit,
    securityInheritingChain: SecurityInheritingChain
) : BaseTask(
    nodeCoreLiteKit, securityInheritingChain
) {
    override val next: BaseTask?
        get() = null // We will be waiting for this operation's veriblock publication, which will trigger the SubmitProofOfProofTask

    override fun executeImpl(operation: MiningOperation) {
        val state = operation.state
        if (state !is OperationState.KeystoneOfProof) {
            failTask("RegisterVeriBlockPublicationPollingTask called without keystone of proof!")
        }
        val subscription = PublicationSubscription(
            state.keystoneOfProof.hash.toString(),
            Utils.encodeHex(state.publicationDataWithContext.context[0]),
            Utils.encodeHex(state.publicationDataWithContext.btcContext[0]),
            { publications ->
                operation.setVeriBlockPublications(publications)
            },
            { error ->
                nodeCoreLiteKit.network.removeVeriBlockPublicationSubscription(operation.id)
                operation.fail(error.message ?: "Unable to get publications")
            }
        )

        nodeCoreLiteKit.network.addVeriBlockPublicationSubscription(operation.id, subscription)
        logger.info(operation) {
            """Successfully added publication subscription!
                |   - Keystone Hash: ${subscription.keystoneHash}
                |   - VBK Context Hash: ${subscription.contextHash}
                |   - BTC Context Hash: ${subscription.btcContextHash}""".trimMargin()
        }
        logger.info(operation) { "Waiting for this operation's veriblock publication..." }
    }
}

class SubmitProofOfProofTask(
    nodeCoreLiteKit: NodeCoreLiteKit,
    securityInheritingChain: SecurityInheritingChain
) : BaseTask(
    nodeCoreLiteKit, securityInheritingChain
) {
    override val next: BaseTask?
        get() = DeregisterVeriBlockPublicationPollingTask(nodeCoreLiteKit, securityInheritingChain)

    override fun executeImpl(operation: MiningOperation) {
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
                        firstPublication.transaction))

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
                        logger.debug {"Success, VTB at index $i connects to VTB at index ${i - 1}!" }
                    }
                }
            } catch (e: Exception) {
                logger.error("An error occurred checking VTB connection and continuity!", e)
            }

            val siTxId = securityInheritingChain.submit(proofOfProof, veriBlockPublications)

            val chainSymbol = operation.chainId.toUpperCase()
            logger.info(operation) { "VTB submitted to $chainSymbol! $chainSymbol PoP TxId: $siTxId" }
            logger.info(operation) { "Waiting for VBK depth completion..." }
            operation.setProofOfProofId(siTxId)
        } catch (e: Exception) {
            logger.error("Error submitting proof of proof", e)
            failTask("Error submitting proof of proof")
        }
    }
}

class DeregisterVeriBlockPublicationPollingTask(
    nodeCoreLiteKit: NodeCoreLiteKit,
    securityInheritingChain: SecurityInheritingChain
) : BaseTask(
    nodeCoreLiteKit, securityInheritingChain
) {
    override val next: BaseTask?
        get() = null

    override fun executeImpl(operation: MiningOperation) {
        nodeCoreLiteKit.network.removeVeriBlockPublicationSubscription(operation.id)
        logger.info(operation) { "Successfully removed the publication subscription!" }
    }
}
