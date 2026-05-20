package com.example.fileservice.service;

import com.example.fileservice.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    /**
     * Upload a single image file.
     *
     * @param file   the multipart file to upload
     * @param folder the Cloudinary folder (e.g., "products", "avatars", "banners")
     * @return upload result with URL and metadata
     */
    FileUploadResponse uploadImage(MultipartFile file, String folder);

    /**
     * Upload multiple image files.
     *
     * @param files  list of multipart files
     * @param folder the Cloudinary folder
     * @return list of upload results
     */
    List<FileUploadResponse> uploadImages(List<MultipartFile> files, String folder);

    /**
     * Delete a file by its Cloudinary public ID.
     *
     * @param publicId the Cloudinary public ID
     */
    void deleteFile(String publicId);

    /**
     * Delete multiple files by their Cloudinary public IDs.
     *
     * @param publicIds list of Cloudinary public IDs
     */
    void deleteFiles(List<String> publicIds);
}

