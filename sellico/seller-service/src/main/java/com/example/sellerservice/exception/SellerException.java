package com.example.sellerservice.exception;

import exception.BaseException;
import response.BaseErrorCode;

public class SellerException extends BaseException {

    private final String customMessage;

    public SellerException(BaseErrorCode errorCode) {
        super(errorCode);
        this.customMessage = null;
    }

    public SellerException(BaseErrorCode errorCode, String customMessage) {
        super(errorCode);
        this.customMessage = customMessage;
    }

    @Override
    public String getMessage() {
        return customMessage != null ? customMessage : super.getMessage();
    }
}

