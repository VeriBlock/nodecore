package org.veriblock.sdk.alt.model

data class SecurityInheritingBlock(
    val hash: String,
    val height: Int,
    val confirmations: Int,
    val version: Int,
    val nonce: Int,
    val merkleRoot: String,
    val difficulty: Double,
    val coinbaseTransactionId: String,
    val transactionIds: List<String>
)
