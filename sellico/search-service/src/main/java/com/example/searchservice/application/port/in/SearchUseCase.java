package com.example.searchservice.application.port.in;

import com.example.searchservice.application.dto.CategoryFacetResponse;
import com.example.searchservice.application.dto.SearchRequest;
import com.example.searchservice.application.dto.SearchResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface SearchUseCase {

    /**
     * Full-text search products with filters and pagination.
     */
    Page<SearchResponse> search(SearchRequest request);

    /**
     * Get search suggestions for auto-complete.
     */
    List<String> suggest(String keyword, int size);

    /**
     * Category facets related to current search context.
     */
    List<CategoryFacetResponse> relatedCategories(SearchRequest request, int size);
}
