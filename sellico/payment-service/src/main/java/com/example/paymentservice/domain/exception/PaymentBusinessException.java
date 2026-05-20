package com.example.paymentservice.domain.exception;

import exception.BaseException;
import response.BaseErrorCode;

public class PaymentBusinessException extends BaseException {
    public PaymentBusinessException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}

