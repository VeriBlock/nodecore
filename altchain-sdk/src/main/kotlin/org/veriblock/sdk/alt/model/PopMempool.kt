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
    val btcBlockOfProof: VtbBitcoinBlock,
    val btcBlockOfProofContext: List<VtbBitcoinBlock>
)

data class VtbBitcoinBlock(
    val hash: String,
    val version: Int,
    val previousBlock: String,
    val merkleRoot: String,
    val timestamp: Int,
    val bits: Int,
    val nonce: Int
)
