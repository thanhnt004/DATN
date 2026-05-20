package com.example.categoryservice.controller;

import com.example.categoryservice.dto.request.CategoryCreateRequest;
import com.example.categoryservice.dto.request.CategoryMoveRequest;
import com.example.categoryservice.dto.request.CategoryUpdateRequest;
import com.example.categoryservice.dto.response.CategoryResponse;
import com.example.categoryservice.dto.response.CategoryTreeResponse;
import com.example.categoryservice.model.Category;
import com.example.categoryservice.repository.CategoryRepository;
import com.example.categoryservice.service.CategoryService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;
    private final CategoryRepository categoryRepository;
    @PostMapping("/api/v1/admin/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@RequestBody CategoryCreateRequest request)
    {
        CategoryResponse categoryResponse = categoryService.createCategory(request);
        return ResponseEntity.ok(ApiResponse.success(categoryResponse));
    }
    @GetMapping("/api/v1/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable("id") UUID id)
    {
        CategoryResponse categoryResponse = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(categoryResponse));
    }
    @GetMapping("/api/v1/categories/tree")
    public ResponseEntity<ApiResponse<List<CategoryTreeResponse>>> getCategoryTree(
            @RequestParam(value = "parentId", required = false) UUID parentId,
            @RequestParam(value = "maxLevel", defaultValue = "3") int maxLevel // Giới hạn độ sâu để tối ưu performance
    ) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getTree(parentId, maxLevel)));
    }
    @DeleteMapping("/api/v1/admin/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable("id") UUID id)
    {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    @PatchMapping("/api/v1/admin/categories/{id}/move")
    public ResponseEntity<ApiResponse<CategoryResponse>> moveCategory(
            @PathVariable("id") UUID id,
            @RequestBody CategoryMoveRequest request
    ) {
        CategoryResponse categoryResponse = categoryService.moveCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success(categoryResponse));
    }
    @PutMapping("/api/v1/admin/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable("id") UUID id,
            @RequestBody CategoryUpdateRequest request
    ) {
        CategoryResponse categoryResponse = categoryService.updateCategory(request, id);
        return ResponseEntity.ok(ApiResponse.success(categoryResponse));
    }

    /**
     * POST /api/v1/admin/categories/normalize-sort-order
     * Re-assigns sequential sortOrder (0, 1, 2, ...) to all categories
     * grouped by parent. Useful when data is imported with all sortOrder = 0.
     */
    @PostMapping("/api/v1/admin/categories/normalize-sort-order")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> normalizeSortOrder() {
        List<Category> all = categoryRepository.findAll();

        // Group by parentId (null = root)
        Map<UUID, List<Category>> grouped = all.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getParent() == null ? new UUID(0, 0) : c.getParent().getId()
                ));

        int totalUpdated = 0;
        for (var entry : grouped.entrySet()) {
            List<Category> siblings = entry.getValue();
            // Sort by name as a stable ordering when all sortOrders are the same
            siblings.sort(Comparator.comparing(Category::getSortOrder)
                    .thenComparing(Category::getName));
            for (int i = 0; i < siblings.size(); i++) {
                if (siblings.get(i).getSortOrder() != i) {
                    siblings.get(i).setSortOrder(i);
                    totalUpdated++;
                }
            }
        }
        categoryRepository.saveAll(all);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "totalUpdated", totalUpdated,
                "message", "Normalized sortOrder for all categories"
        )));
    }
}
