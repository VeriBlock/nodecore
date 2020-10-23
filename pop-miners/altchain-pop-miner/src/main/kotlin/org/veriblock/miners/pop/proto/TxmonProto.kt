package org.veriblock.miners.pop.proto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

object TxmonProto {
    @Serializable
    data class TransactionMeta(
        @ProtoNumber(1) val txId: ByteArray,
        @ProtoNumber(2) val state: Int,
        @ProtoNumber(3) val appearsInBestChainBlock: ByteArray,
        @ProtoNumber(4) val appearsInBlocks: List<ByteArray> = emptyList(),
        @ProtoNumber(5) val appearsAtHeight: Int,
        @ProtoNumber(6) val depth: Int
    )

    @Serializable
    data class TransactionInput(
        @ProtoNumber(1) val address: String,
        @ProtoNumber(2) val amount: Long
    )

    @Serializable
    data class TransactionOutput(
        @ProtoNumber(1) val address: String,
        @ProtoNumber(2) val amount: Long
    )

    @Serializable
    data class MerkleBranch(
        @ProtoNumber(1) val subject: ByteArray = ByteArray(0),
        @ProtoNumber(2) val index: Int = 0,
        @ProtoNumber(3) val merklePathHashes: List<ByteArray> = emptyList(),
        @ProtoNumber(4) val merkleSubTree: Int = 0
    )

    @Serializable
    data class WalletTransaction(
        @ProtoNumber(1) val txId: ByteArray,
        @ProtoNumber(2) val input: TransactionInput,
        @ProtoNumber(3) val outputs: List<TransactionOutput> = emptyList(),
        @ProtoNumber(4) val signatureIndex: Long,
        @ProtoNumber(5) val data: ByteArray,
        @ProtoNumber(6) val merkleBranch: MerkleBranch,
        @ProtoNumber(7) val meta: TransactionMeta,
        @ProtoNumber(8) val signature: ByteArray,
        @ProtoNumber(9) val publicKey: ByteArray
    )

    @Serializable
    data class TransactionMonitor(
        @ProtoNumber(1) val network: String,
        @ProtoNumber(2) val address: String,
        @ProtoNumber(3) val transactions: List<WalletTransaction> = emptyList()
    )
}
