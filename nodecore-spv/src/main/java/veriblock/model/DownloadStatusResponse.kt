package veriblock.model

class DownloadStatusResponse(
    val downloadStatus: DownloadStatus,
    val currentHeight: Int,
    val bestHeight: Int
)
