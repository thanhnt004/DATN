package org.example.userservice.exception;

import exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import response.BaseErrorCode;
import response.ErrorResponse;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @Value("${spring.application.name:unknown-service}")
    private String serviceName;
    // 1. Xử lý các lỗi nghiệp vụ (BaseException)
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        BaseErrorCode errorCode = ex.getErrorCode();
        log.error("[{}] Business Exception: code={}, message={}", serviceName, errorCode.getCode(), errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(buildErrorResponse(errorCode.getCode(), errorCode.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        String combinedErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return  ResponseEntity.status(400)
                .body(buildErrorResponse("VALIDATION_FAILED", combinedErrors));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("[{}] Unexpected error: ", serviceName, ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("INTERNAL_SERVER_ERROR", "Đã có lỗi xảy ra, vui lòng thử lại sau."));
    }
    // Helper method để tạo ErrorResponse
    private ErrorResponse buildErrorResponse(String code, String message) {
        // Lưu ý: traceId nên lấy từ Sleuth/Micrometer hoặc MDC nếu bạn có cấu hình Logging
        String currentTraceId = org.slf4j.MDC.get("traceId");

        return new ErrorResponse(
                code,
                message,
                serviceName,
                currentTraceId != null ? currentTraceId : "N/A"
        );
    }
}
