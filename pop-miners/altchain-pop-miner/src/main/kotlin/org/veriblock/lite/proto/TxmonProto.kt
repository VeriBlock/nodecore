package org.veriblock.lite.proto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoId

object TxmonProto {
    @Serializable
    data class TransactionMeta(
        @ProtoId(1) val txId: ByteArray,
        @ProtoId(2) val state: Int,
        @ProtoId(3) val appearsInBestChainBlock: ByteArray,
        @ProtoId(4) val appearsInBlocks: List<ByteArray> = emptyList(),
        @ProtoId(5) val appearsAtHeight: Int,
        @ProtoId(6) val depth: Int
    )

    @Serializable
    data class TransactionInput(
        @ProtoId(1) val address: String,
        @ProtoId(2) val amount: Long
    )

    @Serializable
    data class TransactionOutput(
        @ProtoId(1) val address: String,
        @ProtoId(2) val amount: Long
    )

    @Serializable
    data class MerkleBranch(
        @ProtoId(1) val subject: ByteArray = ByteArray(0),
        @ProtoId(2) val index: Int = 0,
        @ProtoId(3) val merklePathHashes: List<ByteArray> = emptyList(),
        @ProtoId(4) val merkleSubTree: Int = 0
    )

    @Serializable
    data class WalletTransaction(
        @ProtoId(1) val txId: ByteArray,
        @ProtoId(2) val input: TransactionInput,
        @ProtoId(3) val outputs: List<TransactionOutput> = emptyList(),
        @ProtoId(4) val signatureIndex: Long,
        @ProtoId(5) val data: ByteArray,
        @ProtoId(6) val merkleBranch: MerkleBranch,
        @ProtoId(7) val meta: TransactionMeta,
        @ProtoId(8) val signature: ByteArray,
        @ProtoId(9) val publicKey: ByteArray
    )

    @Serializable
    data class TransactionMonitor(
        @ProtoId(1) val network: String,
        @ProtoId(2) val address: String,
        @ProtoId(3) val transactions: List<WalletTransaction> = emptyList()
    )
}
