package com.example.productservice.adapter.in.web.controller;

import com.example.productservice.adapter.in.web.mapper.ProductWebMapper;
import com.example.productservice.adapter.in.web.request.*;
import com.example.productservice.adapter.in.web.response.ProductResponse;
import com.example.productservice.adapter.in.web.response.SkuResponse;
import com.example.productservice.application.command.*;
import com.example.productservice.application.port.in.*;
import com.example.productservice.application.port.out.SellerClientPort;
import com.example.productservice.domain.model.Product;
import com.example.productservice.domain.model.ProductSku;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for Seller-specific product management endpoints
 * Base path: /api/v1/seller/products
 */
@RestController
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
public class SellerProductController {

    private final UpdateProductBasicInfoUseCase updateProductBasicInfoUseCase;
    private final UpdateProductImagesUseCase updateProductImagesUseCase;
    private final UpdateProductSpecificationsUseCase updateProductSpecificationsUseCase;
    private final UpdateProductSkusUseCase updateProductSkusUseCase;
    private final UpdateSkuUseCase updateSkuUseCase;
    private final ProductWebMapper mapper;
    private final SellerClientPort sellerClientPort;

    // =====================================================
    // Nhóm 1: Thông tin cơ bản (General Info)
    // =====================================================

    /**
     * PATCH /api/v1/seller/products/{id}
     * Update basic product information (name, slug, description, categoryId)
     */
    @PatchMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateBasicInfo(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateProductBasicInfoRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID sellerId = sellerClientPort.getSellerIdByUserId(UUID.fromString(jwt.getSubject()));

        UpdateProductBasicInfoCommand command = UpdateProductBasicInfoCommand.builder()
                .productId(id)
                .sellerId(sellerId)
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .categoryId(request.getCategoryId())
                .build();

        Product product = updateProductBasicInfoUseCase.updateBasicInfo(command);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(product)));
    }

    // =====================================================
    // Nhóm 2: Hình ảnh (Media)
    // =====================================================

    /**
     * PUT /api/v1/seller/products/{id}/images
     * Replace all product images
     */
    @PutMapping("/products/{id}/images")
    public ResponseEntity<ApiResponse<ProductResponse>> updateImages(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateProductImagesRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID sellerId = sellerClientPort.getSellerIdByUserId(UUID.fromString(jwt.getSubject()));

        UpdateProductImagesCommand command = UpdateProductImagesCommand.builder()
                .productId(id)
                .sellerId(sellerId)
                .images(request.getImages().stream()
                        .map(img -> UpdateProductImagesCommand.ImageItem.builder()
                                .url(img.getUrl())
                                .isPrimary(img.getIsPrimary())
                                .sortOrder(img.getSortOrder())
                                .build())
                        .collect(Collectors.toList()))
                .build();

        Product product = updateProductImagesUseCase.updateImages(command);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(product)));
    }

    // =====================================================
    // Nhóm 3: Thông số kỹ thuật (Specifications)
    // =====================================================

    /**
     * PUT /api/v1/seller/products/{id}/specifications
     * Replace all product specifications (JSONB)
     */
    @PutMapping("/products/{id}/specifications")
    public ResponseEntity<ApiResponse<ProductResponse>> updateSpecifications(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateProductSpecificationsRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID sellerId = sellerClientPort.getSellerIdByUserId(UUID.fromString(jwt.getSubject()));

        UpdateProductSpecificationsCommand command = UpdateProductSpecificationsCommand.builder()
                .productId(id)
                .sellerId(sellerId)
                .specifications(request.getSpecifications())
                .build();

        Product product = updateProductSpecificationsUseCase.updateSpecifications(command);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(product)));
    }

    // =====================================================
    // Nhóm 4: Biến thể và Giá (SKUs)
    // =====================================================

    /**
     * PATCH /api/v1/seller/products/{id}/skus
     * Batch update/replace all SKUs for a product
     */
    @PatchMapping("/products/{id}/skus")
    public ResponseEntity<ApiResponse<ProductResponse>> updateSkus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateProductSkusRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID sellerId = sellerClientPort.getSellerIdByUserId(UUID.fromString(jwt.getSubject()));

        UpdateProductSkusCommand command = UpdateProductSkusCommand.builder()
                .productId(id)
                .sellerId(sellerId)
                .skus(request.getSkus().stream()
                        .map(sku -> UpdateProductSkusCommand.SkuItem.builder()
                                .skuCode(sku.getSkuCode())
                                .price(sku.getPrice())
                                .originalPrice(sku.getOriginalPrice())
                                .costPrice(sku.getCostPrice())
                                .weightGram(sku.getWeightGram())
                                .lengthCm(sku.getDimensions() != null ? sku.getDimensions().getLengthCm() : null)
                                .widthCm(sku.getDimensions() != null ? sku.getDimensions().getWidthCm() : null)
                                .heightCm(sku.getDimensions() != null ? sku.getDimensions().getHeightCm() : null)
                                .selectionAttributes(sku.getSelectionAttributes())
                                .build())
                        .collect(Collectors.toList()))
                .build();

        Product product = updateProductSkusUseCase.updateSkus(command);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(product)));
    }

    /**
     * PATCH /api/v1/seller/skus/{skuId}
     * Update a single SKU
     */
    @PatchMapping("/skus/{skuId}")
    public ResponseEntity<ApiResponse<SkuResponse>> updateSku(
            @PathVariable("skuId") UUID skuId,
            @Valid @RequestBody UpdateSkuRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID sellerId = sellerClientPort.getSellerIdByUserId(UUID.fromString(jwt.getSubject()));

        UpdateSkuCommand command = UpdateSkuCommand.builder()
                .skuId(skuId)
                .sellerId(sellerId)
                .price(request.getPrice())
                .originalPrice(request.getOriginalPrice())
                .costPrice(request.getCostPrice())
                .weightGram(request.getWeightGram())
                .lengthCm(request.getDimensions() != null ? request.getDimensions().getLengthCm() : null)
                .widthCm(request.getDimensions() != null ? request.getDimensions().getWidthCm() : null)
                .heightCm(request.getDimensions() != null ? request.getDimensions().getHeightCm() : null)
                .status(request.getStatus())
                .build();

        ProductSku sku = updateSkuUseCase.updateSku(command);
        return ResponseEntity.ok(ApiResponse.success(toSkuResponse(sku)));
    }

    // =====================================================
    // Mapper methods
    // =====================================================


    private SkuResponse toSkuResponse(ProductSku sku) {
        return SkuResponse.builder()
                .id(sku.getId())
                .skuCode(sku.getSkuCode())
                .price(sku.getPrice())
                .originalPrice(sku.getOriginalPrice())
                .status(sku.getStatus())
                .weightGram(sku.getWeightGram())
                .lengthCm(sku.getLengthCm())
                .widthCm(sku.getWidthCm())
                .heightCm(sku.getHeightCm())
                .build();
    }
}

