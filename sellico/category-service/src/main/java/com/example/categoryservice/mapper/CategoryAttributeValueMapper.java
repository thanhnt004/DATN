package com.example.categoryservice.mapper;

import com.example.categoryservice.dto.request.CategoryAttributeValueRequest;
import com.example.categoryservice.dto.response.CategoryAttributeValueResponse;
import com.example.categoryservice.model.CategoryAttribute;
import com.example.categoryservice.model.CategoryAttributeValue;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryAttributeValueMapper {
    CategoryAttributeValue toEntity(CategoryAttributeValueRequest dto);
    List<CategoryAttributeValue> toEntityList(List<CategoryAttributeValueRequest> dtoList);
    CategoryAttributeValueResponse toDto(CategoryAttributeValue entity);
    List<CategoryAttributeValueResponse> toDtoList(List<CategoryAttributeValue> entityList);


    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(CategoryAttributeValueRequest dto, @MappingTarget CategoryAttributeValue entity);
}
