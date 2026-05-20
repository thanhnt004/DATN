package com.example.searchservice.adapter.out.elasticsearch;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.json.JsonData;
import com.example.searchservice.application.dto.CategoryFacetResponse;
import com.example.searchservice.application.dto.SearchRequest;
import com.example.searchservice.application.port.out.SearchPort;
import com.example.searchservice.domain.model.ProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchSearchAdapter implements SearchPort {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductElasticsearchRepository repository;

    @Override
    public ProductDocument save(ProductDocument document) {
        return repository.save(document);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<ProductDocument> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public Page<ProductDocument> search(SearchRequest request) {
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize());

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(buildQuery(request, true))
                .withSort(buildSort(request.getSortBy()))
                .withPageable(pageRequest)
                .build();

        SearchHits<ProductDocument> hits = elasticsearchOperations.search(nativeQuery, ProductDocument.class);

        List<ProductDocument> content = hits.getSearchHits()
                .stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageRequest, hits.getTotalHits());
    }

    @Override
    public List<String> suggest(String keyword, int size) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(Query.of(q -> q.multiMatch(mm -> mm
                        .query(keyword)
                        .fields("name.suggest", "name.suggest._2gram", "name.suggest._3gram")
                        .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BoolPrefix)
                )))
                .withPageable(PageRequest.of(0, size))
                .build();

        SearchHits<ProductDocument> hits = elasticsearchOperations.search(nativeQuery, ProductDocument.class);

        return hits.getSearchHits().stream()
                .map(hit -> hit.getContent().getName())
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryFacetResponse> relatedCategories(SearchRequest request, int size) {
        int fetchSize = Math.max(200, size * 20);
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(buildQuery(request, false))
                .withPageable(PageRequest.of(0, fetchSize))
                .build();

        SearchHits<ProductDocument> hits = elasticsearchOperations.search(nativeQuery, ProductDocument.class);

        Map<String, Long> counter = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(ProductDocument::getCategoryId)
                .filter(categoryId -> categoryId != null && !categoryId.isBlank())
                .collect(Collectors.groupingBy(categoryId -> categoryId, LinkedHashMap::new, Collectors.counting()));

        return counter.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(size)
                .map(entry -> CategoryFacetResponse.builder()
                        .categoryId(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .toList();
    }

    private Query buildQuery(SearchRequest request, boolean includeCategoryFilter) {
        List<Query> mustQueries = new ArrayList<>();
        List<Query> filterQueries = new ArrayList<>();

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = request.getKeyword().trim();

            // Use bool query with should clauses for better relevance
            Query searchQuery = Query.of(q -> q.bool(b -> b
                .should(List.of(
                    // Exact match on keyword field - highest priority
                    Query.of(sq -> sq.term(t -> t
                        .field("name.keyword")
                        .value(keyword)
                        .boost(20.0f)
                    )),
                    // Exact phrase match - very high priority
                    Query.of(sq -> sq.matchPhrase(mp -> mp
                        .field("name")
                        .query(keyword)
                        .boost(15.0f)
                    )),
                    // Autocomplete (Edge Ngram) match
                    Query.of(sq -> sq.match(m -> m
                        .field("name.autocomplete")
                        .query(keyword)
                        .boost(5.0f)
                    )),
                    // Phrase match in description
                    Query.of(sq -> sq.matchPhrase(mp -> mp
                        .field("description")
                        .query(keyword)
                        .boost(2.0f)
                    )),
                    // Multi-match with cross_fields for better multi-word matching
                    Query.of(sq -> sq.multiMatch(mm -> mm
                        .query(keyword)
                        .fields("name^10", "description^2")
                        .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.CrossFields)
                        .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
                        .boost(8.0f)
                    )),
                    // Fuzzy match as fallback
                    Query.of(sq -> sq.multiMatch(mm -> mm
                        .query(keyword)
                        .fields("name^2", "description")
                        .fuzziness("1")
                        .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.Or)
                        .boost(1.0f)
                    ))
                ))
                .minimumShouldMatch("70%")
            ));
            mustQueries.add(searchQuery);
        }

        filterQueries.add(Query.of(q -> q
                .term(t -> t.field("isDeleted").value(false))
        ));

        String status = request.getStatus() != null ? request.getStatus() : "ACTIVE";
        filterQueries.add(Query.of(q -> q
                .term(t -> t.field("status").value(status))
        ));

        if (includeCategoryFilter) {
            if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
                List<co.elastic.clients.elasticsearch._types.FieldValue> catValues = request.getCategoryIds().stream()
                        .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                        .toList();
                filterQueries.add(Query.of(q -> q
                        .terms(t -> t.field("categoryId").terms(tv -> tv.value(catValues)))
                ));
            } else if (request.getCategoryId() != null && !request.getCategoryId().isBlank()) {
                filterQueries.add(Query.of(q -> q
                        .term(t -> t.field("categoryId").value(request.getCategoryId()))
                ));
            }
        }

        if (request.getSellerId() != null && !request.getSellerId().isBlank()) {
            filterQueries.add(Query.of(q -> q
                    .term(t -> t.field("sellerId").value(request.getSellerId()))
            ));
        }

        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            filterQueries.add(Query.of(q -> q
                    .range(r -> {
                        RangeQuery.Builder rangeBuilder = r.field("minPrice");
                        if (request.getMinPrice() != null) {
                            rangeBuilder.gte(JsonData.of(request.getMinPrice()));
                        }
                        if (request.getMaxPrice() != null) {
                            rangeBuilder.lte(JsonData.of(request.getMaxPrice()));
                        }
                        return rangeBuilder;
                    })
            ));
        }

        if (request.getMinRating() != null) {
            filterQueries.add(Query.of(q -> q
                    .range(r -> r
                            .field("ratingAvg")
                            .gte(JsonData.of(request.getMinRating()))
                    )
            ));
        }

        Query baseQuery = Query.of(q -> q.bool(b -> {
            if (!mustQueries.isEmpty()) {
                b.must(mustQueries);
            } else {
                b.must(Query.of(mq -> mq.matchAll(ma -> ma)));
            }
            b.filter(filterQueries);
            return b;
        }));

        // Apply Function Score to boost popular and high-rated products
        return Query.of(q -> q.functionScore(fs -> fs
                .query(baseQuery)
                .functions(List.of(
                        // Boost by soldCount (logarithmic scale)
                        co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore.of(f -> f
                                .fieldValueFactor(fv -> fv
                                        .field("soldCount")
                                        .factor(0.1)
                                        .modifier(co.elastic.clients.elasticsearch._types.query_dsl.FieldValueFactorModifier.Ln1p)
                                        .missing(0.0)
                                )
                        ),
                        // Boost by ratingAvg
                        co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore.of(f -> f
                                .fieldValueFactor(fv -> fv
                                        .field("ratingAvg")
                                        .factor(1.2)
                                        .missing(1.0)
                                )
                        )
                ))
                .scoreMode(FunctionScoreMode.Multiply)
                .boostMode(FunctionBoostMode.Multiply)
        ));
    }

    private List<SortOptions> buildSort(String sortBy) {
        if (sortBy == null) sortBy = "relevance";
        return switch (sortBy) {
            case "price_asc" -> List.of(SortOptions.of(s -> s
                    .field(f -> f.field("minPrice").order(SortOrder.Asc))));
            case "price_desc" -> List.of(SortOptions.of(s -> s
                    .field(f -> f.field("minPrice").order(SortOrder.Desc))));
            case "sold_desc", "best_selling" -> List.of(SortOptions.of(s -> s
                    .field(f -> f.field("soldCount").order(SortOrder.Desc))));
            case "rating_desc" -> List.of(SortOptions.of(s -> s
                    .field(f -> f.field("ratingAvg").order(SortOrder.Desc))));
            case "newest" -> List.of(SortOptions.of(s -> s
                    .field(f -> f.field("createdAt").order(SortOrder.Desc))));
            default -> List.of(SortOptions.of(s -> s
                    .score(sc -> sc)));
        };
    }
}
