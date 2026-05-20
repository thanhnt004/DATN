package com.example.searchservice.application.mapper;

import com.example.searchservice.application.dto.SearchResponse;
import com.example.searchservice.domain.event.ProductEvent;
import com.example.searchservice.domain.model.ProductDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductSearchMapper {

    @Mapping(target = "id", source = "productId")
    ProductDocument toDocument(ProductEvent event);

    SearchResponse toResponse(ProductDocument document);
}
