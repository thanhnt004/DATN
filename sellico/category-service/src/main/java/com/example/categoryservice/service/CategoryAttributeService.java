package com.example.categoryservice.service;

import com.example.categoryservice.dto.request.CategoryAttributeRequest;
import com.example.categoryservice.dto.response.CategoryAttributeResponse;
import com.example.categoryservice.model.CategoryAttribute;

import java.util.List;
import java.util.UUID;

public interface CategoryAttributeService {
    List<CategoryAttributeResponse> getAttributesByCategoryId(UUID categoryId);
    CategoryAttributeResponse createCategoryAttribute(UUID categoryId, CategoryAttributeRequest request);
    CategoryAttributeResponse updateCategoryAttribute(UUID attributeId, CategoryAttributeRequest request);
    void deleteCategoryAttribute(UUID attributeId);
}
