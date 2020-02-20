package veriblock.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DownloadStatusResponse {
    private DownloadStatus downloadStatus;
    private Integer currentHeight;
    private Integer bestHeight;
}
