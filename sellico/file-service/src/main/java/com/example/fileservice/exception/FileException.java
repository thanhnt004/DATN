package com.example.fileservice.exception;

import exception.BaseException;
import response.BaseErrorCode;

public class FileException extends BaseException {

    private final String detail;

    public FileException(BaseErrorCode errorCode) {
        super(errorCode);
        this.detail = null;
    }

    public FileException(BaseErrorCode errorCode, String detail) {
        super(errorCode);
        this.detail = detail;
    }

    @Override
    public String getMessage() {
        return detail != null ? detail : super.getMessage();
    }
}

