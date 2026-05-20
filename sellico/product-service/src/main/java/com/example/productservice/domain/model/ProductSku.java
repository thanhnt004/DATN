package com.example.productservice.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductSku {
    private UUID id;
    private String skuCode;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private BigDecimal costPrice;
    private String status;
    private Integer weightGram;
    private Integer lengthCm;
    private Integer widthCm;
    private Integer heightCm;

    // Product context (populated for internal/batch queries)
    private UUID productId;
    private String productName;
    private UUID sellerId;
    private String primaryImageUrl; // product primary image (fallback for SKU image)

    // Thay vì SkuAttributeValue phức tạp, ở Domain có thể dùng List các Value trực tiếp
    private List<ProductOptionValue> selectedValues;


}