package com.example.productservice.domain.model;

import lombok.*;

import java.util.UUID;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductOptionValue {
    private UUID id;
    private String value;
    private String imageUrl;
    private UUID optionId;
    private String optionName;  // e.g. "Color", "Size"
    public ProductOptionValue(String value)
    {
        this.value = value;
    }
}
