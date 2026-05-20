package com.example.shippingservice.exception;

import exception.BaseException;
import response.BaseErrorCode;

public class ShippingServiceException extends BaseException {

    public ShippingServiceException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
