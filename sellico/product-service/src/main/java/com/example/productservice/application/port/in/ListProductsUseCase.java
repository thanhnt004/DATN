package com.example.productservice.application.port.in;

import com.example.productservice.application.command.ListProductsCommand;
import com.example.productservice.domain.model.Product;
import org.springframework.data.domain.Page;

public interface ListProductsUseCase {
    /**
     * List products with pagination and filtering
     */
    Page<Product> listProducts(ListProductsCommand command);
}
