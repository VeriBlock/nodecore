package org.veriblock.lite.proto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoId

object OperationProto {
    @Serializable
    data class PublicationData(
        @ProtoId(1) val identifier: Long = 0,
        @ProtoId(2) val header: ByteArray = ByteArray(0),
        @ProtoId(3) val payoutInfo: ByteArray = ByteArray(0),
        @ProtoId(4) val veriblockContext: ByteArray = ByteArray(0)
    )

    @Serializable
    data class PopTransaction(
        @ProtoId(1) val txId: String,
        @ProtoId(2) val address: String,
        @ProtoId(3) val publishedBlock: ByteArray,
        @ProtoId(4) val bitcoinTx: ByteArray,
        @ProtoId(5) val merklePath: String,
        @ProtoId(6) val blockOfProof: ByteArray,
        @ProtoId(7) val bitcoinContext: List<ByteArray> = emptyList(),
        @ProtoId(8) val signature: ByteArray,
        @ProtoId(9) val publicKey: ByteArray,
        @ProtoId(10) val networkByte: Int
    )

    @Serializable
    data class VeriBlockPublication(
        @ProtoId(1) val transaction: PopTransaction,
        @ProtoId(2) val merklePath: String,
        @ProtoId(3) val containingBlock: ByteArray,
        @ProtoId(4) val context: List<ByteArray> = emptyList()
    )

    @Serializable
    data class Operation(
        @ProtoId(1) val operationId: String,
        @ProtoId(2) val chainId: String,
        @ProtoId(3) val state: Int,
        @ProtoId(4) val blockHeight: Int,
        @ProtoId(5) val publicationData: PublicationData,
        @ProtoId(6) val publicationContext: List<ByteArray> = emptyList(),
        @ProtoId(7) val publicationBtcContext: List<ByteArray> = emptyList(),
        @ProtoId(8) val txId: String,
        @ProtoId(9) val blockOfProof: ByteArray,
        @ProtoId(10) val merklePath: String,
        @ProtoId(11) val veriblockPublications: List<VeriBlockPublication> = emptyList(),
        @ProtoId(12) val proofOfProofId: String,
        @ProtoId(13) val payoutBlockHash: String,
        @ProtoId(14) val payoutAmount: Long
    )
}
