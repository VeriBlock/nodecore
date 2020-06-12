package org.veriblock.sdk.models

data class SyncStatus(
    val networkHeight: Int = 0,
    val localBlockchainHeight: Int = 0,
    val blockDifference: Int = 0,
    val isSynchronized: Boolean = false,
    val initialblockdownload: Boolean = false
)
