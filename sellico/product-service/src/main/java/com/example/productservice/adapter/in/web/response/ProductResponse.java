package com.example.productservice.adapter.in.web.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import model.SpecAttribute;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private UUID id;
    private UUID sellerId;
    private UUID categoryId;
    private String name;
    private String slug;
    private String description;
    private String status; // DRAFT, PENDING, ACTIVE, BANNED, DELETED

    // --- Các trường thống kê từ Schema ---
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private Integer soldCount;

    // --- Các trường tính toán khoảng giá ---
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isDeleted;

    // --- Quan hệ dữ liệu ---
    private List<ImageResponse> images;
    private List<OptionResponse> options;
    private List<SkuResponse> skus;
    private List<SpecAttribute> specifications;

    @Data
    @Builder
    public static class ImageResponse {
        private UUID id;
        private String url;
        private Boolean isPrimary;
        private Integer sortOrder;
    }

    @Data
    @Builder
    public static class OptionResponse {
        private UUID id;
        private UUID sellerId; // NULL nếu là ADMIN
        private String source; // ADMIN hoặc SELLER
        private String name;
        private List<OptionValueResponse> values;
    }

    @Data
    @Builder
    public static class OptionValueResponse {
        private UUID id;
        private String value;
        private String imageUrl;
    }

    @Data
    @Builder
    public static class SkuResponse {
        private UUID id;
        private String skuCode;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private BigDecimal costPrice;
        private String status; // ACTIVE, OUT_OF_STOCK, DISABLED

        // Kích thước vật lý (từ bảng product_skus)
        private Integer weightGram;
        private Integer lengthCm;
        private Integer widthCm;
        private Integer heightCm;

        // Metadata: Danh sách thuộc tính cụ thể của SKU này
        // Ví dụ: [{"optionName": "Màu sắc", "value": "Đỏ"}]
        private List<SkuAttributeResponse> attributes;
    }

    @Data
    @Builder
    public static class SkuAttributeResponse {
        private String optionName;
        private String valueName;
        private UUID optionValueId;
    }
}