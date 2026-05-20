package org.example.userservice.exception;

import exception.BaseException;
import response.BaseErrorCode;

public class AuthenException extends BaseException {
    public AuthenException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
