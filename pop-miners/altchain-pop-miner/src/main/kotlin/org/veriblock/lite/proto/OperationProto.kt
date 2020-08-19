package org.veriblock.lite.proto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

object OperationProto {
    @Serializable
    data class PublicationData(
        @ProtoNumber(1) val identifier: Long = 0,
        @ProtoNumber(2) val header: ByteArray = ByteArray(0),
        @ProtoNumber(3) val payoutInfo: ByteArray = ByteArray(0),
        @ProtoNumber(4) val veriblockContext: ByteArray = ByteArray(0)
    )

    @Serializable
    data class PopTransaction(
        @ProtoNumber(1) val txId: String,
        @ProtoNumber(2) val address: String,
        @ProtoNumber(3) val publishedBlock: ByteArray,
        @ProtoNumber(4) val bitcoinTx: ByteArray,
        @ProtoNumber(5) val merklePath: String,
        @ProtoNumber(6) val blockOfProof: ByteArray,
        @ProtoNumber(7) val bitcoinContext: List<ByteArray> = emptyList(),
        @ProtoNumber(8) val signature: ByteArray,
        @ProtoNumber(9) val publicKey: ByteArray,
        @ProtoNumber(10) val networkByte: Int
    )

    @Serializable
    data class VeriBlockPublication(
        @ProtoNumber(1) val transaction: PopTransaction,
        @ProtoNumber(2) val merklePath: String,
        @ProtoNumber(3) val containingBlock: ByteArray,
        @ProtoNumber(4) val context: List<ByteArray> = emptyList()
    )

    @Serializable
    data class Operation(
        @ProtoNumber(1) val operationId: String,
        @ProtoNumber(2) val chainId: String,
        @ProtoNumber(3) val state: Int,
        @ProtoNumber(4) val blockHeight: Int,
        @ProtoNumber(5) val publicationData: PublicationData,
        @ProtoNumber(6) val publicationContext: List<ByteArray> = emptyList(),
        @ProtoNumber(7) val publicationBtcContext: List<ByteArray> = emptyList(),
        @ProtoNumber(8) val txId: String,
        @ProtoNumber(9) val blockOfProof: ByteArray,
        @ProtoNumber(10) val merklePath: String,
        @ProtoNumber(11) val keystoneOfProof: ByteArray,
        @ProtoNumber(12) val veriblockPublications: List<VeriBlockPublication> = emptyList(),
        @ProtoNumber(13) val popTxId: String,
        @ProtoNumber(14) val popTxBlockHash: String,
        @ProtoNumber(15) val payoutBlockHash: String,
        @ProtoNumber(16) val payoutAmount: Long,
        @ProtoNumber(17) val failureReason: String
    )
}
