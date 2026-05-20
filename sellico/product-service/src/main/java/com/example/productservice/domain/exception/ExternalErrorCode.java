package com.example.productservice.domain.exception;

import response.BaseErrorCode;

public class ExternalErrorCode implements BaseErrorCode {
    private final String code;
    private final String message;
    private final int statusCode;

    public ExternalErrorCode(String code, String message, int statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
    @Override public int getStatusCode() { return statusCode; }
}

