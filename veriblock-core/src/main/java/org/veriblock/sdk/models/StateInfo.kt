package org.veriblock.sdk.models

data class StateInfo(
    val networkHeight: Int = 0,
    val localBlockchainHeight: Int = 0,
    val blockDifference: Int = 0,
    val isSynchronized: Boolean = false,
    val initialblockdownload: Boolean = false,
    val networkVersion: String = ""
)

inline fun StateInfo.getSynchronizedMessage(): String {
    return "$blockDifference blocks left (LocalHeight=$localBlockchainHeight NetworkHeight=$networkHeight InitialBlockDownload=$initialblockdownload)"
}