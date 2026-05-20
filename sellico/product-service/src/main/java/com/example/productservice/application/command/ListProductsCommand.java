package com.example.productservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@Value
public class ListProductsCommand {
    // Pagination
    Integer page;
    Integer size;
    String sortBy;
    String sortDirection;

    // Filters
    UUID categoryId;
    UUID sellerId;
    String status;
    String keyword;
    BigDecimal minPrice;
    BigDecimal maxPrice;
    BigDecimal minRating;
    Boolean hasPromotion;
    String location;

    public int getPageOrDefault() {
        return page != null ? page : 0;
    }

    public int getSizeOrDefault() {
        return size != null ? size : 20;
    }

    public String getSortByOrDefault() {
        return sortBy != null ? sortBy : "createdAt";
    }

    public String getSortDirectionOrDefault() {
        return sortDirection != null ? sortDirection : "desc";
    }
}

