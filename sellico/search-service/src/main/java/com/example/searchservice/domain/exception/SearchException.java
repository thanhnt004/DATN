package com.example.searchservice.domain.exception;

public class SearchException extends RuntimeException {
    private final String errorCode;

    public SearchException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
