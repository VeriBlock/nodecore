package org.veriblock.sdk.alt.model

data class SecurityInheritingTransaction(
    val txId: String,
    val confirmations: Int,
    val vout: List<SecurityInheritingTransactionVout>
)

data class SecurityInheritingTransactionVout(
    val value: Long,
    val addressHex: String
)
