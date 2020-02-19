package org.veriblock.sdk.alt.model

class SecurityInheritingTransaction(
    val txId: String,
    val blockHash: String,
    val confirmations: Int,
    val vout: List<SecurityInheritingTransactionVout>
)

class SecurityInheritingTransactionVout(
    val value: Double,
    val addressHash: String
)
