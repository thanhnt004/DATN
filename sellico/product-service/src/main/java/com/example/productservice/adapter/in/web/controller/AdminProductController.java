package com.example.productservice.adapter.in.web.controller;

import com.example.productservice.adapter.in.web.mapper.ProductWebMapper;
import com.example.productservice.adapter.in.web.request.UpdateProductStatusRequest;
import com.example.productservice.adapter.in.web.response.ProductResponse;
import com.example.productservice.application.command.ListProductsCommand;
import com.example.productservice.application.command.UpdateProductStatusCommand;
import com.example.productservice.application.port.in.ListProductsUseCase;
import com.example.productservice.application.port.in.UpdateProductStatusUseCase;
import com.example.productservice.domain.model.Product;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.UUID;

/**
 * Admin endpoints for product management.
 * No seller-ownership check — admin can change any product's status.
 */
@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ListProductsUseCase listProductsUseCase;
    private final UpdateProductStatusUseCase updateProductStatusUseCase;
    private final ProductWebMapper mapper;

    /**
     * GET /api/v1/admin/products — list all products (paginated, filtered)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> listProducts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sellerId", required = false) UUID sellerId,
            @RequestParam(value = "categoryId", required = false) UUID categoryId
    ) {
        ListProductsCommand command = ListProductsCommand.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .status(status)
                .keyword(keyword)
                .sellerId(sellerId)
                .categoryId(categoryId)
                .build();
        Page<Product> products = listProductsUseCase.listProducts(command);
        Page<ProductResponse> response = products.map(mapper::toResponse);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * PATCH /api/v1/admin/products/{id}/status — admin updates product status
     * (no ownership check; sellerId is null)
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateProductStatusRequest request
    ) {
        UpdateProductStatusCommand command = UpdateProductStatusCommand.builder()
                .productId(id)
                .sellerId(null) // admin — skip ownership check
                .status(request.getStatus())
                .build();
        Product updatedProduct = updateProductStatusUseCase.updateStatus(command);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(updatedProduct)));
    }
}
