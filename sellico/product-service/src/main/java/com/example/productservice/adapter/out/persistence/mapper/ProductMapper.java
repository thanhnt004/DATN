package com.example.productservice.adapter.out.persistence.mapper;



import com.example.productservice.adapter.out.persistence.entity.*;
import com.example.productservice.domain.model.Product;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        uses = {ProductImageMapper.class, ProductOptionMapper.class, ProductSkuMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "specifications", source = "spec.attributes")
    Product toDomain(ProductEntity entity);

    @Mapping(target = "spec", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "options", ignore = true)
    @Mapping(target = "skus", ignore = true)
    ProductEntity toEntity(Product domain);

    @AfterMapping
    default void linkChildren(@MappingTarget ProductEntity entity, Product domain) {
        // Link specification
        if (domain.getSpecifications() != null && !domain.getSpecifications().isEmpty()) {
            ProductSpecificationEntity specEntity = new ProductSpecificationEntity();
            specEntity.setAttributes(domain.getSpecifications());
            specEntity.setProduct(entity);
            entity.setSpec(specEntity);
        }

        // Link images
        if (domain.getImages() != null) {
            var imageEntities = domain.getImages().stream()
                    .map(img -> {
                        ProductImageEntity imgEntity = ProductImageEntity.builder()
                                .id(img.getId())
                                .url(img.getUrl())
                                .isPrimary(img.getIsPrimary())
                                .sortOrder(img.getSortOrder())
                                .product(entity)
                                .build();
                        return imgEntity;
                    })
                    .toList();
            entity.setImages(new java.util.ArrayList<>(imageEntities));
        }

        // Link options
        if (domain.getOptions() != null) {
            var optionEntities = domain.getOptions().stream()
                    .map(opt -> {
                        ProductOptionEntity optEntity = ProductOptionEntity.builder()
                                .id(opt.getId())
                                .name(opt.getName())
                                .source(opt.getSource() != null ? opt.getSource() : "SELLER")
                                .product(entity)
                                .sellerId(domain.getSellerId())
                                .values(new java.util.ArrayList<>())
                                .build();
                        if (opt.getValues() != null) {
                            opt.getValues().forEach(v -> {
                                ProductOptionValueEntity valueEntity = ProductOptionValueEntity.builder()
                                        .id(v.getId())
                                        .value(v.getValue())
                                        .imageUrl(v.getImageUrl())
                                        .option(optEntity)
                                        .build();
                                optEntity.getValues().add(valueEntity);
                            });
                        }
                        return optEntity;
                    })
                    .toList();
            entity.setOptions(new java.util.ArrayList<>(optionEntities));
        }

        // Build lookup: domain ProductOptionValue → actual entity (with option reference set)
        java.util.IdentityHashMap<com.example.productservice.domain.model.ProductOptionValue, ProductOptionValueEntity> valueEntityLookup =
                new java.util.IdentityHashMap<>();
        if (domain.getOptions() != null && entity.getOptions() != null) {
            for (int oi = 0; oi < domain.getOptions().size(); oi++) {
                var domOpt = domain.getOptions().get(oi);
                var entOpt = entity.getOptions().get(oi);
                if (domOpt.getValues() != null && entOpt.getValues() != null) {
                    for (int vi = 0; vi < domOpt.getValues().size(); vi++) {
                        valueEntityLookup.put(domOpt.getValues().get(vi), entOpt.getValues().get(vi));
                    }
                }
            }
        }

        // Link SKUs
        if (domain.getSkus() != null) {
            var skuEntities = domain.getSkus().stream()
                    .map(sku -> {
                        ProductSkuEntity skuEntity = ProductSkuEntity.builder()
                                .id(sku.getId())
                                .skuCode(sku.getSkuCode())
                                .price(sku.getPrice())
                                .originalPrice(sku.getOriginalPrice())
                                .costPrice(sku.getCostPrice())
                                .status(sku.getStatus() != null ? sku.getStatus() : "ACTIVE")
                                .weightGram(sku.getWeightGram())
                                .lengthCm(sku.getLengthCm())
                                .widthCm(sku.getWidthCm())
                                .heightCm(sku.getHeightCm())
                                .product(entity)
                                .attributeValues(new java.util.HashSet<>())
                                .build();

                        if (sku.getSelectedValues() != null) {
                            sku.getSelectedValues().forEach(pov -> {
                                ProductOptionValueEntity valueEntity = valueEntityLookup.get(pov);
                                SkuAttributeValue sav = new SkuAttributeValue();
                                sav.setId(SkuAttributeValueId.builder()
                                        .skuId(skuEntity.getId())
                                        .optionValueId(valueEntity != null ? valueEntity.getId() : pov.getId())
                                        .build());
                                sav.setSku(skuEntity);
                                sav.setOptionValue(valueEntity != null ? valueEntity :
                                        ProductOptionValueEntity.builder().id(pov.getId()).build());
                                skuEntity.getAttributeValues().add(sav);
                            });
                        }
                        return skuEntity;
                    })
                    .toList();
            entity.setSkus(new java.util.ArrayList<>(skuEntities));
        }
    }
}
