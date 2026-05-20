package com.example.productservice.application.port.out;

import com.example.productservice.domain.model.Product;

public interface ProductEventPublisherPort {

    void publishProductCreated(Product product);

    void publishProductUpdated(Product product);

    void publishProductDeleted(String productId);

    void publishProductStatusChanged(Product product);
}
