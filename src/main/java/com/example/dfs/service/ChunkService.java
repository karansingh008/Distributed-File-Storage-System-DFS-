package com.example.dfs.service;

import com.example.dfs.entity.Chunk;
import com.example.dfs.repository.ChunkRepository;
import com.example.dfs.util.HashUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
public class ChunkService {

    @Autowired
    private ChunkRepository chunkRepository;

    @Autowired
    private DeduplicationService deduplicationService;

    @Value("${storage.location}")
    private String storageLocation;

    @Transactional
    public Chunk storeChunk(byte[] data) throws IOException {
        String hash = HashUtils.calculateSHA256(data);

        boolean possibleHit = deduplicationService.mightContain(hash);

        if (possibleHit) {
            Optional<Chunk> existingChunk = chunkRepository.findById(hash);
            if (existingChunk.isPresent()) {
                Chunk chunk = existingChunk.get();
                chunk.setRefCount(chunk.getRefCount() + 1);
                return chunkRepository.save(chunk);
            }
        }

        Path storagePath = Paths.get(storageLocation);
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
        }

        Path chunkPath = storagePath.resolve(hash);
        if (!Files.exists(chunkPath)) {
            try (FileOutputStream fos = new FileOutputStream(chunkPath.toFile())) {
                fos.write(data);
            }
        }

        Chunk newChunk = new Chunk();
        newChunk.setHash(hash);
        newChunk.setPath(chunkPath.toAbsolutePath().toString());
        newChunk.setRefCount(1);
        newChunk.setSize(data.length);
        chunkRepository.save(newChunk);

        deduplicationService.add(hash);

        return newChunk;
    }

    public byte[] getChunkData(Chunk chunk) throws IOException {
        Path path = Paths.get(chunk.getPath());
        return Files.readAllBytes(path);
    }
}
