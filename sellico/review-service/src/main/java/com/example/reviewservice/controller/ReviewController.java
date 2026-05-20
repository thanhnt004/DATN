package com.example.reviewservice.controller;

import com.example.reviewservice.dto.request.CreateReviewRequest;
import com.example.reviewservice.dto.request.UpdateReviewRequest;
import com.example.reviewservice.dto.response.ReviewResponse;
import com.example.reviewservice.dto.response.ReviewSummaryResponse;
import com.example.reviewservice.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * POST /api/v1/reviews — Create a review (buyer, authenticated)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        ReviewResponse response = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * PUT /api/v1/reviews/{id} — Update own review
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateReviewRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        ReviewResponse response = reviewService.updateReview(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * DELETE /api/v1/reviews/{id} — Delete own review
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        reviewService.deleteReview(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * GET /api/v1/reviews/product/{productId} — Get product reviews (public)
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductReviews(
            @PathVariable("productId") UUID productId,
            @RequestParam(value = "rating", required = false) Integer rating,
            @RequestParam(value = "hasComment", required = false) Boolean hasComment,
            @RequestParam(value = "hasImages", required = false) Boolean hasImages,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection
    ) {
        Map<String, Object> result = reviewService.getProductReviews(
                productId, rating, hasComment, hasImages, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * GET /api/v1/reviews/product/{productId}/summary — Get rating summary (public)
     */
    @GetMapping("/product/{productId}/summary")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> getReviewSummary(
            @PathVariable("productId") UUID productId
    ) {
        ReviewSummaryResponse summary = reviewService.getReviewSummary(productId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * GET /api/v1/reviews/my — Get current user's reviews
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyReviews(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Map<String, Object> result = reviewService.getUserReviews(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * GET /api/v1/reviews/order/{orderId} — Get reviews for an order
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getOrderReviews(
            @PathVariable("orderId") UUID orderId
    ) {
        List<ReviewResponse> reviews = reviewService.getOrderReviews(orderId);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    /**
     * GET /api/v1/reviews/check — Check if user has reviewed a product for an order
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Boolean>> hasUserReviewed(
            @RequestParam("productId") UUID productId,
            @RequestParam("orderId") UUID orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        boolean hasReviewed = reviewService.hasUserReviewed(userId, productId, orderId);
        return ResponseEntity.ok(ApiResponse.success(hasReviewed));
    }
}
