package org.example.identityservice.exception;

import exception.BaseException;
import response.BaseErrorCode;

public class KeycloakException extends BaseException {
    public KeycloakException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
