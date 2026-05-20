package org.example.identityservice.exception;

import exception.BaseException;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import response.BaseErrorCode;
import response.CommonErrorCode;

import java.util.HashMap;
import java.util.Map;

@Component
public class KeycloakErrorNormalizer {
    private final Map<String, BaseErrorCode> errorCodeMap;
    public KeycloakErrorNormalizer(){
        errorCodeMap = new HashMap<>();
        errorCodeMap.put("User exists with same email", AuthErrorCode.EMAIL_EXISTS);
        errorCodeMap.put("User exists with same username", AuthErrorCode.USER_NAME_EXISTS);
        errorCodeMap.put("User name is missing", AuthErrorCode.USER_NAME_MISSING);
        errorCodeMap.put("Account is not fully set up", AuthErrorCode.EMAIL_NOT_VERIFIED);
    }
    public KeycloakException normalize(String errorMessage){
        BaseErrorCode code = errorCodeMap.getOrDefault(errorMessage, CommonErrorCode.UNCATEGORIZED_EXCEPTION);
        return new KeycloakException(code);
    }
}
