package com.example.orderservice.application.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Page response DTO matching Spring Page<T> JSON format.
 * Frontend expects: content, totalElements, totalPages, size, number, first, last, empty
 */
@Data
@Builder
public class OrderPageResponse<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int size;
    private int number; // current page (0-indexed)
    private boolean first;
    private boolean last;
    private boolean empty;

    public static <T> OrderPageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return OrderPageResponse.<T>builder()
                .content(content)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .size(size)
                .number(page)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .empty(content == null || content.isEmpty())
                .build();
    }
}
