package com.example.dfs.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Chunk {
    @Id
    @Column(length = 64) // SHA-256 hash length is 64 hex characters
    private String hash;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private long refCount = 0;

    @Column(nullable = false)
    private long size;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
    }
}
