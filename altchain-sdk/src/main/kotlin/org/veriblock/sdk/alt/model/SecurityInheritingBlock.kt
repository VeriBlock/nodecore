package org.veriblock.sdk.alt.model

data class SecurityInheritingBlock(
    val hash: String,
    val height: Int,
    val previousHash: String,
    val confirmations: Int,
    val version: Short,
    val nonce: Int,
    val merkleRoot: String,
    val difficulty: Double,
    val coinbaseTransactionId: String,
    val transactionIds: List<String>,

    // Previous keystone references
    val previousKeystone: String? = null,
    val secondPreviousKeystone: String? = null
)
