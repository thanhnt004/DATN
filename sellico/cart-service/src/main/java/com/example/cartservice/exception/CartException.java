package com.example.cartservice.exception;

import exception.BaseException;
import response.BaseErrorCode;

public class CartException extends BaseException {

    private final String customMessage;

    public CartException(BaseErrorCode errorCode) {
        super(errorCode);
        this.customMessage = null;
    }

    public CartException(BaseErrorCode errorCode, String customMessage) {
        super(errorCode);
        this.customMessage = customMessage;
    }

    @Override
    public String getMessage() {
        return customMessage != null ? customMessage : super.getMessage();
    }
}

