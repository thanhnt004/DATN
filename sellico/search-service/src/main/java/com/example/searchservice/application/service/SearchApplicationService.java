package com.example.searchservice.application.service;

import com.example.searchservice.application.dto.CategoryFacetResponse;
import com.example.searchservice.application.dto.SearchRequest;
import com.example.searchservice.application.dto.SearchResponse;
import com.example.searchservice.application.mapper.ProductSearchMapper;
import com.example.searchservice.application.port.in.IndexProductUseCase;
import com.example.searchservice.application.port.in.SearchUseCase;
import com.example.searchservice.application.port.out.SearchPort;
import com.example.searchservice.domain.event.ProductEvent;
import com.example.searchservice.domain.model.ProductDocument;
import com.example.searchservice.infrastructure.client.CategoryFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchApplicationService implements SearchUseCase, IndexProductUseCase {

    private final SearchPort searchPort;
    private final ProductSearchMapper mapper;
    private final CategoryFeignClient categoryFeignClient;

    @Override
    public Page<SearchResponse> search(SearchRequest request) {
        log.debug("Searching products with keyword='{}', page={}, size={}",
                request.getKeyword(), request.getPage(), request.getSize());

        expandCategoryDescendants(request);

        Page<ProductDocument> results = searchPort.search(request);
        return results.map(mapper::toResponse);
    }

    @Override
    public List<String> suggest(String keyword, int size) {
        log.debug("Suggesting for keyword='{}'", keyword);
        return searchPort.suggest(keyword, size);
    }

    @Override
    public List<CategoryFacetResponse> relatedCategories(SearchRequest request, int size) {
        // Keep category list independent from selected category filter
        request.setCategoryId(null);
        request.setCategoryIds(null);
        return searchPort.relatedCategories(request, size);
    }

    private void expandCategoryDescendants(SearchRequest request) {
        if (request.getCategoryId() == null || request.getCategoryId().isBlank()) {
            return;
        }
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            return;
        }

        try {
            List<String> descendantIds = categoryFeignClient.getDescendantIds(request.getCategoryId());
            if (descendantIds != null && descendantIds.size() > 1) {
                log.debug("Expanded categoryId={} to {} descendant IDs", request.getCategoryId(), descendantIds.size());
                request.setCategoryIds(descendantIds);
                request.setCategoryId(null);
            }
        } catch (Exception e) {
            log.warn("Failed to resolve descendant categories for {}: {}", request.getCategoryId(), e.getMessage());
        }
    }

    @Override
    public void handleProductEvent(ProductEvent event) {
        log.info("Handling product event: type={}, productId={}", event.getEventType(), event.getProductId());

        switch (event.getEventType()) {
            case PRODUCT_CREATED, PRODUCT_UPDATED, PRODUCT_ACTIVATED -> {
                ProductDocument document = mapper.toDocument(event);
                searchPort.save(document);
                log.info("Indexed product: {}", event.getProductId());
            }
            case PRODUCT_DELETED, PRODUCT_DEACTIVATED -> {
                searchPort.deleteById(event.getProductId());
                log.info("Removed product from index: {}", event.getProductId());
            }
            default -> log.warn("Unknown event type: {}", event.getEventType());
        }
    }
}
