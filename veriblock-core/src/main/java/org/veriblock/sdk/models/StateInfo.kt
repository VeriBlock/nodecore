package org.veriblock.sdk.models

data class StateInfo(
    val networkTipHeight: Int? = null,
    val localBlockchainHash: String = "",
    val localBlockchainHeight: Int = 0,
    val blockDifference: Int = 0,
    val isSynchronized: Boolean = false,
    val initialBlockDownload: Boolean = false,
    val networkVersion: String = ""
)

fun StateInfo.getSynchronizedMessage(): String = if (networkTipHeight == null) {
    "connecting to the network..."
} else {
    "$blockDifference blocks left (LocalHeight=$localBlockchainHeight NetworkHeight=${networkTipHeight} InitialBlockDownload=$initialBlockDownload)"
}
