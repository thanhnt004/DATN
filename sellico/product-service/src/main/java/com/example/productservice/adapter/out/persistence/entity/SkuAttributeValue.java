package com.example.productservice.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sku_attribute_values")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkuAttributeValue {

    @EmbeddedId
    private SkuAttributeValueId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("skuId")
    @JoinColumn(name = "sku_id")
    private ProductSkuEntity sku;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("optionValueId")
    @JoinColumn(name = "option_value_id")
    private ProductOptionValueEntity optionValue;
}
