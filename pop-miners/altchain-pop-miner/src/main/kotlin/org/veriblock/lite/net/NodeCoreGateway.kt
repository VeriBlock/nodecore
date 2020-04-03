package org.veriblock.lite.net

import org.veriblock.core.contracts.AddressManager
import org.veriblock.lite.core.Balance
import org.veriblock.lite.core.BlockChainDelta
import org.veriblock.lite.core.FullBlock
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockPublication
import org.veriblock.sdk.models.VeriBlockTransaction

interface NodeCoreGateway {

    fun getBalance(address: String): Balance

    fun getVeriBlockPublications(keystoneHash: String, contextHash: String, btcContextHash: String): List<VeriBlockPublication>

    fun getDebugVeriBlockPublications(vbkContextHash: String, btcContextHash: String): List<VeriBlockPublication>

    fun ping(): Boolean

    fun getNodeCoreSyncStatus(): NodeCoreSyncStatus

    fun submitEndorsementTransaction(publicationData: ByteArray, addressManager: AddressManager, feePerByte: Long, maxFee: Long): VeriBlockTransaction

    fun shutdown()

    data class NodeCoreSyncStatus(
        val networkHeight: Int,
        val localBlockchainHeight: Int,
        val blockDifference: Int,
        val isSynchronized: Boolean
    )

    //TODO remove on the next step
    fun listChangesSince(hash: String?): BlockChainDelta
    fun getBlock(height: Int): FullBlock?
    fun getBlock(hash: String): FullBlock?
    fun getLastBlock(): VeriBlockBlock

}
