package com.example.inventoryservice.exception;

import exception.BaseException;
import response.BaseErrorCode;

public class InventoryException extends BaseException {

    private final String customMessage;

    public InventoryException(BaseErrorCode errorCode) {
        super(errorCode);
        this.customMessage = null;
    }

    public InventoryException(BaseErrorCode errorCode, String customMessage) {
        super(errorCode);
        this.customMessage = customMessage;
    }

    @Override
    public String getMessage() {
        return customMessage != null ? customMessage : super.getMessage();
    }
}

