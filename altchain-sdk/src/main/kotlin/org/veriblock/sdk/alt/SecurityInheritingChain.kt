// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.alt

import org.veriblock.core.altchain.AltchainPoPEndorsement
import org.veriblock.core.contracts.BlockEvidence
import org.veriblock.sdk.alt.model.PopMempool
import org.veriblock.sdk.alt.model.SecurityInheritingBlock
import org.veriblock.sdk.alt.model.SecurityInheritingTransaction
import org.veriblock.sdk.models.AltPublication
import org.veriblock.sdk.models.StateInfo
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockPublication

interface SecurityInheritingChain {

    /**
     * Common configuration for the chain.
     */
    suspend fun getConfig(): ChainConfig

    /**
     * Returns this security inheriting chain's unique identifier.
     */
    suspend fun getId(): Long

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
    suspend fun getBestBlockHeight(): Int

    /**
     * Returns the block for the given [hash] retrieved from the SI chain daemon,
     * or null if it does not exist.
     */
    suspend fun getBlock(hash: String): SecurityInheritingBlock?

    /**
     * Returns the block in the best chain for the given [height] retrieved from the SI chain daemon,
     * or null if the height is out of the chain's bounds.
     */
    suspend fun getBlock(height: Int): SecurityInheritingBlock?

    /**
     * Retrieves the block in the best chain for the given [height] from the SI chain daemon.
     * If it does not exist, it returns false.
     * If it exists, it compares returns the comparison result of its header with [blockHeaderToCheck].
     */
    suspend fun checkBlockIsOnMainChain(height: Int, blockHeaderToCheck: ByteArray): Boolean

    /**
     * Returns the transaction for the given [txId] retrieved from the SI chain daemon,
     * or null if it does not exist.
     */
    suspend fun getTransaction(txId: String): SecurityInheritingTransaction?

    /**
     * Returns this security inheriting chain's best known VeriBlock Block hash.
     */
    suspend fun getVbkBestKnownBlockHash(): String

    /** 
     * Returns this security inheriting chain's VBK block 
     */
    suspend fun getVbkBlock(hash: String): VbkBlock

    /**
     * Returns this security inheriting chain's best known Bitcoin Block hash.
     */
    suspend fun getBtcBestKnownBlockHash(): String

    /**
     * Returns this security inheriting chain's BTC block 
     */
    suspend fun getBtcBlock(hash: String): BtcBlock

    /**
     * Returns this security inheriting chain's PoP mempool (ATVs and VTBs).
     */
    suspend fun getPopMempool(): PopMempool

    /**
     * Retrieves mining instruction from the SI chain for the given [blockHash] (or the best block hash
     * if [block hash] is null).
     */
    suspend fun getMiningInstruction(blockHash: String? = null): ApmInstruction

    suspend fun submitAtv(atv: AltPublication): ValidationState
    suspend fun submitVtb(vtb: VeriBlockPublication): ValidationState
    suspend fun submitVbk(vbk: VbkBlock): ValidationState

    /**
     * Extracts an address' display string from the given data (coming from the Mining Instruction)
     */
    fun extractAddressDisplay(addressData: ByteArray): String

    /**
     * Extracts a block endorsement from the given data (coming from a VBK PoP Transaction)
     */
    fun extractBlockEvidence(altchainPopEndorsement: AltchainPoPEndorsement): BlockEvidence

    /**
     * Whether or not this chain is configured to perform any automining
     */
    fun shouldAutoMine(): Boolean = config.shouldAutoMine()

    /**
     * Whether or not this chain is configured to automine the block at the given [blockHeight]
     */
    fun shouldAutoMine(blockHeight: Int): Boolean = config.shouldAutoMine(blockHeight)

    /**
     * @return the block chain information
     */
    suspend fun getBlockChainInfo(): StateInfo
}

data class ValidationState(
    val valid: Boolean,
    val code: String? = null,
    val message: String? = null
)