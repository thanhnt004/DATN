package com.example.discountservice.domain.exception;

import response.BaseErrorCode;

public enum DiscountErrorCode implements BaseErrorCode {
    CAMPAIGN_NOT_FOUND("DISC_001", "Campaign not found", 404),
    COUPON_NOT_FOUND("DISC_002", "Coupon not found", 404),
    COUPON_NOT_ACTIVE("DISC_003", "Coupon is not active", 400),
    COUPON_EXPIRED("DISC_004", "Coupon has expired", 400),
    COUPON_NOT_STARTED("DISC_005", "Coupon is not yet available", 400),
    COUPON_DEPLETED("DISC_006", "Coupon is fully claimed", 400),
    COUPON_ALREADY_CLAIMED("DISC_007", "You have already claimed this coupon", 409),
    COUPON_USAGE_LIMIT_REACHED("DISC_008", "You have reached the usage limit for this coupon", 400),
    MIN_ORDER_NOT_MET("DISC_009", "Order amount does not meet the minimum requirement", 400),
    COUPON_CODE_EXISTS("DISC_010", "Coupon code already exists", 409),
    INVALID_DISCOUNT_VALUE("DISC_011", "Invalid discount value", 400),
    INVALID_DATE_RANGE("DISC_012", "End date must be after start date", 400),
    CLAIM_NOT_FOUND("DISC_013", "Coupon claim not found", 404),
    CLAIM_ALREADY_USED("DISC_014", "Coupon has already been used", 400),
    NOT_COUPON_OWNER("DISC_015", "You do not own this coupon", 403),
    CAMPAIGN_NOT_ACTIVE("DISC_016", "Campaign is not active", 400),
    SELLER_MISMATCH("DISC_017", "Seller does not own this resource", 403),
    INVALID_COUPON_TYPE("DISC_018", "Invalid coupon type for this operation", 400),
    ;

    private final String code;
    private final String message;
    private final int statusCode;

    DiscountErrorCode(String code, String message, int statusCode) {
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

