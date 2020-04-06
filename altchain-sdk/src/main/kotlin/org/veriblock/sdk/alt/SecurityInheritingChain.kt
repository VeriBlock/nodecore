// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.alt

import org.veriblock.core.altchain.AltchainPoPEndorsement
import org.veriblock.core.contracts.BlockEndorsement
import org.veriblock.sdk.alt.model.SecurityInheritingBlock
import org.veriblock.sdk.alt.model.SecurityInheritingTransaction
import org.veriblock.sdk.models.AltPublication
import org.veriblock.sdk.models.VeriBlockPublication

interface SecurityInheritingChain {

    /**
     * Common configuration for the chain.
     */
    val config: ChainConfig

    /**
     * Returns this security inheriting chain's unique identifier.
     */
    val id: Long

    /**
     * Returns this security inheriting chain's key, usually the token symbol.
     */
    val key: String

    /**
     * Returns this security inheriting chain's name.
     */
    val name: String

    /**
     * Returns the best block height retrieved from the SI chain daemon.
     */
    fun getBestBlockHeight(): Int

    /**
     * Returns the block for the given [hash] retrieved from the SI chain daemon,
     * or null if it does not exist.
     */
    fun getBlock(hash: String): SecurityInheritingBlock?

    /**
     * Returns the block in the best chain for the given [height] retrieved from the SI chain daemon,
     * or null if the height is out of the chain's bounds.
     */
    fun getBlock(height: Int): SecurityInheritingBlock?

    /**
     * Retrieves the block in the best chain for the given [height] from the SI chain daemon.
     * If it does not exist, it returns false.
     * If it exists, it compares returns the comparison result of its header with [blockHeaderToCheck].
     */
    fun checkBlockIsOnMainChain(height: Int, blockHeaderToCheck: ByteArray): Boolean

    /**
     * Returns the transaction for the given [txId] retrieved from the SI chain daemon,
     * or null if it does not exist.
     */
    fun getTransaction(txId: String): SecurityInheritingTransaction?

    /**
     * Returns this security inheriting chain's payout interval, in blocks.
     */
    fun getPayoutInterval(): Int

    /**
     * Retrieves mining instruction from the SI chain for the given [blockHeight] (or the best block height
     * if [blockHeight] is null).
     */
    fun getMiningInstruction(blockHeight: Int?): ApmInstruction

    /**
     * Submits ATV ([proofOfProof]) and VTBs ([veriBlockPublications]) to the altchain
     * @return The submission's resulting pop transaction hash
     */
    fun submit(proofOfProof: AltPublication, veriBlockPublications: List<VeriBlockPublication>): String

    /**
     * Updates the chain's context with a set of VTBs ([veriBlockPublications])
     */
    fun updateContext(veriBlockPublications: List<VeriBlockPublication>): String

    /**
     * Extracts a block endorsement from the given data (coming from a VBK PoP Transaction)
     */
    fun extractBlockEndorsement(altchainPopEndorsement: AltchainPoPEndorsement): BlockEndorsement

    /**
     * Whether or not this chain is configured to perform any automining
     */
    fun shouldAutoMine(): Boolean = config.shouldAutoMine()

    /**
     * Whether or not this chain is configured to automine the block at the given [blockHeight]
     */
    fun shouldAutoMine(blockHeight: Int): Boolean = config.shouldAutoMine(blockHeight)

    /**
     * @return true if the chain is connected
     */
    fun isConnected(): Boolean

    /**
     * @return true if the chain is synchronized
     */
    fun isSynchronized(): Boolean
}
