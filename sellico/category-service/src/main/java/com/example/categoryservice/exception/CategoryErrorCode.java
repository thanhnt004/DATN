package com.example.categoryservice.exception;

import response.BaseErrorCode;

public enum CategoryErrorCode implements BaseErrorCode {
    NANE_ALREADY_EXISTS("CATEGORY_001", "Category name already exists", 400),
    SLUG_ALREADY_EXISTS("CATEGORY_002", "Category slug already exists", 400),
    PARENT_CATEGORY_NOT_FOUND("CATEGORY_003", "Parent category not found",404 ),
    SELF_PARENTING("CATEGORY_004", "Category cannot be parent itself",400),
    CANNOT_SET_CHILD_AS_PARENT("CATEGORY_005", "Cannot set child category as parent",400),

    CATEGORY_NOT_FOUND("CATEGORY_006","Category not found" ,404 ),
    ATTRIBUTE_NOT_FOUND("CATEGORY_007","Category attribute not found" ,404 );
    CategoryErrorCode( String code, String message, int statusCode) {
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
