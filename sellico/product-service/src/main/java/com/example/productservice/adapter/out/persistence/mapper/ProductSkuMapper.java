package com.example.productservice.adapter.out.persistence.mapper;

import com.example.productservice.adapter.out.persistence.entity.ProductImageEntity;
import com.example.productservice.adapter.out.persistence.entity.ProductOptionValueEntity;
import com.example.productservice.adapter.out.persistence.entity.ProductSkuEntity;
import com.example.productservice.adapter.out.persistence.entity.SkuAttributeValue;
import com.example.productservice.domain.model.ProductOptionValue;
import com.example.productservice.domain.model.ProductSku;
import org.mapstruct.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductSkuMapper {

    @Mapping(target = "selectedValues", source = "attributeValues")
    ProductSku toDomain(ProductSkuEntity entity);

    default ProductOptionValue map(SkuAttributeValue sav) {
        return ProductOptionValue.builder()
                .id(sav.getOptionValue().getId())
                .value(sav.getOptionValue().getValue())
                .imageUrl(sav.getOptionValue().getImageUrl())
                .optionId(sav.getOptionValue().getOption().getId())
                .optionName(sav.getOptionValue().getOption().getName())
                .build();
    }

    // Chuyển đổi từ Set<SkuAttributeValue> sang List<ProductOptionValue>
    default List<ProductOptionValue> mapAttributeValues(Set<SkuAttributeValue> values) {
        if (values == null) return null;
        return values.stream()
                .map(attr -> ProductOptionValue.builder()
                        .id(attr.getOptionValue().getId())
                        .value(attr.getOptionValue().getValue())
                        .imageUrl(attr.getOptionValue().getImageUrl())
                        .optionId(attr.getOptionValue().getOption().getId())
                        .optionName(attr.getOptionValue().getOption().getName())
                        .build())
                .collect(Collectors.toList());
    }

    @Mapping(target = "attributeValues", ignore = true) // xử lý tay ở AfterMapping
    @Mapping(target = "product", ignore = true)
    ProductSkuEntity toEntity(ProductSku domain);

    /**
     * Populate product context fields on the domain model from the JPA entity.
     * This runs for all toDomain() calls — when loaded via batch SKU queries
     * the product relationship is lazily loaded.
     */
    @AfterMapping
    default void populateProductContext(ProductSkuEntity entity,
                                        @MappingTarget ProductSku domain) {
        if (entity.getProduct() != null) {
            domain.setProductId(entity.getProduct().getId());
            domain.setProductName(entity.getProduct().getName());
            domain.setSellerId(entity.getProduct().getSellerId());
            // Resolve primary image URL from product images
            if (entity.getProduct().getImages() != null && !entity.getProduct().getImages().isEmpty()) {
                String primary = entity.getProduct().getImages().stream()
                        .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                        .map(ProductImageEntity::getUrl)
                        .findFirst()
                        .orElse(null);
                if (primary == null) {
                    primary = entity.getProduct().getImages().stream()
                            .sorted(java.util.Comparator.comparingInt(img -> img.getSortOrder() != null ? img.getSortOrder() : Integer.MAX_VALUE))
                            .map(ProductImageEntity::getUrl)
                            .findFirst()
                            .orElse(null);
                }
                domain.setPrimaryImageUrl(primary);
            }
        }
    }

    // map List<ProductOptionValue> -> Set<SkuAttributeValue>
    @AfterMapping
    default void mapAttributeValues(ProductSku domain,
                                    @MappingTarget ProductSkuEntity entity) {

        if (domain.getSelectedValues() == null) return;

        Set<SkuAttributeValue> set = new HashSet<>();

        for (ProductOptionValue pov : domain.getSelectedValues()) {
            SkuAttributeValue sav = new SkuAttributeValue();

            // set owning side
            sav.setSku(entity);

            // chỉ set ID → không overwrite bảng option_value
            ProductOptionValueEntity ref = ProductOptionValueEntity.builder()
                    .id(pov.getId())
                    .build();

            sav.setOptionValue(ref);
            set.add(sav);
        }

        entity.setAttributeValues(set);
    }

}
