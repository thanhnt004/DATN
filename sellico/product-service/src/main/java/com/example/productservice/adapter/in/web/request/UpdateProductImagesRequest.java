package com.example.productservice.adapter.in.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.util.List;

/**
 * Request DTO for updating product images
 * Endpoint: PUT /v1/seller/products/{id}/images
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductImagesRequest {

    @NotEmpty(message = "At least one image is required")
    @Valid
    private List<ImageItem> images;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageItem {
        @NotBlank(message = "Image URL is required")
        @URL(message = "Invalid image URL format")
        private String url;

        @Builder.Default
        private Boolean isPrimary = false;

        @Builder.Default
        private Integer sortOrder = 0;
    }
}

