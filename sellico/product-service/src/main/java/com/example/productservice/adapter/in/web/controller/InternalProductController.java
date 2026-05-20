package com.example.productservice.adapter.in.web.controller;

import com.example.productservice.adapter.in.web.request.BatchProductsRequest;
import com.example.productservice.adapter.in.web.request.BatchSkusRequest;
import com.example.productservice.adapter.in.web.request.UpdateProductRatingRequest;
import com.example.productservice.adapter.in.web.request.UpdateSoldCountRequest;
import com.example.productservice.adapter.in.web.response.ProductSummaryResponse;
import com.example.productservice.adapter.in.web.response.SkuDetailResponse;
import com.example.productservice.application.command.GetBatchProductsCommand;
import com.example.productservice.application.command.UpdateProductRatingCommand;
import com.example.productservice.application.command.UpdateSoldCountCommand;
import com.example.productservice.application.port.in.*;
import com.example.productservice.application.port.out.ProductEventPublisherPort;
import com.example.productservice.application.port.out.ProductRepositoryPort;
import com.example.productservice.domain.model.Product;
import com.example.productservice.domain.model.ProductImage;
import com.example.productservice.domain.model.ProductOptionValue;
import com.example.productservice.domain.model.ProductSku;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for internal service communication endpoints
 * Base path: /internal/v1
 *
 * These endpoints are used for inter-service communication and should be
 * protected by service mesh or internal network policies.
 */
