package com.example.categoryservice.controller;

import com.example.categoryservice.model.Category;
import com.example.categoryservice.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Internal API controller for service-to-service communication
 * Base path: /internal/v1/categories
 */
@RestController
@RequestMapping("/internal/v1/categories")
@RequiredArgsConstructor
public class CategoryInternalController {

    private final CategoryRepository categoryRepository;

    /**
     * GET /internal/v1/categories/{id}/exists - Check if category exists
     */
    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> exists(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(categoryRepository.existsById(id));
    }

    /**
     * GET /internal/v1/categories/{id}/is-leaf - Check if category is a leaf node (no children)
     */
    @GetMapping("/{id}/is-leaf")
    public ResponseEntity<Boolean> isLeaf(@PathVariable("id") UUID id) {
        // A leaf category has no children
        boolean hasChildren = categoryRepository.existsByParentId(id);
        return ResponseEntity.ok(!hasChildren);
    }

    /**
     * GET /internal/v1/categories/{id}/descendant-ids
     * Returns this category's ID plus all descendant IDs (entire sub-tree).
     */
    @GetMapping("/{id}/descendant-ids")
    public ResponseEntity<List<String>> getDescendantIds(@PathVariable("id") UUID id) {
        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) {
            return ResponseEntity.ok(List.of(id.toString()));
        }
        List<String> ids = categoryRepository.findDescendantIds(category.getPath())
                .stream()
                .map(UUID::toString)
                .toList();
        return ResponseEntity.ok(ids.isEmpty() ? List.of(id.toString()) : ids);
    }

    /**
     * GET /internal/v1/categories/batch
     * Get multiple categories by their IDs
     */
    @GetMapping("/batch")
    public ResponseEntity<List<com.example.categoryservice.dto.projection.CategoryBasicProjection>> getBatchCategories(@RequestParam("ids") List<UUID> ids) {
        List<com.example.categoryservice.dto.projection.CategoryBasicProjection> categories = categoryRepository.findBasicProjectionsByIds(ids);
        return ResponseEntity.ok(categories);
    }

    /**
     * POST /internal/v1/categories/batch
     * Get multiple categories by their IDs using request body.
     */
    @PostMapping("/batch")
    public ResponseEntity<List<com.example.categoryservice.dto.projection.CategoryBasicProjection>> getBatchCategoriesPost(@RequestBody List<UUID> ids) {
        List<com.example.categoryservice.dto.projection.CategoryBasicProjection> categories = categoryRepository.findBasicProjectionsByIds(ids);
        return ResponseEntity.ok(categories);
    }
}
