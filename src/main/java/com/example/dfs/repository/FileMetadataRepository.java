package com.example.dfs.repository;

import com.example.dfs.entity.FileMetadata;
import com.example.dfs.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    List<FileMetadata> findByUser(User user);

    List<FileMetadata> findByUserAndIsDeletedFalse(User user);

    List<FileMetadata> findByUserAndIsDeletedTrue(User user);

    Optional<FileMetadata> findByUserAndName(User user, String name);

    long countByUser(User user);

    long countByUserAndIsDeletedFalse(User user);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("UPDATE FileMetadata f SET f.size = :size WHERE f.id = :id")
    void updateSize(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("size") Long size);
}
