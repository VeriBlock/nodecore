package veriblock.model;

public enum DownloadStatus {
    DISCOVERING,
    DOWNLOADING,
    READY;

    public boolean isDiscovering(){
        return DownloadStatus.DISCOVERING == this;
    }
    public boolean isDownloading(){
        return DownloadStatus.DOWNLOADING == this;
    }
}
