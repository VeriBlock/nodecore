package org.veriblock.sdk.alt.model

data class PopMempool(
    val vbkBlockHashes: List<String>,
    val atvs: List<String>,
    val vtbs: List<String>
)

data class Atv(
    val vbkTransactionId: String,
    val vbkBlockOfProofHash: String,
    val containingBlock: String,
    val confirmations: Int
)

// TODO
data class Vtb(
    //val btcTransactionId: String,
    val btcBlockOfProofHash: String
)
