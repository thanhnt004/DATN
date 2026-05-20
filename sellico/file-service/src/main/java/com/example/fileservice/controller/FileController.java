package com.example.fileservice.controller;

import com.example.fileservice.dto.FileUploadResponse;
import com.example.fileservice.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import response.ApiResponse;

import java.util.List;

/**
 * File Upload Controller
 *
 * Public (authenticated):
 *   POST   /api/v1/files/upload             — Upload single image
 *   POST   /api/v1/files/upload/batch       — Upload multiple images
 *   DELETE /api/v1/files                     — Delete single file (publicId as query param)
 *   DELETE /api/v1/files/batch               — Delete multiple files (publicIds in body)
 *
 * Internal (no auth):
 *   POST   /internal/v1/files/upload         — Upload single (service-to-service)
 *   POST   /internal/v1/files/upload/batch   — Upload multiple (service-to-service)
 *   DELETE /internal/v1/files                — Delete single (publicId as query param)
 *   DELETE /internal/v1/files/batch          — Delete multiple (publicIds in body)
 */
@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    // =====================================================
    // Authenticated Endpoints (/api/v1/files)
    // =====================================================

    /**
     * POST /api/v1/files/upload - Upload single image
     */
    @PostMapping(value = "/api/v1/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder
    ) {
        FileUploadResponse response = fileService.uploadImage(file, folder);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/files/upload/batch - Upload multiple images
     */
    @PostMapping(value = "/api/v1/files/upload/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<FileUploadResponse>>> uploadImages(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "folder", defaultValue = "general") String folder
    ) {
        List<FileUploadResponse> response = fileService.uploadImages(files, folder);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * DELETE /api/v1/files?publicId=xxx - Delete single file
     */
    @DeleteMapping("/api/v1/files")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @RequestParam("publicId") String publicId
    ) {
        fileService.deleteFile(publicId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * DELETE /api/v1/files/batch - Delete multiple files
     */
    @DeleteMapping("/api/v1/files/batch")
    public ResponseEntity<ApiResponse<Void>> deleteFiles(
            @RequestBody List<String> publicIds
    ) {
        fileService.deleteFiles(publicIds);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // =====================================================
    // Internal Endpoints (/internal/v1/files)
    // =====================================================

    @PostMapping(value = "/internal/v1/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> internalUploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder
    ) {
        FileUploadResponse response = fileService.uploadImage(file, folder);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping(value = "/internal/v1/files/upload/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<FileUploadResponse>>> internalUploadImages(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "folder", defaultValue = "general") String folder
    ) {
        List<FileUploadResponse> response = fileService.uploadImages(files, folder);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/internal/v1/files")
    public ResponseEntity<ApiResponse<Void>> internalDeleteFile(
            @RequestParam("publicId") String publicId
    ) {
        fileService.deleteFile(publicId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/internal/v1/files/batch")
    public ResponseEntity<ApiResponse<Void>> internalDeleteFiles(
            @RequestBody List<String> publicIds
    ) {
        fileService.deleteFiles(publicIds);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

