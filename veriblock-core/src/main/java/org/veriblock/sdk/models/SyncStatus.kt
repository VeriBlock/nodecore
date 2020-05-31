package org.veriblock.sdk.models

data class SyncStatus(
    val networkHeight: Int,
    val localBlockchainHeight: Int,
    val blockDifference: Int,
    val isSynchronized: Boolean
)
