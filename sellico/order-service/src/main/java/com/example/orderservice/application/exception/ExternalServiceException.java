package com.example.orderservice.application.exception;

import exception.BaseException;
import response.BaseErrorCode;

public class ExternalServiceException extends BaseException {
    public ExternalServiceException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}

