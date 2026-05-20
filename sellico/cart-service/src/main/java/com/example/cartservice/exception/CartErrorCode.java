package com.example.cartservice.exception;

import response.BaseErrorCode;

public enum CartErrorCode implements BaseErrorCode {
    // Cart
    CART_NOT_FOUND("CART_NOT_FOUND", "Cart not found", 404),
    CART_EMPTY("CART_EMPTY", "Cart is empty", 400),

    // Cart Item
    ITEM_NOT_FOUND("ITEM_NOT_FOUND", "Item not found in cart", 404),
    ITEM_ALREADY_EXISTS("ITEM_ALREADY_EXISTS", "Item already exists in cart", 400),
    INVALID_QUANTITY("INVALID_QUANTITY", "Invalid quantity", 400),
    MAX_QUANTITY_EXCEEDED("MAX_QUANTITY_EXCEEDED", "Maximum quantity exceeded", 400),
    INSUFFICIENT_STOCK("INSUFFICIENT_STOCK", "Insufficient stock available", 400),

    // Saved Item
    SAVED_ITEM_NOT_FOUND("SAVED_ITEM_NOT_FOUND", "Saved item not found", 404),
    SAVED_ITEM_ALREADY_EXISTS("SAVED_ITEM_ALREADY_EXISTS", "Item already saved", 400),

    // Product/SKU
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", "Product not found", 404),
    SKU_NOT_FOUND("SKU_NOT_FOUND", "SKU not found", 404),
    PRODUCT_UNAVAILABLE("PRODUCT_UNAVAILABLE", "Product is unavailable", 400),

    // General
    INVALID_REQUEST("INVALID_REQUEST", "Invalid request", 400),
    INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error", 500);

    private final String code;
    private final String message;
    private final int statusCode;

    CartErrorCode(String code, String message, int statusCode) {
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

