package com.example.dfs.repository;

import com.example.dfs.entity.FileMetadata;
import com.example.dfs.entity.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {
    List<FileVersion> findByFileOrderByVersionNoDesc(FileMetadata file);

    // Active (non-deleted) versions only
    List<FileVersion> findByFileAndIsDeletedFalseOrderByVersionNoDesc(FileMetadata file);

    Optional<FileVersion> findByFileAndVersionNo(FileMetadata file, int versionNo);

    // All versions for a file (for permanent delete cleanup)
    List<FileVersion> findByFile(FileMetadata file);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(v.sizeBytes), 0) FROM FileVersion v WHERE v.file.user = :user AND v.file.isDeleted = false AND v.isDeleted = false")
    Long sumSizeBytesByUser(@org.springframework.data.repository.query.Param("user") com.example.dfs.entity.User user);
}
