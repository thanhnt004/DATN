package com.example.reviewservice.controller;

import com.example.reviewservice.dto.request.CreateReplyRequest;
import com.example.reviewservice.dto.response.ReplyResponse;
import com.example.reviewservice.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seller/reviews")
@RequiredArgsConstructor
public class SellerReviewController {

    private final ReviewService reviewService;

    /**
     * POST /api/v1/seller/reviews/{reviewId}/reply — Reply to a review (seller)
     */
    @PostMapping("/{reviewId}/reply")
    public ResponseEntity<ApiResponse<ReplyResponse>> createReply(
            @PathVariable("reviewId") UUID reviewId,
            @Valid @RequestBody CreateReplyRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID sellerId = UUID.fromString(jwt.getSubject());
        ReplyResponse response = reviewService.createReply(sellerId, reviewId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * PUT /api/v1/seller/reviews/replies/{replyId} — Update a reply
     */
    @PutMapping("/replies/{replyId}")
    public ResponseEntity<ApiResponse<ReplyResponse>> updateReply(
            @PathVariable("replyId") UUID replyId,
            @Valid @RequestBody CreateReplyRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID sellerId = UUID.fromString(jwt.getSubject());
        ReplyResponse response = reviewService.updateReply(sellerId, replyId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * DELETE /api/v1/seller/reviews/replies/{replyId} — Delete a reply
     */
    @DeleteMapping("/replies/{replyId}")
    public ResponseEntity<ApiResponse<Void>> deleteReply(
            @PathVariable("replyId") UUID replyId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID sellerId = UUID.fromString(jwt.getSubject());
        reviewService.deleteReply(sellerId, replyId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
