package com.example.discountservice.domain.exception;

import exception.BaseException;

public class DiscountBusinessException extends BaseException {
    public DiscountBusinessException(DiscountErrorCode errorCode) {
        super(errorCode);
    }
}

