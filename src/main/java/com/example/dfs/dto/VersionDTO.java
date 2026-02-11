package com.example.dfs.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VersionDTO {
    private Long id;
    private int versionNo;
    private LocalDateTime uploadTime;
    private Long fileId;
    private String fileName;
    private Long sizeBytes;
    private boolean isDeleted;
}
