package org.veriblock.miners.pop.model.proto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoId

object OperationProto {
    @Serializable
    data class MiningInstruction(
        @ProtoId(1) val publicationData: ByteArray = ByteArray(0),
        @ProtoId(2) val endorsedBlockHeader: ByteArray = ByteArray(0),
        @ProtoId(3) val lastBitcoinBlock: ByteArray = ByteArray(0),
        @ProtoId(4) val minerAddress: ByteArray = ByteArray(0),
        @ProtoId(5) val bitcoinContextAtEndorsed: List<ByteArray> = emptyList()
    )

    @Serializable
    data class Operation(
        @ProtoId(1) val id: String,
        @ProtoId(2) val state: String,
        @ProtoId(3) val action: String,
        @ProtoId(4) val endorsedBlockNumber: Int,
        @ProtoId(5) val miningInstruction: MiningInstruction,
        @ProtoId(6) val transaction: ByteArray,
        @ProtoId(7) val bitcoinTxId: String,
        @ProtoId(8) val blockOfProof: ByteArray,
        // kotlinx-serialization-protobuf has issues deserializing emptyLists so we give it a default value
        @ProtoId(9) val bitcoinContext: List<ByteArray> = emptyList(),
        @ProtoId(10) val merklePath: String,
        @ProtoId(11) val popTxId: String,
        @ProtoId(12) val payoutBlockHash: String,
        @ProtoId(13) val payoutAmount: Long
    )
}
