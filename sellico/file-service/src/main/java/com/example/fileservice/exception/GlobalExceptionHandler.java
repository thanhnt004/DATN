package com.example.fileservice.exception;

import exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import response.BaseErrorCode;
import response.ErrorResponse;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Value("${spring.application.name:file-service}")
    private String serviceName;

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        BaseErrorCode err = ex.getErrorCode();
        log.error("[{}] {}: {}", serviceName, err.getCode(), ex.getMessage());
        return ResponseEntity.status(err.getStatusCode())
                .body(new ErrorResponse(err.getCode(), ex.getMessage(), serviceName, null));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.error("[{}] File too large: {}", serviceName, ex.getMessage());
        return ResponseEntity.status(400)
                .body(new ErrorResponse("FILE_TOO_LARGE", "File size exceeds maximum allowed size", serviceName, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("[{}] Unexpected error: ", serviceName, ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred", serviceName, null));
    }
}

