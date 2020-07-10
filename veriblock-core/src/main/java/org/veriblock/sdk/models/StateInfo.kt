package org.veriblock.sdk.models

data class StateInfo(
    val networkHeight: Int = 0,
    val localBlockchainHeight: Int = 0,
    val blockDifference: Int = 0,
    val isSynchronized: Boolean = false,
    val initialBlockDownload: Boolean = false,
    val networkVersion: String = ""
)

fun StateInfo.getSynchronizedMessage(): String = if (networkHeight == 0) {
    "connecting to the network..."
} else {
    "$blockDifference blocks left (LocalHeight=$localBlockchainHeight NetworkHeight=$networkHeight InitialBlockDownload=$initialBlockDownload)"
}
