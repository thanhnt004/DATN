package com.example.productservice.application.service.image;

import com.example.productservice.application.command.AddImageCommand;
import com.example.productservice.application.port.in.image.AddProductImageUseCase;
import com.example.productservice.application.port.out.ProductRepositoryPort;
import com.example.productservice.domain.exception.ProductBusinessException;
import com.example.productservice.domain.exception.ProductErrorCode;
import com.example.productservice.domain.model.Product;
import com.example.productservice.domain.model.ProductImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddImageService implements AddProductImageUseCase {
    private final ProductRepositoryPort productRepositoryPort;

    @Override
    @Transactional
    public void addProductImage(AddImageCommand command) {
        Product product = productRepositoryPort.findById(command.getProductId())
                .orElseThrow(() -> new ProductBusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        if (command.getUrl() == null || command.getUrl().isBlank()) {
            throw new ProductBusinessException(ProductErrorCode.IMAGE_URL_REQUIRED);
        }

        ProductImage image = ProductImage.create(
                command.getUrl(),
                command.getIsPrimary(),
                command.getSortOrder()
        );

        product.addImage(image);
        productRepositoryPort.save(product);
    }
}
