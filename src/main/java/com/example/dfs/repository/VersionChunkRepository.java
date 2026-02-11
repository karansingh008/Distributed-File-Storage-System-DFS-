package com.example.dfs.repository;

import com.example.dfs.entity.FileVersion;
import com.example.dfs.entity.VersionChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VersionChunkRepository extends JpaRepository<VersionChunk, Long> {
    List<VersionChunk> findByVersionOrderBySequenceNoAsc(FileVersion version);

    void deleteByVersion(FileVersion version);
}
