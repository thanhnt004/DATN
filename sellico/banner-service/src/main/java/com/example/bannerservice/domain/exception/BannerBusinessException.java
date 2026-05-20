package com.example.bannerservice.domain.exception;

import exception.BaseException;
import response.BaseErrorCode;

public class BannerBusinessException extends BaseException {
    public BannerBusinessException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}

