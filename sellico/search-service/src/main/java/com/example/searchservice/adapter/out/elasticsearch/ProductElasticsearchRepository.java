package com.example.searchservice.adapter.out.elasticsearch;

import com.example.searchservice.domain.model.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ProductElasticsearchRepository extends ElasticsearchRepository<ProductDocument, String> {

    List<ProductDocument> findByNameContainingIgnoreCaseAndStatusAndIsDeletedFalse(
            String name, String status);

    List<ProductDocument> findByCategoryIdAndStatusAndIsDeletedFalse(
            String categoryId, String status);
}
