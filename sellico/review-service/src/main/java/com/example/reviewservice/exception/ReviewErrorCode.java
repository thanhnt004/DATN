package com.example.reviewservice.exception;

import response.BaseErrorCode;

public enum ReviewErrorCode implements BaseErrorCode {

    REVIEW_NOT_FOUND("REVIEW_NOT_FOUND", "Review not found", 404),
    REVIEW_ALREADY_EXISTS("REVIEW_ALREADY_EXISTS", "You have already reviewed this product for this order", 409),
    ORDER_NOT_BELONGS_TO_USER("ORDER_NOT_BELONGS_TO_USER", "This order does not belong to you", 403),
    ORDER_NOT_COMPLETED("ORDER_NOT_COMPLETED", "Order must be completed before reviewing", 400),
    ORDER_NOT_HAS_PRODUCT("ORDER_NOT_HAS_PRODUCT", "This order does not contain the specified product", 400),
    REPLY_ALREADY_EXISTS("REPLY_ALREADY_EXISTS", "A reply already exists for this review", 409),
    REPLY_NOT_FOUND("REPLY_NOT_FOUND", "Reply not found", 404),
    NOT_REVIEW_OWNER("NOT_REVIEW_OWNER", "You are not the owner of this review", 403),
    NOT_SELLER_OF_PRODUCT("NOT_SELLER_OF_PRODUCT", "You are not the seller of this product", 403),
    INVALID_RATING("INVALID_RATING", "Rating must be between 1 and 5", 400);

    private final String code;
    private final String message;
    private final int statusCode;

    ReviewErrorCode(String code, String message, int statusCode) {
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