@Slf4j
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalProductController {

    private final GetBatchProductsUseCase getBatchProductsUseCase;
    private final GetSkuByCodeUseCase getSkuByCodeUseCase;
    private final GetBatchSkusUseCase getBatchSkusUseCase;
    private final UpdateProductRatingUseCase updateProductRatingUseCase;
    private final UpdateSoldCountUseCase updateSoldCountUseCase;
    private final ProductRepositoryPort productRepositoryPort;
    private final ProductEventPublisherPort productEventPublisherPort;

    // =====================================================
    // Batch Product Operations
    // =====================================================

    /**
     * GET /internal/v1/products/batch
     * Get multiple products by their IDs
     * Used by: Order Service, Cart Service, etc.
     */
    @GetMapping("/products/batch")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getBatchProducts(
            @RequestParam("ids") List<UUID> ids
    ) {
        GetBatchProductsCommand command = GetBatchProductsCommand.builder()
                .productIds(ids)
                .build();

        List<Product> products = getBatchProductsUseCase.getBatchProducts(command);

        List<ProductSummaryResponse> response = products.stream()
                .map(this::toProductSummaryResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /internal/v1/products/batch
     * Get multiple products by their IDs (POST version for large ID lists)
     * Used by: Order Service, Cart Service, etc.
     */
    @PostMapping("/products/batch")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getBatchProductsPost(
            @Valid @RequestBody BatchProductsRequest request
    ) {
        GetBatchProductsCommand command = GetBatchProductsCommand.builder()
                .productIds(request.getProductIds())
                .build();

        List<Product> products = getBatchProductsUseCase.getBatchProducts(command);

        List<ProductSummaryResponse> response = products.stream()
                .map(this::toProductSummaryResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // SKU Operations
    // =====================================================

    /**
     * GET /internal/v1/skus/{skuCode}
     * Get SKU by its code
     * Used by: Inventory Service, Order Service, etc.
     */
    @GetMapping("/skus/{skuCode}")
    public ResponseEntity<ApiResponse<SkuDetailResponse>> getSkuByCode(
            @PathVariable("skuCode") String skuCode
    ) {
        GetSkuByCodeUseCase.SkuWithProduct result = getSkuByCodeUseCase.getSkuWithProductByCode(skuCode);
        return ResponseEntity.ok(ApiResponse.success(toSkuDetailResponse(result)));
    }

    /**
     * POST /internal/v1/skus/batch
     * Get multiple SKUs by their IDs or codes
     * Used by: Cart Service, Order Service for validation
     */
    @PostMapping("/skus/batch")
    public ResponseEntity<ApiResponse<List<SkuDetailResponse>>> getBatchSkus(
            @Valid @RequestBody BatchSkusRequest request
    ) {
        List<ProductSku> skus;
        if (request.getSkuIds() != null && !request.getSkuIds().isEmpty()) {
            skus = getBatchSkusUseCase.getBatchSkusByIds(request.getSkuIds());
        } else if (request.getSkuCodes() != null && !request.getSkuCodes().isEmpty()) {
            skus = getBatchSkusUseCase.getBatchSkusByCodes(request.getSkuCodes());
        } else {
            skus = List.of();
        }

        List<SkuDetailResponse> response = skus.stream()
                .map(this::toSimpleSkuDetailResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /internal/v1/skus/batch
     * Get multiple SKUs by their IDs (GET version)
     * Used by: Cart Service, Order Service for validation
     */
    @GetMapping("/skus/batch")
    public ResponseEntity<ApiResponse<List<SkuDetailResponse>>> getBatchSkusByIds(
            @RequestParam("ids") List<UUID> ids
    ) {
        List<ProductSku> skus = getBatchSkusUseCase.getBatchSkusByIds(ids);

        List<SkuDetailResponse> response = skus.stream()
                .map(this::toSimpleSkuDetailResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // Rating Operations
    // =====================================================

    /**
     * PUT /internal/v1/products/{id}/ratings
     * Update product rating
     * Used by: Review Service (after a review is created/updated/deleted)
     */
    @PutMapping("/products/{id}/ratings")
    public ResponseEntity<ApiResponse<ProductSummaryResponse>> updateProductRating(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateProductRatingRequest request
    ) {
        UpdateProductRatingCommand command = UpdateProductRatingCommand.builder()
                .productId(id)
                .ratingAvg(request.getRatingAvg())
                .ratingCount(request.getRatingCount())
                .build();

        Product product = updateProductRatingUseCase.updateRating(command);
        return ResponseEntity.ok(ApiResponse.success(toProductSummaryResponse(product)));
    }

    // =====================================================
    // Sold Count Operations
    // =====================================================

    /**
     * PATCH /internal/v1/products/{id}/sold-count
     * Increment product sold count
     * Used by: Order Service (after successful payment)
     */
    @PatchMapping("/products/{id}/sold-count")
    public ResponseEntity<ApiResponse<ProductSummaryResponse>> updateSoldCount(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateSoldCountRequest request
    ) {
        UpdateSoldCountCommand command = UpdateSoldCountCommand.builder()
                .productId(id)
                .quantity(request.getQuantity())
                .build();

        Product product = updateSoldCountUseCase.updateSoldCount(command);
        return ResponseEntity.ok(ApiResponse.success(toProductSummaryResponse(product)));
    }

    // =====================================================
    // Reindex Operations
    // =====================================================

    /**
     * POST /internal/v1/products/reindex
     * Re-publish all active products to Kafka so search-service re-indexes them in Elasticsearch.
     * Used by: Admin / DevOps to rebuild the search index.
     */
    @PostMapping("/products/reindex")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reindexAllProducts() {
        log.info("Reindex requested: loading all ACTIVE products...");
        int totalPublished = 0;
        int page = 0;
        int size = 100;

        while (true) {
            Page<Product> productPage = productRepositoryPort.findAllWithFilters(
                    null, null, "ACTIVE", null, null, null, null,
                    page, size, "createdAt", "desc"
            );

            for (Product product : productPage.getContent()) {
                try {
                    productEventPublisherPort.publishProductUpdated(product);
                    totalPublished++;
                } catch (Exception e) {
                    log.error("Failed to publish product {} for reindex: {}", product.getId(), e.getMessage());
                }
            }

            log.info("Reindex progress: published page {}/{} ({} products so far)",
                    page + 1, productPage.getTotalPages(), totalPublished);

            if (!productPage.hasNext()) {
                break;
            }
            page++;
        }

        log.info("Reindex completed: {} products published to Kafka", totalPublished);

        Map<String, Object> result = Map.of(
                "totalPublished", totalPublished,
                "message", "Reindex completed. Products will appear in Elasticsearch shortly."
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // =====================================================
    // Mapper methods
    // =====================================================

    private ProductSummaryResponse toProductSummaryResponse(Product product) {
        String primaryImageUrl = null;
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            primaryImageUrl = product.getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .findFirst()
                    .map(ProductImage::getUrl)
                    .orElse(product.getImages().get(0).getUrl());
        }

        return ProductSummaryResponse.builder()
                .id(product.getId())
                .sellerId(product.getSellerId())
                .categoryId(product.getCategoryId())
                .name(product.getName())
                .slug(product.getSlug())
                .status(product.getStatus())
                .minPrice(product.getMinPrice())
                .maxPrice(product.getMaxPrice())
                .ratingAvg(product.getRatingAvg())
                .ratingCount(product.getRatingCount())
                .soldCount(product.getSoldCount())
                .primaryImageUrl(primaryImageUrl)
                .build();
    }

    private String resolveSkuImageUrl(ProductSku sku) {
        // 1. Try option-value image (e.g. color variant swatch)
        if (sku.getSelectedValues() != null) {
            String optionImage = sku.getSelectedValues().stream()
                    .map(ProductOptionValue::getImageUrl)
                    .filter(url -> url != null && !url.isEmpty())
                    .findFirst()
                    .orElse(null);
            if (optionImage != null) return optionImage;
        }
        // 2. Fallback to product primary image
        return sku.getPrimaryImageUrl();
    }

    private SkuDetailResponse toSkuDetailResponse(GetSkuByCodeUseCase.SkuWithProduct result) {
        ProductSku sku = result.sku();
        Map<String, String> attributes = new java.util.LinkedHashMap<>();
        if (sku.getSelectedValues() != null) {
            for (var val : sku.getSelectedValues()) {
                String optName = val.getOptionName() != null ? val.getOptionName() : "Option";
                attributes.put(optName, val.getValue());
            }
        }
        return SkuDetailResponse.builder()
                .id(sku.getId())
                .productId(result.productId())
                .productName(result.productName())
                .sellerId(result.sellerId())
                .skuCode(sku.getSkuCode())
                .price(sku.getPrice())
                .originalPrice(sku.getOriginalPrice())
                .status(sku.getStatus())
                .imageUrl(resolveSkuImageUrl(sku))
                .weightGram(sku.getWeightGram())
                .lengthCm(sku.getLengthCm())
                .widthCm(sku.getWidthCm())
                .heightCm(sku.getHeightCm())
                .attributes(attributes)
                .build();
    }

    private SkuDetailResponse toSimpleSkuDetailResponse(ProductSku sku) {
        Map<String, String> attributes = new java.util.LinkedHashMap<>();
        if (sku.getSelectedValues() != null) {
            for (var val : sku.getSelectedValues()) {
                String optName = val.getOptionName() != null ? val.getOptionName() : "Option";
                attributes.put(optName, val.getValue());
            }
        }
        return SkuDetailResponse.builder()
                .id(sku.getId())
                .productId(sku.getProductId())
                .productName(sku.getProductName())
                .sellerId(sku.getSellerId())
                .skuCode(sku.getSkuCode())
                .price(sku.getPrice())
                .originalPrice(sku.getOriginalPrice())
                .status(sku.getStatus())
                .imageUrl(resolveSkuImageUrl(sku))
                .weightGram(sku.getWeightGram())
                .lengthCm(sku.getLengthCm())
                .widthCm(sku.getWidthCm())
                .heightCm(sku.getHeightCm())
                .attributes(attributes)
                .build();
    }
}
