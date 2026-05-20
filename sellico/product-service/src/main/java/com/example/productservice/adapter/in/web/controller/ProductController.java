package com.example.productservice.adapter.in.web.controller;

import com.example.productservice.adapter.in.web.mapper.ProductWebMapper;
import com.example.productservice.adapter.in.web.request.CreateProductRequest;
import com.example.productservice.adapter.in.web.request.UpdateProductStatusRequest;
import com.example.productservice.adapter.in.web.response.ProductResponse;
import com.example.productservice.adapter.in.web.response.ProductSummaryResponse;
import com.example.productservice.application.command.CreateProductCommand;
import com.example.productservice.application.command.ListProductsCommand;
import com.example.productservice.application.command.UpdateProductStatusCommand;
import com.example.productservice.application.port.in.*;
import com.example.productservice.domain.model.Product;
import com.example.productservice.domain.model.ProductImage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import com.example.productservice.application.port.out.ProductEventPublisherPort;
import com.example.productservice.application.port.out.ProductRepositoryPort;
import com.example.productservice.application.port.out.SellerClientPort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for public product endpoints
 * Base path: /api/v1/products
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final GetProductUseCase getProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    private final UpdateProductStatusUseCase updateProductStatusUseCase;
    private final ListProductsUseCase listProductsUseCase;
    private final GetRelatedProductsUseCase getRelatedProductsUseCase;
    private final GetSellerProductsUseCase getSellerProductsUseCase;
    private final ProductWebMapper mapper;
    private final SellerClientPort sellerClientPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final ProductEventPublisherPort productEventPublisherPort;

    /**
     * GET /api/v1/products - List products (paginated, filtered)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> listProducts(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortDirection", required = false) String sortDirection,
            @RequestParam(value = "categoryId", required = false) UUID categoryId,
            @RequestParam(value = "sellerId", required = false) UUID sellerId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "minRating", required = false) BigDecimal minRating
    ) {
        ListProductsCommand command = ListProductsCommand.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .categoryId(categoryId)
                .sellerId(sellerId)
                .status(status)
                .keyword(keyword)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .minRating(minRating)
                .build();

        Page<Product> products = listProductsUseCase.listProducts(command);
        Page<ProductResponse> response = products.map(mapper::toResponse);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/products/{id} - Get product details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable("id") UUID id) {
        Product product = getProductUseCase.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(product)));
    }

    /**
     * GET /api/v1/products/slug/{slug} - Get product by slug
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySlug(@PathVariable("slug") String slug) {
        Product product = getProductUseCase.getProductBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(product)));
    }

    /**
     * POST /api/v1/products - Create product (seller)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        CreateProductCommand command = mapper.toCommand(request, userId);
        Product createdProduct = createProductUseCase.createProduct(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(mapper.toResponse(createdProduct)));
    }


    /**
     * DELETE /api/v1/products/{id} - Delete product (seller)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID sellerId = sellerClientPort.getSellerIdByUserId(UUID.fromString(jwt.getSubject()));
        deleteProductUseCase.deleteProduct(id, sellerId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * PATCH /api/v1/products/{id}/status - Update product status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateProductStatusRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID sellerId = sellerClientPort.getSellerIdByUserId(UUID.fromString(jwt.getSubject()));
        UpdateProductStatusCommand command = UpdateProductStatusCommand.builder()
                .productId(id)
                .sellerId(sellerId)
                .status(request.getStatus())
                .build();
        Product updatedProduct = updateProductStatusUseCase.updateStatus(command);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(updatedProduct)));
    }

    // =====================================================
    // Related Products
    // =====================================================

    /**
     * GET /api/v1/products/{id}/related - Get related/similar products
     */
    @GetMapping("/{id}/related")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getRelatedProducts(
            @PathVariable("id") UUID id,
            @RequestParam(value = "limit", defaultValue = "12") int limit
    ) {
        List<Product> related = getRelatedProductsUseCase.getRelatedProducts(id, limit);
        List<ProductSummaryResponse> response = related.stream()
                .map(this::toSummaryResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // Seller's own product management
    // =====================================================

    /**
     * GET /api/v1/products/seller/me - Get current seller's products
     */
    @GetMapping("/seller/me")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getMyProducts(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID sellerId = sellerClientPort.getSellerIdByUserId(UUID.fromString(jwt.getSubject()));
        Page<Product> products = getSellerProductsUseCase.getSellerProducts(
                sellerId, status, keyword, page, size, sortBy, sortDirection
        );
        Page<ProductResponse> response = products.map(mapper::toResponse);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/products/seller/{sellerId} - Get products by seller (public)
     */
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<ApiResponse<Page<ProductSummaryResponse>>> getSellerProducts(
            @PathVariable("sellerId") UUID sellerId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection
    ) {
        // Public view - only show ACTIVE products
        Page<Product> products = getSellerProductsUseCase.getSellerProducts(
                sellerId, "ACTIVE", keyword, page, size, sortBy, sortDirection
        );
        Page<ProductSummaryResponse> response = products.map(this::toSummaryResponse);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // Reindex / Sync to Search Service
    // =====================================================

    /**
     * POST /api/v1/products/reindex
     * Re-publish all products to Kafka so search-service re-indexes them in Elasticsearch.
     * Useful when data is inserted directly into DB (e.g. scraping) and search index is out of sync.
     */
    @PostMapping("/reindex")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reindexAllProducts(
            @RequestParam(value = "status", required = false) String status
    ) {
        log.info("Reindex requested via public API: status filter = {}", status);
        int totalPublished = 0;
        int totalFailed = 0;
        int page = 0;
        int size = 100;

        while (true) {
            Page<Product> productPage = productRepositoryPort.findAllWithFilters(
                    null, null, status, null, null, null, null,
                    page, size, "createdAt", "desc"
            );

            for (Product product : productPage.getContent()) {
                try {
                    productEventPublisherPort.publishProductUpdated(product);
                    totalPublished++;
                } catch (Exception e) {
                    totalFailed++;
                    log.error("Failed to publish product {} for reindex: {}", product.getId(), e.getMessage());
                }
            }

            log.info("Reindex progress: page {}/{} — published {} so far",
                    page + 1, productPage.getTotalPages(), totalPublished);

            if (!productPage.hasNext()) break;
            page++;
        }

        log.info("Reindex completed: {} published, {} failed", totalPublished, totalFailed);

        Map<String, Object> result = Map.of(
                "totalPublished", totalPublished,
                "totalFailed", totalFailed,
                "message", "Reindex completed. Products will appear in search shortly."
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // =====================================================
    // Mapper helpers
    // =====================================================

    private ProductSummaryResponse toSummaryResponse(Product product) {
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
}
