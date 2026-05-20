package com.example.categoryservice.exception;

import exception.BaseException;
import response.BaseErrorCode;

public class CategoryBusinessException extends BaseException {
    public CategoryBusinessException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
