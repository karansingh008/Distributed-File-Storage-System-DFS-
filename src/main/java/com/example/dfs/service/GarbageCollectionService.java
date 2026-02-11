package com.example.dfs.service;

import com.example.dfs.entity.Chunk;
import com.example.dfs.repository.ChunkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class GarbageCollectionService {

    @Autowired
    private ChunkRepository chunkRepository;

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupOrphanedChunks() {
        System.out.println("Running Garbage Collection...");

        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);

        List<Chunk> allChunks = chunkRepository.findAll();

        for (Chunk chunk : allChunks) {
            if (chunk.getRefCount() == 0 && chunk.getCreatedAt().isBefore(cutoff)) {
                try {
                    Files.deleteIfExists(Paths.get(chunk.getPath()));
                    chunkRepository.delete(chunk);
                    System.out.println("Deleted chunk: " + chunk.getHash());
                } catch (IOException e) {
                    System.err.println("Failed to delete chunk file: " + chunk.getPath());
                }
            }
        }
    }
}
