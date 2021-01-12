package org.veriblock.sdk.alt.model

enum class Validity(value: String) {
    VALID("valid"),
    INVALID("invalid")
}

data class SubmitPopResponse(
    // if true, then submission is successful and payload is added to mempool
    val accepted: Boolean,
    val state: Validity,
    val code: String,
    val message: String
)
