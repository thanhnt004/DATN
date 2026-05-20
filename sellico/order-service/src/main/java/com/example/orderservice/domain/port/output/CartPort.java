package com.example.orderservice.domain.port.output;

import java.util.UUID;

/**
 * Output Port - Cart Service Interface
 */
public interface CartPort {

    /**
     * Clear selected items from user's cart after order creation
     */
    void clearSelectedItems(UUID userId);
}

