package com.example.productservice.adapter.in.web.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import model.SpecAttribute;

import java.util.List;

/**
 * Request DTO for updating product specifications
 * Endpoint: PUT /v1/seller/products/{id}/specifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductSpecificationsRequest {

    @NotNull(message = "Specifications cannot be null")
    private List<SpecAttribute> specifications;
}

