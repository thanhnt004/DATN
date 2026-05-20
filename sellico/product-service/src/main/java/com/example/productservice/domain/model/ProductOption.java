package com.example.productservice.domain.model;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductOption {
    private UUID id;
    private String name;
    private String source; // ADMIN / SELLER
    private List<ProductOptionValue> values;
}

