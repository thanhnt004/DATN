package com.example.searchservice.adapter.in.web;

import com.example.searchservice.application.dto.CategoryFacetResponse;
import com.example.searchservice.application.dto.SearchRequest;
import com.example.searchservice.application.dto.SearchResponse;
import com.example.searchservice.application.port.in.SearchUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import response.ApiResponse;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchUseCase searchUseCase;

    /**
     * Full-text search with filters.
     *
     * GET /api/v1/search/products?keyword=iphone&categoryId=...&minPrice=100&maxPrice=2000&sortBy=price_asc&page=0&size=20
     */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<Page<SearchResponse>>> searchProducts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "categoryIds", required = false) String categoryIds,
            @RequestParam(value = "sellerId", required = false) String sellerId,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "minRating", required = false) BigDecimal minRating,
            @RequestParam(value = "status", required = false, defaultValue = "ACTIVE") String status,
            @RequestParam(value = "sortBy", required = false, defaultValue = "relevance") String sortBy,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        SearchRequest request = SearchRequest.builder()
                .keyword(keyword)
                .categoryId(categoryId)
                .categoryIds(parseCategoryIds(categoryIds))
                .sellerId(sellerId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .minRating(minRating)
                .status(status)
                .sortBy(sortBy)
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(ApiResponse.success(searchUseCase.search(request)));
    }

    /**
     * Related categories for current search keyword/filter context.
     */
    @GetMapping("/products/related-categories")
    public ResponseEntity<ApiResponse<List<CategoryFacetResponse>>> relatedCategories(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sellerId", required = false) String sellerId,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "minRating", required = false) BigDecimal minRating,
            @RequestParam(value = "status", required = false, defaultValue = "ACTIVE") String status,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        SearchRequest request = SearchRequest.builder()
                .keyword(keyword)
                .sellerId(sellerId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .minRating(minRating)
                .status(status)
                .page(0)
                .size(500)
                .build();

        return ResponseEntity.ok(ApiResponse.success(searchUseCase.relatedCategories(request, size)));
    }

    /**
     * Auto-complete / search suggestion.
     *
     * GET /api/v1/search/suggest?keyword=iph&size=5
     */
    @GetMapping("/suggest")
    public ResponseEntity<ApiResponse<List<String>>> suggest(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "size", defaultValue = "5") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(searchUseCase.suggest(keyword, size)));
    }

    private List<String> parseCategoryIds(String categoryIds) {
        if (categoryIds == null || categoryIds.isBlank()) {
            return null;
        }
        return Arrays.stream(categoryIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
