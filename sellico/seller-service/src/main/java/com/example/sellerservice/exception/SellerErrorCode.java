package com.example.sellerservice.exception;

import response.BaseErrorCode;

public enum SellerErrorCode implements BaseErrorCode {
    // Seller
    SELLER_NOT_FOUND("SELLER_NOT_FOUND", "Seller not found", 404),
    SELLER_ALREADY_EXISTS("SELLER_ALREADY_EXISTS", "User already has a seller account", 400),
    SHOP_NAME_ALREADY_EXISTS("SHOP_NAME_ALREADY_EXISTS", "Shop name already exists", 400),
    SHOP_SLUG_ALREADY_EXISTS("SHOP_SLUG_ALREADY_EXISTS", "Shop slug already exists", 400),
    SELLER_NOT_ACTIVE("SELLER_NOT_ACTIVE", "Seller account is not active", 403),
    SELLER_SUSPENDED("SELLER_SUSPENDED", "Seller account is suspended", 403),
    SELLER_BANNED("SELLER_BANNED", "Seller account is banned", 403),
    INVALID_SELLER_TYPE("INVALID_SELLER_TYPE", "Invalid seller type", 400),
    BUSINESS_INFO_REQUIRED("BUSINESS_INFO_REQUIRED", "Business information is required for business sellers", 400),
    INVALID_STATUS_TRANSITION("INVALID_STATUS_TRANSITION", "Invalid seller status transition", 400),
    SELLER_NOT_PENDING("SELLER_NOT_PENDING", "Seller is not in pending status", 400),
    SELLER_CANNOT_RESUBMIT("SELLER_CANNOT_RESUBMIT", "Seller cannot resubmit registration in current status", 400),

    // Document
    DOCUMENT_NOT_FOUND("DOCUMENT_NOT_FOUND", "Document not found", 404),
    DOCUMENT_ALREADY_EXISTS("DOCUMENT_ALREADY_EXISTS", "Document of this type already exists", 400),
    DOCUMENT_ALREADY_VERIFIED("DOCUMENT_ALREADY_VERIFIED", "Document is already verified", 400),
    INVALID_DOCUMENT_TYPE("INVALID_DOCUMENT_TYPE", "Invalid document type", 400),

    // Bank Account
    BANK_ACCOUNT_NOT_FOUND("BANK_ACCOUNT_NOT_FOUND", "Bank account not found", 404),
    BANK_ACCOUNT_ALREADY_EXISTS("BANK_ACCOUNT_ALREADY_EXISTS", "Bank account already exists", 400),

    // Follower
    ALREADY_FOLLOWING("ALREADY_FOLLOWING", "Already following this seller", 400),
    NOT_FOLLOWING("NOT_FOLLOWING", "Not following this seller", 400),
    CANNOT_FOLLOW_OWN_SHOP("CANNOT_FOLLOW_OWN_SHOP", "Cannot follow your own shop", 400),

    // General
    UNAUTHORIZED("UNAUTHORIZED", "Unauthorized access", 401),
    FORBIDDEN("FORBIDDEN", "Access forbidden", 403),
    INVALID_REQUEST("INVALID_REQUEST", "Invalid request", 400);

    private final String code;
    private final String message;
    private final int statusCode;

    SellerErrorCode(String code, String message, int statusCode) {
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

