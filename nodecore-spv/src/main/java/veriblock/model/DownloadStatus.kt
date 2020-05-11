package veriblock.model

enum class DownloadStatus {
    DISCOVERING,
    DOWNLOADING,
    READY;

    fun isDiscovering(): Boolean =
        DISCOVERING == this

    fun isDownloading(): Boolean =
        DOWNLOADING == this
}
