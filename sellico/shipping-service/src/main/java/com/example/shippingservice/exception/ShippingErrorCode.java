package com.example.shippingservice.exception;

import response.BaseErrorCode;

public enum ShippingErrorCode implements BaseErrorCode {
    SHIPPING_DISTRICT_NOT_FOUND("SHIPPING_DISTRICT_NOT_FOUND", "Shipping district not found", 404),
    SHIPPING_WARD_NOT_FOUND("SHIPPING_WARD_NOT_FOUND", "Shipping ward not found", 404),
    ;

    private final String code;
    private final String message;
    private final int statusCode;

    ShippingErrorCode(String code, String message, int statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }
}
