package com.example.inventoryservice.exception;

import response.BaseErrorCode;

public enum InventoryErrorCode implements BaseErrorCode {
    // Inventory
    INVENTORY_NOT_FOUND("INVENTORY_NOT_FOUND", "Inventory not found for SKU", 404),
    INVENTORY_ALREADY_EXISTS("INVENTORY_ALREADY_EXISTS", "Inventory already exists for SKU", 400),
    INSUFFICIENT_STOCK("INSUFFICIENT_STOCK", "Insufficient stock available", 400),
    INVALID_STOCK_QUANTITY("INVALID_STOCK_QUANTITY", "Invalid stock quantity", 400),

    // Reservation
    RESERVATION_NOT_FOUND("RESERVATION_NOT_FOUND", "Reservation not found", 404),
    RESERVATION_ALREADY_EXISTS("RESERVATION_ALREADY_EXISTS", "Reservation already exists for this order and SKU", 400),
    RESERVATION_EXPIRED("RESERVATION_EXPIRED", "Reservation has expired", 400),
    RESERVATION_ALREADY_CONFIRMED("RESERVATION_ALREADY_CONFIRMED", "Reservation is already confirmed", 400),
    RESERVATION_ALREADY_CANCELLED("RESERVATION_ALREADY_CANCELLED", "Reservation is already cancelled", 400),
    CANNOT_CONFIRM_RESERVATION("CANNOT_CONFIRM_RESERVATION", "Cannot confirm reservation", 400),
    CANNOT_CANCEL_RESERVATION("CANNOT_CANCEL_RESERVATION", "Cannot cancel reservation", 400),

    // General
    INVALID_REQUEST("INVALID_REQUEST", "Invalid request", 400),
    INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error", 500);

    private final String code;
    private final String message;
    private final int statusCode;

    InventoryErrorCode(String code, String message, int statusCode) {
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

