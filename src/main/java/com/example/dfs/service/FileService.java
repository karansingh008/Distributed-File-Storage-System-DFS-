package com.example.dfs.service;

import com.example.dfs.entity.*;
import com.example.dfs.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;

import java.util.List;

@Service
public class FileService {

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private FileVersionRepository fileVersionRepository;

    @Autowired
    private VersionChunkRepository versionChunkRepository;

    @Autowired
    private ChunkService chunkService;

    private static final int CHUNK_SIZE = 4 * 1024 * 1024;

    @Transactional
    public void uploadFile(User user, MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();

        FileMetadata fileMetadata = fileMetadataRepository.findByUserAndName(user, fileName)
                .orElseGet(() -> {
                    FileMetadata newFile = new FileMetadata();
                    newFile.setUser(user);
                    newFile.setName(fileName);
                    return fileMetadataRepository.save(newFile);
                });

        if (fileMetadata.isDeleted()) {
            fileMetadata.setDeleted(false);
            fileMetadata.setDeletedAt(null);
            fileMetadataRepository.save(fileMetadata);

            List<FileVersion> allVersions = fileVersionRepository.findByFile(fileMetadata);
            for (FileVersion v : allVersions) {
                if (v.isDeleted()) {
                    v.setDeleted(false);
                    v.setDeletedAt(null);
                    fileVersionRepository.save(v);
                }
            }
        }

        List<FileVersion> versions = fileVersionRepository.findByFileOrderByVersionNoDesc(fileMetadata);
        int nextVersion = versions.isEmpty() ? 1 : versions.get(0).getVersionNo() + 1;

        FileVersion version = new FileVersion();
        version.setFile(fileMetadata);
        version.setVersionNo(nextVersion);
        version.setUploadTime(LocalDateTime.now());
        version.setCommitted(false);
        version = fileVersionRepository.save(version);

        long totalSize = 0;
        try (InputStream is = file.getInputStream()) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            int sequenceNo = 0;
            while ((bytesRead = is.read(buffer)) != -1) {
                byte[] chunkData;
                if (bytesRead < CHUNK_SIZE) {
                    chunkData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, chunkData, 0, bytesRead);
                } else {
                    chunkData = buffer;
                }

                Chunk chunk = chunkService.storeChunk(chunkData);
                totalSize += chunkData.length;

                VersionChunk versionChunk = new VersionChunk();
                versionChunk.setVersion(version);
                versionChunk.setChunk(chunk);
                versionChunk.setSequenceNo(sequenceNo++);
                versionChunkRepository.save(versionChunk);
            }
        }

        version.setSizeBytes(totalSize);
        version.setCommitted(true);
        fileVersionRepository.save(version);

        fileMetadataRepository.updateSize(fileMetadata.getId(), totalSize);
    }

    public List<FileMetadata> listFiles(User user) {
        return fileMetadataRepository.findByUserAndIsDeletedFalse(user);
    }

    public List<FileVersion> listVersions(FileMetadata file) {
        return fileVersionRepository.findByFileOrderByVersionNoDesc(file);
    }

    public void trashFile(Long fileId, User user) {
        FileMetadata file = getFile(fileId);
        if (!file.getUser().getId().equals(user.getId()))
            throw new RuntimeException("Unauthorized");
        file.setDeleted(true);
        file.setDeletedAt(LocalDateTime.now());
        fileMetadataRepository.save(file);
    }

    @Transactional
    public void restoreFile(Long fileId, User user) {
        FileMetadata file = getFile(fileId);
        if (!file.getUser().getId().equals(user.getId()))
            throw new RuntimeException("Unauthorized");
        file.setDeleted(false);
        file.setDeletedAt(null);
        fileMetadataRepository.save(file);

        List<FileVersion> allVersions = fileVersionRepository.findByFile(file);
        for (FileVersion v : allVersions) {
            if (v.isDeleted()) {
                v.setDeleted(false);
                v.setDeletedAt(null);
                fileVersionRepository.save(v);
            }
        }

        List<FileVersion> activeVersions = fileVersionRepository.findByFileAndIsDeletedFalseOrderByVersionNoDesc(file);
        long newSize = activeVersions.isEmpty() ? 0L : activeVersions.get(0).getSizeBytes();
        fileMetadataRepository.updateSize(file.getId(), newSize);
    }

    @Transactional
    public void restoreVersion(Long versionId, User user) {
        FileVersion version = getVersion(versionId);
        FileMetadata fileMeta = version.getFile();
        if (!fileMeta.getUser().getId().equals(user.getId()))
            throw new RuntimeException("Unauthorized");

        if (version.isDeleted()) {
            version.setDeleted(false);
            version.setDeletedAt(null);
            fileVersionRepository.save(version);
        }

        List<FileVersion> activeVersions = fileVersionRepository
                .findByFileAndIsDeletedFalseOrderByVersionNoDesc(fileMeta);

        if (!activeVersions.isEmpty()) {
            long newSize = activeVersions.get(0).getSizeBytes();
            fileMetadataRepository.updateSize(fileMeta.getId(), newSize);
        }
    }

    @Transactional
    public void deleteFilePermanently(Long fileId, User user) {
        FileMetadata file = getFile(fileId);
        if (!file.getUser().getId().equals(user.getId()))
            throw new RuntimeException("Unauthorized");

        List<FileVersion> allVersions = fileVersionRepository.findByFile(file);
        for (FileVersion v : allVersions) {
            versionChunkRepository.deleteByVersion(v);
            fileVersionRepository.delete(v);
        }

        fileMetadataRepository.delete(file);
    }

    public List<FileMetadata> listTrash(User user) {
        return fileMetadataRepository.findByUserAndIsDeletedTrue(user);
    }

    public FileMetadata getFile(Long id) {
        return fileMetadataRepository.findById(id).orElseThrow(() -> new RuntimeException("File not found"));
    }

    public FileVersion getVersion(Long id) {
        return fileVersionRepository.findById(id).orElseThrow(() -> new RuntimeException("Version not found"));
    }

    public List<VersionChunk> getVersionChunks(FileVersion version) {
        return versionChunkRepository.findByVersionOrderBySequenceNoAsc(version);
    }

    @Transactional
    public void deleteVersion(Long versionId, User user) {
        FileVersion version = getVersion(versionId);
        FileMetadata fileMeta = version.getFile();
        if (!fileMeta.getUser().getId().equals(user.getId()))
            throw new RuntimeException("Unauthorized");

        version.setDeleted(true);
        version.setDeletedAt(LocalDateTime.now());
        fileVersionRepository.save(version);

        List<FileVersion> activeVersions = fileVersionRepository
                .findByFileAndIsDeletedFalseOrderByVersionNoDesc(fileMeta);
        if (activeVersions.isEmpty()) {
            fileMeta.setDeleted(true);
            fileMeta.setDeletedAt(LocalDateTime.now());
            fileMeta.setSize(0L);
            fileMetadataRepository.save(fileMeta);
        } else {
            fileMetadataRepository.updateSize(fileMeta.getId(), activeVersions.get(0).getSizeBytes());
        }
    }

    public com.example.dfs.dto.UserStatsDTO getUserStats(User user) {
        com.example.dfs.dto.UserStatsDTO stats = new com.example.dfs.dto.UserStatsDTO();
        stats.setTotalFiles(fileMetadataRepository.countByUserAndIsDeletedFalse(user));
        stats.setUsedStorageBytes(fileVersionRepository.sumSizeBytesByUser(user));
        return stats;
    }
}
