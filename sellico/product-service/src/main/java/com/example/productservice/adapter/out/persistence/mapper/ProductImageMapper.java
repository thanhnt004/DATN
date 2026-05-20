package com.example.productservice.adapter.out.persistence.mapper;

import com.example.productservice.adapter.out.persistence.entity.ProductEntity;
import com.example.productservice.adapter.out.persistence.entity.ProductImageEntity;
import com.example.productservice.domain.model.ProductImage;
import org.mapstruct.*;

import java.util.List;

// ProductImageMapper.java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductImageMapper {
    ProductImage toDomain(ProductImageEntity entity);
    ProductImageEntity toEntity(ProductImage productImage);
    List<ProductImageEntity> toEntityList(List<ProductImage> productImages);

    @AfterMapping
    default void setOrder(@MappingTarget List<ProductImageEntity> productImageEntity, List<ProductImage> productImage) {
        for (int i = 0; i < productImageEntity.size(); i++) {
            productImageEntity.get(i).setSortOrder(i + 1);
        }
    }
}