package com.example.searchservice.application.port.out;

import com.example.searchservice.application.dto.CategoryFacetResponse;
import com.example.searchservice.application.dto.SearchRequest;
import com.example.searchservice.domain.model.ProductDocument;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface SearchPort {

    /**
     * Save or update a product document.
     */
    ProductDocument save(ProductDocument document);

    /**
     * Delete a product document by ID.
     */
    void deleteById(String id);

    /**
     * Full-text search with filters.
     */
    Page<ProductDocument> search(SearchRequest request);

    /**
     * Auto-complete suggestions.
     */
    List<String> suggest(String keyword, int size);

    /**
     * Category facets related to current search context.
     */
    List<CategoryFacetResponse> relatedCategories(SearchRequest request, int size);

    /**
     * Find a product document by ID.
     */
    Optional<ProductDocument> findById(String id);
}
