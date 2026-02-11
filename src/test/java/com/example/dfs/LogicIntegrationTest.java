package com.example.dfs;

import com.example.dfs.entity.*;
import com.example.dfs.repository.*;
import com.example.dfs.service.*;
import com.example.dfs.util.HashUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class LogicIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private FileService fileService;
    @Autowired
    private ChunkService chunkService;
    @Autowired
    private ChunkRepository chunkRepository;
    @Autowired
    private FileMetadataRepository fileMetadataRepository;
    @Autowired
    private FileVersionRepository fileVersionRepository;

    @Test
    public void testFullFlow() throws IOException {
        // 1. User Registration
        String email = "test@example.com";
        User user = userService.registerUser(email, "password");
        Assertions.assertNotNull(user.getId());

        // 2. Upload File 1 (Version 1)
        String content1 = "Hello World Data";
        MockMultipartFile file1 = new MockMultipartFile("file", "test.txt", "text/plain", content1.getBytes());
        fileService.uploadFile(user, file1);

        // Verify File Metadata
        Optional<FileMetadata> fileMetaOpt = fileMetadataRepository.findByUserAndName(user, "test.txt");
        Assertions.assertTrue(fileMetaOpt.isPresent());
        FileMetadata fileMeta = fileMetaOpt.get();

        // Verify Version 1
        List<FileVersion> versions = fileService.listVersions(fileMeta);
        Assertions.assertEquals(1, versions.size());
        Assertions.assertEquals(1, versions.get(0).getVersionNo());

        // Verify Chunk
        String hash1 = HashUtils.calculateSHA256(content1.getBytes());
        Optional<Chunk> chunk1 = chunkRepository.findById(hash1);
        Assertions.assertTrue(chunk1.isPresent());
        Assertions.assertEquals(1, chunk1.get().getRefCount());

        // 3. Upload Same File Content again (Deduplication Check) - different name
        // Requirement: "Different filename, same content -> Separate file, shared
        // chunks"
        MockMultipartFile file2 = new MockMultipartFile("file", "copy.txt", "text/plain", content1.getBytes());
        fileService.uploadFile(user, file2);

        // Verify File 2
        Optional<FileMetadata> fileMeta2Opt = fileMetadataRepository.findByUserAndName(user, "copy.txt");
        Assertions.assertTrue(fileMeta2Opt.isPresent());
        FileMetadata fileMeta2 = fileMeta2Opt.get();

        // Verify Chunk Ref Count increased
        Chunk chunkRef = chunkRepository.findById(hash1).get();
        Assertions.assertEquals(2, chunkRef.getRefCount(), "Chunk ref count should be 2");

        // 4. Upload Same Filename, Different Content (Versioning Check)
        // Requirement: "Same filename -> New version"
        String content2 = "New Content Version";
        MockMultipartFile file1v2 = new MockMultipartFile("file", "test.txt", "text/plain", content2.getBytes());
        fileService.uploadFile(user, file1v2);

        // Verify Versions
        versions = fileService.listVersions(fileMeta);
        Assertions.assertEquals(2, versions.size());

        // Ensure sorted order (descending): latest should be v2 at index 0, v1 at index
        // 1
        Assertions.assertEquals(2, versions.get(0).getVersionNo());
        Assertions.assertEquals(1, versions.get(1).getVersionNo());

        // Verify New Chunk Created
        String hash2 = HashUtils.calculateSHA256(content2.getBytes());
        Optional<Chunk> chunk2 = chunkRepository.findById(hash2);
        Assertions.assertTrue(chunk2.isPresent());
        Assertions.assertEquals(1, chunk2.get().getRefCount());

        // 5. Download and Verify Content
        // Download v1 of test.txt (index 1)
        FileVersion v1 = versions.get(1);
        List<VersionChunk> v1Chunks = fileService.getVersionChunks(v1);
        byte[] downloadedBytes = chunkService.getChunkData(v1Chunks.get(0).getChunk());
        Assertions.assertArrayEquals(content1.getBytes(), downloadedBytes);

        // Download v2 of test.txt (index 0)
        FileVersion v2 = versions.get(0);
        List<VersionChunk> v2Chunks = fileService.getVersionChunks(v2);
        byte[] downloadedBytes2 = chunkService.getChunkData(v2Chunks.get(0).getChunk());
        Assertions.assertArrayEquals(content2.getBytes(), downloadedBytes2);
    }
}
