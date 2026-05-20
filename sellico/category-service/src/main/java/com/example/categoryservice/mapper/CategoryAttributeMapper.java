package com.example.categoryservice.mapper;

import com.example.categoryservice.dto.request.CategoryAttributeRequest;
import com.example.categoryservice.dto.response.CategoryAttributeResponse;
import com.example.categoryservice.model.Category;
import com.example.categoryservice.model.CategoryAttribute;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CategoryAttributeValueMapper.class})
public interface CategoryAttributeMapper {
    CategoryAttribute toEntity(CategoryAttributeRequest dto);
    List<CategoryAttribute> toEntityList(List<CategoryAttributeRequest> dtoList);

    @Mapping(source = "name", target = "value")
    @Mapping(source = "isRequired", target = "required")
    @Mapping(source = "isFilterable", target = "filterable")
    CategoryAttributeResponse toDto(CategoryAttribute entity);
    List<CategoryAttributeResponse> toDtoList(List<CategoryAttribute> entityList);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(CategoryAttributeRequest dto, @MappingTarget CategoryAttribute entity);

    @AfterMapping
    default void setSortOrders(@MappingTarget CategoryAttribute target) {
       if (target.getPredefinedValues()!=null) {
           for (int i = 0; i < target.getPredefinedValues().size(); i++) {
               target.getPredefinedValues().get(i).setSortOrder(i + 1);
           }
       }
    }
}
