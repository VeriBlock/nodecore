package veriblock.model;

public class DownloadStatusResponse {
    private DownloadStatus downloadStatus;
    private Integer currentHeight;
    private Integer bestHeight;

    public DownloadStatusResponse(DownloadStatus downloadStatus, Integer currentHeight, Integer bestHeight) {
        this.downloadStatus = downloadStatus;
        this.currentHeight = currentHeight;
        this.bestHeight = bestHeight;
    }

    public DownloadStatus getDownloadStatus() {
        return downloadStatus;
    }

    public Integer getCurrentHeight() {
        return currentHeight;
    }

    public Integer getBestHeight() {
        return bestHeight;
    }
}
