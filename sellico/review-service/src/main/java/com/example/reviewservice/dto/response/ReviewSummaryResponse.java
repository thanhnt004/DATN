package com.example.reviewservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummaryResponse {
    private double ratingAvg;
    private long totalCount;
    private Map<Integer, Long> ratingDistribution; // rating (1-5) -> count
    private long withCommentCount;
    private long withImagesCount;
}
