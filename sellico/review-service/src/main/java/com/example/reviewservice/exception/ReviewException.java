package com.example.reviewservice.exception;

import exception.BaseException;
import response.BaseErrorCode;

public class ReviewException extends BaseException {
    public ReviewException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
