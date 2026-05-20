package com.example.productservice.application.port.in;


import com.example.productservice.application.command.CreateProductCommand;
import com.example.productservice.domain.model.Product;

public interface CreateProductUseCase {
    /**
     * Tiếp nhận lệnh tạo sản phẩm và trả về thông tin sản phẩm sau khi lưu.
     */
    Product createProduct(CreateProductCommand command);
}
