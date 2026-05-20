package com.example.orderservice.domain.exception;

/**
 * Domain Exception for Order business logic violations
 */
public class OrderDomainException extends RuntimeException {

    public OrderDomainException(String message) {
        super(message);
    }

    public OrderDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}

