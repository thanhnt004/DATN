package com.example.productservice.adapter.out.persistence.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkuAttributeValueId implements Serializable {

    private UUID skuId;

    private UUID optionValueId;
}
