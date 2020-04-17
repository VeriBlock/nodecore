package org.veriblock.lite.net

import nodecore.api.grpc.VeriBlockMessages
import org.veriblock.core.contracts.AddressManager
import org.veriblock.lite.core.Balance
import org.veriblock.sdk.models.Sha256Hash
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

    fun getLastVBKBlockHeader(): VeriBlockBlock

    fun getVBKBlockHeader(blockHash: ByteArray): VeriBlockBlock

    fun getTransactions(request: List<Sha256Hash>): List<VeriBlockMessages.TransactionInfo>?

    data class NodeCoreSyncStatus(
        val networkHeight: Int,
        val localBlockchainHeight: Int,
        val blockDifference: Int,
        val isSynchronized: Boolean
    )

}
