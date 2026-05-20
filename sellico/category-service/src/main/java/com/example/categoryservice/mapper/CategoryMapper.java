package com.example.categoryservice.mapper;

import com.example.categoryservice.dto.request.CategoryCreateRequest;
import com.example.categoryservice.dto.request.CategoryUpdateRequest;
import com.example.categoryservice.dto.response.CategoryResponse;
import com.example.categoryservice.model.Category;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {CategoryAttributeMapper.class})
public interface CategoryMapper {

    Category toEntity(CategoryCreateRequest dto);

    CategoryResponse toDto(Category entity);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(CategoryUpdateRequest dto, @MappingTarget Category entity);

}
