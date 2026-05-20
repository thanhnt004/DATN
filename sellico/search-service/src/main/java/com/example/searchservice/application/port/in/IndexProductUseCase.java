package com.example.searchservice.application.port.in;

import com.example.searchservice.domain.event.ProductEvent;

public interface IndexProductUseCase {

    /**
     * Index or update a product document in Elasticsearch.
     */
    void handleProductEvent(ProductEvent event);
}
