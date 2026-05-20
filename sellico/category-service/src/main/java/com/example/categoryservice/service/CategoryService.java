package com.example.categoryservice.service;

import com.example.categoryservice.dto.request.CategoryCreateRequest;
import com.example.categoryservice.dto.request.CategoryMoveRequest;
import com.example.categoryservice.dto.request.CategoryUpdateRequest;
import com.example.categoryservice.dto.response.CategoryResponse;
import com.example.categoryservice.dto.response.CategoryTreeResponse;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;
import java.util.UUID;


public interface CategoryService {
    CategoryResponse createCategory(CategoryCreateRequest request);
    List<CategoryTreeResponse> getTree(UUID parentId, Integer depth);
    CategoryResponse updateCategory(CategoryUpdateRequest updateCategoryRequest, UUID id) ;

    CategoryResponse getCategoryById(UUID id);
    CategoryResponse moveCategory(UUID id, CategoryMoveRequest request);
    void deleteCategory(UUID id);
}
