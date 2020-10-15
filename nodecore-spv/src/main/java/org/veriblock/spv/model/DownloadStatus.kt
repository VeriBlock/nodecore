package org.veriblock.spv.model

enum class DownloadStatus {
    DISCOVERING,
    DOWNLOADING,
    READY;

    fun isDiscovering(): Boolean =
        DISCOVERING == this

    fun isDownloading(): Boolean =
        DOWNLOADING == this
}
