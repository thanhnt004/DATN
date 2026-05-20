package com.example.productservice.adapter.in.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import model.SpecAttribute;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProductRequest {

    @NotBlank
    private String name;

    @NotNull
    private UUID sellerId;

    @NotNull
    private UUID categoryId;

    private String description;

    @NotEmpty
    private List<@Valid ImageRequest> images;

    @NotEmpty
    private List<@Valid OptionRequest> options;

    @NotEmpty
    private List<@Valid SkuRequest> skus;

    private List<SpecAttribute> specifications;

    @Data
    public static class ImageRequest {
        @NotBlank
        private String url;

        private Boolean isPrimary = false;

        private Integer sortOrder = 0;
    }

    @Data
    public static class OptionRequest {
        @NotBlank
        private String name;

        @NotEmpty
        private List<@Valid OptionValueRequest> values;
    }

    @Data
    public static class OptionValueRequest {
        @NotBlank
        private String value;

        private String imageUrl;
    }

    @Data
    public static class SkuRequest {
        @NotBlank
        private String skuCode;

        @NotNull
        @PositiveOrZero
        private BigDecimal price;

        @PositiveOrZero
        private BigDecimal originalPrice;

        @PositiveOrZero
        private BigDecimal costPrice;

        @Positive
        private Integer weightGram;

        @PositiveOrZero
        private Integer lengthCm;

        @PositiveOrZero
        private Integer widthCm;

        @PositiveOrZero
        private Integer heightCm;

        @NotEmpty
        private Map<String, String> selectionAttributes;

        // Inventory fields (optional, synced to inventory-service)
        @PositiveOrZero
        private Integer totalStock;

        @PositiveOrZero
        private Integer lowStockThreshold;

        private String locationCode;
    }
}
