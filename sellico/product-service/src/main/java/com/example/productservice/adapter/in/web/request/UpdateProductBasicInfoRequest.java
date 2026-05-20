package com.example.productservice.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for updating basic product information
 * Endpoint: PATCH /v1/seller/products/{id}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductBasicInfoRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 10, max = 255, message = "Product name must be between 10 and 255 characters")
    private String name;

    @Size(max = 200, message = "Slug must not exceed 200 characters")
    private String slug;

    @Size(max = 10000, message = "Description must not exceed 10000 characters")
    private String description;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;
}

