@file:OptIn(ExperimentalSerializationApi::class)

package org.veriblock.miners.pop.proto

import kotlinx.serialization.ExperimentalSerializationApi
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
    data class Operation(
        @ProtoNumber(1) val operationId: String,
        @ProtoNumber(2) val chainId: String,
        @ProtoNumber(3) val state: Int,
        @ProtoNumber(4) val blockHeight: Int,
        @ProtoNumber(5) val miningInstruction: PublicationData,
        @ProtoNumber(6) val publicationContext: List<ByteArray> = emptyList(),
        @ProtoNumber(7) val publicationBtcContext: List<ByteArray> = emptyList(),
        @ProtoNumber(8) val txId: String,
        @ProtoNumber(9) val blockOfProof: ByteArray,
        @ProtoNumber(10) val merklePath: String,
        @ProtoNumber(13) val atvId: String,
        // number 14 is missing (previously: AtvBlockHash)
        @ProtoNumber(15) val payoutBlockHash: String,
        @ProtoNumber(16) val payoutAmount: Long,
        @ProtoNumber(17) val failureReason: String
    )
}
