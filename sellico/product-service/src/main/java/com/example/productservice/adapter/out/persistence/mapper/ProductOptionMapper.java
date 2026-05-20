package com.example.productservice.adapter.out.persistence.mapper;

import com.example.productservice.adapter.out.persistence.entity.ProductOptionEntity;
import com.example.productservice.adapter.out.persistence.entity.ProductOptionValueEntity;
import com.example.productservice.domain.model.ProductOption;
import com.example.productservice.domain.model.ProductOptionValue;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductOptionMapper {
    ProductOption toDomain(ProductOptionEntity entity);

    @Mapping(target = "optionId", source = "option.id")
    ProductOptionValue toDomainValue(ProductOptionValueEntity entity);

    @Mapping(target = "values", ignore = true)
    ProductOptionEntity toEntity(ProductOption productOption);
    @AfterMapping
    default void linkValues(@MappingTarget ProductOptionEntity entity, ProductOption productOption) {
        if (productOption.getValues() != null) {
            for (ProductOptionValue value : productOption.getValues()) {
                ProductOptionValueEntity valueEntity = new ProductOptionValueEntity();
                valueEntity.setId(value.getId());
                valueEntity.setImageUrl(value.getImageUrl());
                entity.addValue(valueEntity);
            }
        }
    }
    List<ProductOptionEntity> toEntityList(List<ProductOption> productOptions);

}