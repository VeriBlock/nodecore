package org.veriblock.sdk.alt.model

data class SecurityInheritingAtv(
    val id: String,
    val confirmations: Int?,
    val blockHash: String?
)

data class SecurityInheritingTransaction(
    val txId: String,
    val confirmations: Int,
    val vout: List<SecurityInheritingTransactionVout>,
    val blockHash: String?
)

data class SecurityInheritingTransactionVout(
    val value: Long,
    val addressHex: String
)
