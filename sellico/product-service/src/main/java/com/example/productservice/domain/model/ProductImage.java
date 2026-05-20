package com.example.productservice.domain.model;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductImage {
    private UUID id;
    private String url;
    private Boolean isPrimary;
    private Integer sortOrder;
    public static ProductImage create(String url, Boolean isPrimary, Integer sortOrder) {
        return new ProductImage(
                UUID.randomUUID(),
                url,
                isPrimary != null && isPrimary,
                sortOrder != null ? sortOrder : 0
        );
    }
}