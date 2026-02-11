package com.example.dfs.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "version_chunks")
public class VersionChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private FileVersion version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chunk_hash", nullable = false)
    private Chunk chunk;

    @Column(name = "seq_no", nullable = false)
    private int sequenceNo;
}
