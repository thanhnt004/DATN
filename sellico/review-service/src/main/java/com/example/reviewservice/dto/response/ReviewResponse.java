package com.example.reviewservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private UUID id;
    private UUID productId;
    private UUID userId;
    private UUID orderId;
    private UUID skuId;
    private Integer rating;
    private String comment;
    private List<String> images;
    private boolean anonymous;
    private Instant createdAt;
    private Instant updatedAt;

    // User info (populated via Feign)
    private String userName;
    private String userAvatar;

    // Reply
    private ReplyResponse reply;
}
