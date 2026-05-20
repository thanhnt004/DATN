package com.example.fileservice.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.fileservice.dto.FileUploadResponse;
import com.example.fileservice.exception.FileErrorCode;
import com.example.fileservice.exception.FileException;
import com.example.fileservice.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryFileService implements FileService {

    private final Cloudinary cloudinary;

    @Value("${app.upload.max-file-size:5242880}")  // 5MB default
    private long maxFileSize;

    @Value("${app.upload.max-files:10}")
    private int maxFiles;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "svg"
    );

    // =====================================================
    // Upload Single Image
    // =====================================================

    @Override
    public FileUploadResponse uploadImage(MultipartFile file, String folder) {
        validateFile(file);

        try {
            Map<String, Object> options = buildUploadOptions(file, folder);
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), options);
            return mapToResponse(result, file.getOriginalFilename());

        } catch (IOException e) {
            log.error("Failed to upload file '{}' to Cloudinary: {}", file.getOriginalFilename(), e.getMessage());
            throw new FileException(FileErrorCode.UPLOAD_FAILED,
                    "Failed to upload file: " + file.getOriginalFilename());
        }
    }

    // =====================================================
    // Upload Multiple Images
    // =====================================================

    @Override
    public List<FileUploadResponse> uploadImages(List<MultipartFile> files, String folder) {
        if (files == null || files.isEmpty()) {
            throw new FileException(FileErrorCode.FILE_EMPTY, "No files provided");
        }

        if (files.size() > maxFiles) {
            throw new FileException(FileErrorCode.TOO_MANY_FILES,
                    "Maximum " + maxFiles + " files allowed per request, got " + files.size());
        }

        List<FileUploadResponse> responses = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                FileUploadResponse response = uploadImage(file, folder);
                responses.add(response);
            } catch (Exception e) {
                log.error("Failed to upload file '{}': {}", file.getOriginalFilename(), e.getMessage());
                failedFiles.add(file.getOriginalFilename());
            }
        }

        if (!failedFiles.isEmpty() && responses.isEmpty()) {
            throw new FileException(FileErrorCode.UPLOAD_FAILED,
                    "All uploads failed. Failed files: " + String.join(", ", failedFiles));
        }

        if (!failedFiles.isEmpty()) {
            log.warn("Partially uploaded. Failed files: {}", failedFiles);
        }

        return responses;
    }

    // =====================================================
    // Delete File(s)
    // =====================================================

    @Override
    public void deleteFile(String publicId) {
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            String deleteResult = (String) result.get("result");

            if (!"ok".equals(deleteResult)) {
                log.warn("Cloudinary delete returned '{}' for publicId={}", deleteResult, publicId);
                throw new FileException(FileErrorCode.FILE_NOT_FOUND,
                        "File not found in Cloudinary: " + publicId);
            }

            log.info("Deleted file from Cloudinary: publicId={}", publicId);
        } catch (FileException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to delete file from Cloudinary: publicId={}", publicId, e);
            throw new FileException(FileErrorCode.DELETE_FAILED,
                    "Failed to delete file: " + publicId);
        }
    }

    @Override
    public void deleteFiles(List<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return;
        }

        List<String> failedIds = new ArrayList<>();
        for (String publicId : publicIds) {
            try {
                deleteFile(publicId);
            } catch (Exception e) {
                log.error("Failed to delete publicId={}: {}", publicId, e.getMessage());
                failedIds.add(publicId);
            }
        }

        if (!failedIds.isEmpty()) {
            log.warn("Failed to delete {} out of {} files: {}",
                    failedIds.size(), publicIds.size(), failedIds);
        }
    }

    // =====================================================
    // Validation
    // =====================================================

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileException(FileErrorCode.FILE_EMPTY);
        }

        if (file.getSize() > maxFileSize) {
            throw new FileException(FileErrorCode.FILE_TOO_LARGE,
                    "File size " + humanReadableSize(file.getSize()) +
                            " exceeds maximum " + humanReadableSize(maxFileSize));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new FileException(FileErrorCode.INVALID_FILE_TYPE,
                    "File type '" + contentType + "' is not allowed. Allowed types: " + ALLOWED_IMAGE_TYPES);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = getFileExtension(originalFilename).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new FileException(FileErrorCode.INVALID_FILE_TYPE,
                        "File extension '." + extension + "' is not allowed");
            }
        }
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    private Map<String, Object> buildUploadOptions(MultipartFile file, String folder) {
        String publicId = generatePublicId(file.getOriginalFilename());

        return ObjectUtils.asMap(
                "folder", folder != null ? folder : "general",
                "public_id", publicId,
                "resource_type", "image",
                "overwrite", false,
                "quality", "auto:good",
                "fetch_format", "auto"
        );
    }

    private String generatePublicId(String originalFilename) {
        String nameWithoutExt = originalFilename != null
                ? originalFilename.replaceAll("\\.[^.]+$", "")
                : "file";

        // Clean non-alphanumeric chars and add timestamp for uniqueness
        String cleanName = nameWithoutExt.replaceAll("[^a-zA-Z0-9_-]", "_");
        return cleanName + "_" + System.currentTimeMillis();
    }

    private FileUploadResponse mapToResponse(Map<?, ?> result, String originalFilename) {
        return FileUploadResponse.builder()
                .publicId((String) result.get("public_id"))
                .url((String) result.get("url"))
                .secureUrl((String) result.get("secure_url"))
                .format((String) result.get("format"))
                .resourceType((String) result.get("resource_type"))
                .bytes(toLong(result.get("bytes")))
                .width(toInteger(result.get("width")))
                .height(toInteger(result.get("height")))
                .folder((String) result.get("folder"))
                .originalFilename(originalFilename)
                .build();
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot >= 0 ? filename.substring(lastDot + 1) : "";
    }

    private String humanReadableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private Long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        return null;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        return null;
    }
}

