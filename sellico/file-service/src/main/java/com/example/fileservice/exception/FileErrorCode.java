package com.example.fileservice.exception;

import response.BaseErrorCode;

public enum FileErrorCode implements BaseErrorCode {

    FILE_EMPTY("FILE_EMPTY", "File is empty", 400),
    FILE_TOO_LARGE("FILE_TOO_LARGE", "File size exceeds the maximum allowed size", 400),
    INVALID_FILE_TYPE("INVALID_FILE_TYPE", "File type is not allowed", 400),
    UPLOAD_FAILED("UPLOAD_FAILED", "Failed to upload file", 500),
    DELETE_FAILED("DELETE_FAILED", "Failed to delete file", 500),
    FILE_NOT_FOUND("FILE_NOT_FOUND", "File not found", 404),
    TOO_MANY_FILES("TOO_MANY_FILES", "Too many files in a single upload request", 400),
    CLOUDINARY_ERROR("CLOUDINARY_ERROR", "Cloudinary service error", 502),
    ;

    private final String code;
    private final String message;
    private final int statusCode;

    FileErrorCode(String code, String message, int statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }
}

