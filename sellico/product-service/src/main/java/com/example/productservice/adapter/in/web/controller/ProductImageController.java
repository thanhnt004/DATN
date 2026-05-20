package com.example.productservice.adapter.in.web.controller;

import com.example.productservice.adapter.in.web.request.AddImageRequest;
import com.example.productservice.adapter.in.web.response.ProductImageResponse;
import com.example.productservice.application.command.AddImageCommand;
import com.example.productservice.application.port.in.image.AddProductImageUseCase;
import com.example.productservice.application.port.in.image.DeleteImageUseCase;
import com.example.productservice.application.port.in.image.GetProductImagesUseCase;
import com.example.productservice.application.port.in.image.SetPrimaryImageUseCase;
import com.example.productservice.domain.model.ProductImage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductImageController {

    private final AddProductImageUseCase addProductImageUseCase;
    private final DeleteImageUseCase deleteImageUseCase;
    private final GetProductImagesUseCase getProductImagesUseCase;
    private final SetPrimaryImageUseCase setPrimaryImageUseCase;

    /**
     * GET /api/v1/products/{id}/images - Get all images of a product
     */
    @GetMapping("/{id}/images")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> getProductImages(
            @PathVariable("id") UUID productId) {

        List<ProductImage> images = getProductImagesUseCase.getProductImages(productId);

        List<ProductImageResponse> response = images.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/products/{id}/images - Upload/Add image to product
     */
    @PostMapping("/{id}/images")
    public ResponseEntity<ApiResponse<Void>> uploadProductImage(
            @PathVariable("id") UUID productId,
            @Valid @RequestBody AddImageRequest request) {

        AddImageCommand command = new AddImageCommand(
                productId,
                request.getUrl(),
                request.getIsPrimary(),
                request.getSortOrder()
        );

        addProductImageUseCase.addProductImage(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null));
    }

    /**
     * DELETE /api/v1/products/{id}/images/{imageId} - Delete an image from product
     */
    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteProductImage(
            @PathVariable("id") UUID productId,
            @PathVariable("imageId") UUID imageId) {

        deleteImageUseCase.deleteImage(productId, imageId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * PUT /api/v1/products/{id}/images/{imageId}/primary - Set an image as primary
     */
    @PutMapping("/{id}/images/{imageId}/primary")
    public ResponseEntity<ApiResponse<Void>> setPrimaryImage(
            @PathVariable("id") UUID productId,
            @PathVariable("imageId") UUID imageId) {

        setPrimaryImageUseCase.setPrimaryImage(productId, imageId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private ProductImageResponse toResponse(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .url(image.getUrl())
                .isPrimary(image.getIsPrimary())
                .sortOrder(image.getSortOrder())
                .build();
    }
}
