// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.alt

import org.veriblock.core.altchain.AltchainPopEndorsement
import org.veriblock.core.contracts.BlockEvidence
import org.veriblock.sdk.alt.model.Atv
import org.veriblock.sdk.alt.model.PopMempool
import org.veriblock.sdk.alt.model.PopParamsResponse
import org.veriblock.sdk.alt.model.SecurityInheritingBlock
import org.veriblock.sdk.alt.model.SecurityInheritingTransaction
import org.veriblock.sdk.alt.model.SubmitPopResponse
import org.veriblock.sdk.alt.model.Vtb
import org.veriblock.sdk.models.*

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
    suspend fun getTransaction(txId: String, blockHash: String? = null): SecurityInheritingTransaction?

    /**
     * Returns this security inheriting chain's payout delay, in blocks.
     */
    fun getPayoutDelay(): Int

    /**
     * Returns this security inheriting chain's best known VeriBlock Block hash.
     */
    suspend fun getBestKnownVbkBlockHash(): String

    suspend fun getPopParams(): PopParamsResponse

    /**
     * Returns the VeriBlockBlock for the given [hash]
     */
    suspend fun getVbkBlock(hash: String): VeriBlockBlock?

    /**
     * Returns the hash from the best known BTC block
     */
    suspend fun getBestKnownBtcBlockHash(): String

    /**
     * Returns the BitcoinBlock for the given [hash]
     */
    suspend fun getBtcBlock(hash: String): BitcoinBlock?

    /**
     * Returns this security inheriting chain's PoP mempool (ATVs and VTBs).
     */
    suspend fun getPopMempool(): PopMempool

    /**
     * Retrieves an ATV by id, or null if it does not exist.
     */
    suspend fun getAtv(id: String): Atv?

    /**
     * Retrieves a VTB by id, or null if it does not exist.
     */
    suspend fun getVtb(id: String): Vtb?

    /**
     * Retrieves mining instruction from the SI chain for the given [blockHeight] (or the best block height
     * if [blockHeight] is null).
     */
    suspend fun getMiningInstructionByHeight(blockHeight: Int? = null): ApmInstruction

    suspend fun submitPopVbk(block: VeriBlockBlock): SubmitPopResponse

    suspend fun submitPopAtv(atv: AltPublication): SubmitPopResponse

    suspend fun submitPopVtb(vtb: VeriBlockPublication): SubmitPopResponse

    /**
     * Extracts an address' display string from the given data (coming from the Mining Instruction)
     */
    fun extractAddressDisplay(addressData: ByteArray): String

    /**
     * Extracts a block endorsement from the given data (coming from a VBK PoP Transaction)
     */
    suspend fun extractBlockEvidences(altchainPopEndorsements: List<AltchainPopEndorsement>): List<BlockEvidence>

    /**
     * Whether or not this chain is configured to perform any automining
     */
    fun shouldAutoMine(): Boolean = config.shouldAutoMine()

    /**
     * Whether or not this chain is configured to automine the block at the given [blockHeight]
     */
    fun shouldAutoMine(blockHeight: Int): Boolean = config.shouldAutoMine(blockHeight)

    fun validatePayoutAddress()

    /**
     * @return the block chain information
     */
    suspend fun getBlockChainInfo(): StateInfo

    suspend fun getMissingBtcBlockHashes(): List<String>
}
