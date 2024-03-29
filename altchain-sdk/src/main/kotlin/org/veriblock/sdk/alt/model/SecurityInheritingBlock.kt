package org.veriblock.sdk.alt.model

data class SecurityInheritingBlock(
    val hash: String,
    val height: Int,
    val previousHash: String,
    val merkleRoot: String,
    val coinbaseTransactionId: String,
    val transactionIds: List<String>,
    val endorsedBy: List<String>,
    val knownVbkHashes: List<String>,
    val veriBlockPublicationIds: List<String>,
    val bitcoinPublicationIds: List<String>,

    // Previous keystone references
    val previousKeystone: String? = null,
    val secondPreviousKeystone: String? = null
)
