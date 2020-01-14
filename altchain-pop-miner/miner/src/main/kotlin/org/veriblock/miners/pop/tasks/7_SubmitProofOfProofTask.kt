// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.tasks

import org.veriblock.core.utilities.Utility
import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.miners.pop.core.OperationState
import org.veriblock.miners.pop.core.info
import org.veriblock.miners.pop.util.VTBDebugUtility
import org.veriblock.sdk.models.AltPublication
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.core.utilities.createLogger

private val logger = createLogger {}

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
            logger.info(operation) { "VTB submitted to $chainSymbol! $chainSymbol PoP TxId: $siTxId. Waiting for VBK depth completion..." }
            operation.setProofOfProofId(siTxId)
        } catch (e: Exception) {
            logger.error("Error submitting proof of proof", e)
            failTask("Error submitting proof of proof")
        }
    }
}
