package com.example.dfs.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "file_versions")
public class FileVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileMetadata file;

    @Column(nullable = false)
    private int versionNo;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadTime;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "committed_flag", nullable = false)
    private boolean committed = false;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
