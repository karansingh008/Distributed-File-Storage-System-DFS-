package com.example.dfs.repository;

import com.example.dfs.entity.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChunkRepository extends JpaRepository<Chunk, String> {
}
