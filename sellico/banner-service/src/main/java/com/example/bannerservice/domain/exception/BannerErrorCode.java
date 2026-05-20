package com.example.bannerservice.domain.exception;

import response.BaseErrorCode;

public enum BannerErrorCode implements BaseErrorCode {
    BANNER_NOT_FOUND("BANNER_NOT_FOUND", "Banner not found", 404),
    POSITION_NOT_FOUND("POSITION_NOT_FOUND", "Banner position not found", 404),
    POSITION_ALREADY_EXISTS("POSITION_ALREADY_EXISTS", "Banner position already exists", 409),
    POSITION_MAX_BANNERS_REACHED("POSITION_MAX_BANNERS_REACHED", "Maximum banners for this position reached", 400),
    INVALID_STATUS("INVALID_STATUS", "Invalid banner status", 400),
    INVALID_STATUS_TRANSITION("INVALID_STATUS_TRANSITION", "Invalid status transition", 400),
    INVALID_DATE_RANGE("INVALID_DATE_RANGE", "End date must be after start date", 400),
    INVALID_LINK_TYPE("INVALID_LINK_TYPE", "Invalid link type", 400),
    IMAGE_URL_REQUIRED("IMAGE_URL_REQUIRED", "Image URL is required", 400),
    TITLE_REQUIRED("TITLE_REQUIRED", "Banner title is required", 400),
    POSITION_CODE_REQUIRED("POSITION_CODE_REQUIRED", "Position code is required", 400),
    SCHEDULE_DATES_REQUIRED("SCHEDULE_DATES_REQUIRED", "Start and end dates are required for scheduled banners", 400),
    POSITION_NOT_ACTIVE("POSITION_NOT_ACTIVE", "Banner position is not active", 400);

    private final String code;
    private final String message;
    private final int statusCode;

    BannerErrorCode(String code, String message, int statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    @Override
    public String getCode() { return code; }
    @Override
    public String getMessage() { return message; }
    @Override
    public int getStatusCode() { return statusCode; }
}

