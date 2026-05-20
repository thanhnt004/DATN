package com.example.productservice.domain.exception;

import response.BaseErrorCode;

public enum ProductErrorCode implements BaseErrorCode {
    // ===== CATEGORY / SELLER =====
    CATEGORY_NOT_EXISTED("CATEGORY_NOT_EXISTED","Category is not existed",400),
    CATEGORY_IS_NOT_LEAF("CATEGORY_IS_NOT_LEAF","Category is not leaf",400),
    SELLER_ID_REQUIRED("SELLER_ID_REQUIRED","Seller id is required",400),
    SELLER_ID_INVALID("SELLER_INVALID","Your account is not shop owner" ,400 ),
    // ===== PRODUCT BASIC =====
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", "Product not found", 404),
    PRODUCT_NAME_REQUIRED("PRODUCT_NAME_REQUIRED","Product name is required",400),
    PRODUCT_MUST_HAVE_SKU("PRODUCT_MUST_HAVE_SKU","Product must have at least one sku",400),
    INVALID_STATUS("INVALID_STATUS", "Invalid product status", 400),
    INVALID_STATUS_TRANSITION("INVALID_STATUS_TRANSITION", "Invalid status transition", 400),

    // ===== IMAGE =====
    PRODUCT_IMAGE_REQUIRED("PRODUCT_IMAGE_REQUIRED","Product must have image",400),
    ONLY_ONE_PRIMARY_IMAGE("ONLY_ONE_PRIMARY_IMAGE","Only one primary image is allowed",400),
    IMAGE_URL_REQUIRED("IMAGE_URL_REQUIRED","Image url is required",400),
    IMAGE_SORT_ORDER_INVALID("IMAGE_SORT_ORDER_INVALID","Image sort order is invalid",400),
    IMAGE_NOT_FOUND("IMAGE_NOT_FOUND", "Image not found", 404),

    // ===== OPTION =====
    OPTION_NAME_REQUIRED("OPTION_NAME_REQUIRED","Option name is required",400),
    OPTION_VALUE_REQUIRED("OPTION_VALUE_REQUIRED","Option value is required",400),
    DUPLICATE_OPTION_NAME("DUPLICATE_OPTION_NAME","Duplicate option name",400),
    DUPLICATE_OPTION_VALUE("DUPLICATE_OPTION_VALUE","Duplicate option value",400),

    // ===== SKU =====
    DUPLICATE_SKU_CODE("DUPLICATE_SKU_CODE","Duplicate sku code",400),
    PRICE_INVALID("PRICE_INVALID","Price must be greater than zero",400),
    ORIGINAL_PRICE_INVALID("ORIGINAL_PRICE_INVALID","Original price must be greater than or equal to selling price when provided",400),
    STOCK_INVALID("STOCK_INVALID","Stock quantity is invalid",400),
    WEIGHT_INVALID("WEIGHT_INVALID","Weight gram is invalid",400),

    SKU_CODE_REQUIRED("SKU_CODE_REQUIRED","Sku code is required" ,400 ),
    SKU_NOT_FOUND("SKU_NOT_FOUND", "SKU not found", 404),
    SKU_OPTION_MISMATCH("SKU_OPTION_MISMATCH","Sku option does not match product option",400),
    INVALID_OPTION_VALUE("INVALID_OPTION_VALUE","Invalid option value in sku",400),
    DUPLICATE_SKU_COMBINATION("DUPLICATE_SKU_COMBINATION","Duplicate sku combination",400),
    SKU_CODE_ALREADY_EXISTS_FOR_SELLER("SKU_CODE_ALREADY_EXISTS_FOR_SELLER", "Sku codes already exist for shop",400 );

    ProductErrorCode(String code, String message, int statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
    private final String code;
    private final String message;
    private final int statusCode;
    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public int getStatusCode() {
        return this.statusCode;
    }
}
