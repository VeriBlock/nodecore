@file:OptIn(ExperimentalSerializationApi::class)

package org.veriblock.miners.pop.model.proto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

object OperationProto {
    @Serializable
    data class MiningInstruction(
        @ProtoNumber(1) val publicationData: ByteArray = ByteArray(0),
        @ProtoNumber(2) val endorsedBlockHeader: ByteArray = ByteArray(0),
        @ProtoNumber(3) val lastBitcoinBlock: ByteArray = ByteArray(0),
        @ProtoNumber(4) val minerAddress: ByteArray = ByteArray(0),
        @ProtoNumber(5) val bitcoinContextAtEndorsed: List<ByteArray> = emptyList()
    )

    @Serializable
    data class Operation(
        @ProtoNumber(1) val id: String,
        @ProtoNumber(2) val state: String,
        @ProtoNumber(3) val action: String,
        @ProtoNumber(4) val endorsedBlockNumber: Int,
        @ProtoNumber(5) val miningInstruction: MiningInstruction,
        @ProtoNumber(6) val transaction: ByteArray,
        @ProtoNumber(7) val bitcoinTxId: String,
        @ProtoNumber(8) val blockOfProof: ByteArray,
        // kotlinx-serialization-protobuf has issues deserializing emptyLists so we give it a default value
        @ProtoNumber(9) val bitcoinContext: List<ByteArray> = emptyList(),
        @ProtoNumber(10) val merklePath: String,
        @ProtoNumber(11) val popTxId: String,
        @ProtoNumber(12) val payoutBlockHash: String,
        @ProtoNumber(13) val payoutAmount: Long,
        @ProtoNumber(14) val failureReason: String
    )
}
