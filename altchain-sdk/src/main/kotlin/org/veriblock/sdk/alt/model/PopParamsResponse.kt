package org.veriblock.sdk.alt.model

data class PopPayoutParams(
    val popPayoutDelay: Int
)

data class NetworkParam(
    val network: String
)

data class PopParamsResponse(
    val popActivationHeight: Int,
    val networkId: Long,
    val payoutParams: PopPayoutParams,
    val vbkBootstrapBlock: NetworkParam,
)
