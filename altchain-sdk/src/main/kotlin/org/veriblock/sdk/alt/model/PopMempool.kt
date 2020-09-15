package org.veriblock.sdk.alt.model

data class PopMempool(
    val vbkBlockHashes: List<String>,
    val atvs: List<String>,
    val vtbs: List<String>
)
