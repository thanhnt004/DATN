package com.example.categoryservice.controller;

import com.example.categoryservice.dto.request.CategoryAttributeRequest;
import com.example.categoryservice.dto.response.CategoryAttributeResponse;
import com.example.categoryservice.model.CategoryAttribute;
import com.example.categoryservice.service.CategoryAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CategoryAttributeController {
    private final CategoryAttributeService categoryAttributeService;
    @GetMapping("/api/v1/categories/{id}/attributes")
    public ResponseEntity<ApiResponse<List<CategoryAttributeResponse>>> getCategoryAttribute(@PathVariable("id")UUID categoryId){
        List<CategoryAttributeResponse> attributes = categoryAttributeService.getAttributesByCategoryId(categoryId);
        return ResponseEntity.ok(ApiResponse.success(attributes));
    }
    @PostMapping("/api/v1/admin/categories/{categoryId}/attributes")
    public ResponseEntity<ApiResponse<CategoryAttributeResponse>> createCategoryAttribute(
            @PathVariable("categoryId") UUID categoryId,
            @RequestBody CategoryAttributeRequest request) {
        CategoryAttributeResponse result = categoryAttributeService.createCategoryAttribute(categoryId, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/api/v1/admin/categories/attributes/{id}")
    public ResponseEntity<ApiResponse<CategoryAttributeResponse>> updateCategoryAttribute(@PathVariable("id") UUID attributeId, @RequestBody CategoryAttributeRequest request){
        CategoryAttributeResponse result = categoryAttributeService.updateCategoryAttribute(attributeId,request);
        return ResponseEntity.ok(ApiResponse.success(result));}
    @DeleteMapping("/api/v1/admin/categories/attributes/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategoryAttribute(@PathVariable("id") UUID attributeId){
        categoryAttributeService.deleteCategoryAttribute(attributeId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

