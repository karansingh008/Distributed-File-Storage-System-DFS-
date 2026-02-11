package com.example.dfs.controller;

import com.example.dfs.entity.*;
import com.example.dfs.dto.VersionDTO;
import com.example.dfs.service.ChunkService;
import com.example.dfs.service.FileService;
import com.example.dfs.service.UserService;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private UserService userService;

    @Autowired
    private ChunkService chunkService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file, Principal principal) {
        try {
            User user = userService.findByEmail(principal.getName()).orElseThrow();
            fileService.uploadFile(user, file);
            return ResponseEntity.ok("File uploaded successfully");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<FileMetadata>> listFiles(Principal principal) {
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        return ResponseEntity.ok(fileService.listFiles(user));
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<VersionDTO>> listVersions(@PathVariable Long id) {
        FileMetadata file = fileService.getFile(id);
        List<FileVersion> versions = fileService.listVersions(file);

        List<VersionDTO> dtos = versions.stream().map(v -> {
            VersionDTO dto = new VersionDTO();
            dto.setId(v.getId());
            dto.setVersionNo(v.getVersionNo());
            dto.setUploadTime(v.getUploadTime());
            dto.setFileId(file.getId());
            dto.setFileName(file.getName());
            dto.setSizeBytes(v.getSizeBytes());
            dto.setDeleted(v.isDeleted());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/download/{versionId}")
    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable Long versionId) {
        FileVersion version = fileService.getVersion(versionId);
        List<VersionChunk> chunks = fileService.getVersionChunks(version);
        String filename = version.getFile().getName();

        StreamingResponseBody stream = out -> {
            for (VersionChunk vc : chunks) {
                out.write(chunkService.getChunkData(vc.getChunk()));
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(stream);
    }

    @GetMapping("/stats")
    public ResponseEntity<com.example.dfs.dto.UserStatsDTO> getStats(Principal principal) {
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        return ResponseEntity.ok(fileService.getUserStats(user));
    }

    @GetMapping("/trash")
    public ResponseEntity<List<FileMetadata>> listTrash(Principal principal) {
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        return ResponseEntity.ok(fileService.listTrash(user));
    }

    @PostMapping("/{id}/trash")
    public ResponseEntity<?> trashFile(@PathVariable Long id, Principal principal) {
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        fileService.trashFile(id, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<?> restoreFile(@PathVariable Long id, Principal principal) {
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        fileService.restoreFile(id, user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable Long id, Principal principal) {
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        fileService.deleteFilePermanently(id, user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/versions/{id}")
    public ResponseEntity<?> deleteVersion(@PathVariable Long id, Principal principal) {
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        fileService.deleteVersion(id, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/versions/{id}/restore")
    public ResponseEntity<?> restoreVersion(@PathVariable Long id, Principal principal) {
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        fileService.restoreVersion(id, user);
        return ResponseEntity.ok().build();
    }
}
