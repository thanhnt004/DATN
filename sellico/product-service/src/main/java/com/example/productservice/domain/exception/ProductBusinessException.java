package com.example.productservice.domain.exception;

import exception.BaseException;
import response.BaseErrorCode;

public class ProductBusinessException extends BaseException {
    public ProductBusinessException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
