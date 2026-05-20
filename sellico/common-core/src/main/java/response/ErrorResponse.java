package response;

import java.time.Instant;

public class ErrorResponse {

    private boolean success;
    private String errorCode;
    private String message;
    private String service;
    private Instant timestamp;
    private String traceId;

    public ErrorResponse() {
        this.success = false;
        this.timestamp = Instant.now();
    }

    public ErrorResponse(String errorCode, String message, String service, String traceId) {
        this.success = false;
        this.errorCode = errorCode;
        this.message = message;
        this.service = service;
        this.traceId = traceId;
        this.timestamp = Instant.now();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public String getService() {
        return service;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getTraceId() {
        return traceId;
    }
}
