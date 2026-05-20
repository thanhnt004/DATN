package com.example.reviewservice.service;

import com.example.reviewservice.dto.request.CreateReplyRequest;
import com.example.reviewservice.dto.request.CreateReviewRequest;
import com.example.reviewservice.dto.request.UpdateReviewRequest;
import com.example.reviewservice.dto.response.ReplyResponse;
import com.example.reviewservice.dto.response.ReviewResponse;
import com.example.reviewservice.dto.response.ReviewSummaryResponse;
import com.example.reviewservice.exception.ReviewErrorCode;
import com.example.reviewservice.exception.ReviewException;
import com.example.reviewservice.infrastructure.client.OrderClient;
import com.example.reviewservice.infrastructure.client.ProductClient;
import com.example.reviewservice.infrastructure.client.UserClient;
import com.example.reviewservice.infrastructure.persistence.entity.ReviewEntity;
import com.example.reviewservice.infrastructure.persistence.entity.ReviewReplyEntity;
import com.example.reviewservice.infrastructure.persistence.repository.ReviewReplyRepository;
import com.example.reviewservice.infrastructure.persistence.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewReplyRepository reviewReplyRepository;
    private final OrderClient orderClient;
    private final ProductClient productClient;
    private final UserClient userClient;

    // ═══════════════════════════════════════════════════════════════════
    // CREATE REVIEW
    // ═══════════════════════════════════════════════════════════════════

    @Transactional
    public ReviewResponse createReview(UUID userId, CreateReviewRequest request) {
        log.info("Creating review for product {} by user {}", request.getProductId(), userId);

        // 1. Check duplicate
        if (reviewRepository.existsByUserIdAndProductIdAndOrderId(
                userId, request.getProductId(), request.getOrderId())) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }

        // 2. Validate order belongs to user
        try {
            var belongsRes = orderClient.checkOrderBelongsToUser(request.getOrderId(), userId);
            if (belongsRes == null || belongsRes.getData() == null || !belongsRes.getData()) {
                throw new ReviewException(ReviewErrorCode.ORDER_NOT_BELONGS_TO_USER);
            }
        } catch (ReviewException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to verify order ownership", e);
            throw new ReviewException(ReviewErrorCode.ORDER_NOT_BELONGS_TO_USER);
        }

        // 3. Validate order is completed
        try {
            var completedRes = orderClient.checkOrderIsCompleted(request.getOrderId());
            if (completedRes == null || completedRes.getData() == null || !completedRes.getData()) {
                throw new ReviewException(ReviewErrorCode.ORDER_NOT_COMPLETED);
            }
        } catch (ReviewException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to verify order completion", e);
            throw new ReviewException(ReviewErrorCode.ORDER_NOT_COMPLETED);
        }

        // 4. Validate order has product
        try {
            var hasProductRes = orderClient.checkOrderHasProduct(request.getOrderId(), request.getProductId());
            if (hasProductRes == null || hasProductRes.getData() == null || !hasProductRes.getData()) {
                throw new ReviewException(ReviewErrorCode.ORDER_NOT_HAS_PRODUCT);
            }
        } catch (ReviewException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to verify order-product", e);
            throw new ReviewException(ReviewErrorCode.ORDER_NOT_HAS_PRODUCT);
        }

        // 5. Create and save
        ReviewEntity entity = new ReviewEntity();
        entity.setId(UUID.randomUUID());
        entity.setProductId(request.getProductId());
        entity.setUserId(userId);
        entity.setOrderId(request.getOrderId());
        entity.setSkuId(request.getSkuId());
        entity.setRating(request.getRating());
        entity.setComment(request.getComment());
        entity.setImages(request.getImages() != null ? new ArrayList<>(request.getImages()) : null);
        entity.setAnonymous(request.isAnonymous());

        entity = reviewRepository.save(entity);
        log.info("Review created: {}", entity.getId());

        // 6. Recalculate product rating
        recalculateProductRating(request.getProductId());

        return toResponse(entity, userId);
    }

    // ═══════════════════════════════════════════════════════════════════
    // UPDATE REVIEW
    // ═══════════════════════════════════════════════════════════════════

    @Transactional
    public ReviewResponse updateReview(UUID userId, UUID reviewId, UpdateReviewRequest request) {
        ReviewEntity entity = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.REVIEW_NOT_FOUND));

        if (!entity.getUserId().equals(userId)) {
            throw new ReviewException(ReviewErrorCode.NOT_REVIEW_OWNER);
        }

        if (request.getRating() != null) entity.setRating(request.getRating());
        if (request.getComment() != null) entity.setComment(request.getComment());
        if (request.getImages() != null) entity.setImages(new ArrayList<>(request.getImages()));
        if (request.getIsAnonymous() != null) entity.setAnonymous(request.getIsAnonymous());

        entity = reviewRepository.save(entity);
        log.info("Review updated: {}", entity.getId());

        recalculateProductRating(entity.getProductId());

        return toResponse(entity, userId);
    }

    // ═══════════════════════════════════════════════════════════════════
    // DELETE REVIEW
    // ═══════════════════════════════════════════════════════════════════

    @Transactional
    public void deleteReview(UUID userId, UUID reviewId) {
        ReviewEntity entity = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.REVIEW_NOT_FOUND));

        if (!entity.getUserId().equals(userId)) {
            throw new ReviewException(ReviewErrorCode.NOT_REVIEW_OWNER);
        }

        UUID productId = entity.getProductId();
        reviewRepository.delete(entity);
        log.info("Review deleted: {}", reviewId);

        recalculateProductRating(productId);
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET PRODUCT REVIEWS (public)
    // ═══════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Map<String, Object> getProductReviews(UUID productId, Integer rating,
                                                  Boolean hasComment, Boolean hasImages,
                                                  int page, int size,
                                                  String sortBy, String sortDirection) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<ReviewEntity> reviewPage;

        if (rating != null) {
            reviewPage = reviewRepository.findByProductIdAndRating(productId, rating, pageable);
        } else if (Boolean.TRUE.equals(hasComment)) {
            reviewPage = reviewRepository.findByProductIdWithComment(productId, pageable);
        } else if (Boolean.TRUE.equals(hasImages)) {
            reviewPage = reviewRepository.findByProductIdWithImages(productId, pageable);
        } else {
            reviewPage = reviewRepository.findByProductId(productId, pageable);
        }

        List<ReviewResponse> reviews = enrichWithUserInfo(reviewPage.getContent());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", reviews);
        result.put("page", reviewPage.getNumber());
        result.put("size", reviewPage.getSize());
        result.put("totalElements", reviewPage.getTotalElements());
        result.put("totalPages", reviewPage.getTotalPages());
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET REVIEW SUMMARY (public)
    // ═══════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getReviewSummary(UUID productId) {
        Double avgRating = reviewRepository.avgRatingByProductId(productId);
        List<Object[]> distribution = reviewRepository.countByProductIdGroupByRating(productId);

        Map<Integer, Long> ratingDistribution = new LinkedHashMap<>();
        long totalCount = 0;
        for (int i = 5; i >= 1; i--) {
            ratingDistribution.put(i, 0L);
        }
        for (Object[] row : distribution) {
            Integer star = (Integer) row[0];
            Long count = (Long) row[1];
            ratingDistribution.put(star, count);
            totalCount += count;
        }

        // Count with comment
        long withComment = reviewRepository.findByProductIdWithComment(
                productId, PageRequest.of(0, 1)).getTotalElements();

        // Count with images
        long withImages = reviewRepository.findByProductIdWithImages(
                productId, PageRequest.of(0, 1)).getTotalElements();

        return new ReviewSummaryResponse(
                avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0,
                totalCount,
                ratingDistribution,
                withComment,
                withImages
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET USER REVIEWS
    // ═══════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Map<String, Object> getUserReviews(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReviewEntity> reviewPage = reviewRepository.findByUserId(userId, pageable);

        List<ReviewResponse> reviews = enrichWithUserInfo(reviewPage.getContent());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", reviews);
        result.put("page", reviewPage.getNumber());
        result.put("size", reviewPage.getSize());
        result.put("totalElements", reviewPage.getTotalElements());
        result.put("totalPages", reviewPage.getTotalPages());
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET ORDER REVIEWS
    // ═══════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<ReviewResponse> getOrderReviews(UUID orderId) {
        List<ReviewEntity> entities = reviewRepository.findByOrderId(orderId);
        return enrichWithUserInfo(entities);
    }

    // ═══════════════════════════════════════════════════════════════════
    // CHECK IF USER HAS REVIEWED
    // ═══════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public boolean hasUserReviewed(UUID userId, UUID productId, UUID orderId) {
        return reviewRepository.existsByUserIdAndProductIdAndOrderId(userId, productId, orderId);
    }

    // ═══════════════════════════════════════════════════════════════════
    // SELLER: REPLY TO REVIEW
    // ═══════════════════════════════════════════════════════════════════

    @Transactional
    public ReplyResponse createReply(UUID sellerId, UUID reviewId, CreateReplyRequest request) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.REVIEW_NOT_FOUND));

        if (reviewReplyRepository.existsByReviewId(reviewId)) {
            throw new ReviewException(ReviewErrorCode.REPLY_ALREADY_EXISTS);
        }

        ReviewReplyEntity reply = new ReviewReplyEntity();
        reply.setId(UUID.randomUUID());
        reply.setReview(review);
        reply.setSellerId(sellerId);
        reply.setComment(request.getComment());

        reply = reviewReplyRepository.save(reply);
        log.info("Reply created for review {}: {}", reviewId, reply.getId());

        return toReplyResponse(reply);
    }

    @Transactional
    public ReplyResponse updateReply(UUID sellerId, UUID replyId, CreateReplyRequest request) {
        ReviewReplyEntity reply = reviewReplyRepository.findById(replyId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.REPLY_NOT_FOUND));

        if (!reply.getSellerId().equals(sellerId)) {
            throw new ReviewException(ReviewErrorCode.NOT_SELLER_OF_PRODUCT);
        }

        reply.setComment(request.getComment());
        reply = reviewReplyRepository.save(reply);
        log.info("Reply updated: {}", replyId);

        return toReplyResponse(reply);
    }

    @Transactional
    public void deleteReply(UUID sellerId, UUID replyId) {
        ReviewReplyEntity reply = reviewReplyRepository.findById(replyId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.REPLY_NOT_FOUND));

        if (!reply.getSellerId().equals(sellerId)) {
            throw new ReviewException(ReviewErrorCode.NOT_SELLER_OF_PRODUCT);
        }

        reviewReplyRepository.delete(reply);
        log.info("Reply deleted: {}", replyId);
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════

    private void recalculateProductRating(UUID productId) {
        try {
            Double avgRating = reviewRepository.avgRatingByProductId(productId);
            long count = reviewRepository.countByProductId(productId);

            BigDecimal avg = avgRating != null
                    ? BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            productClient.updateProductRating(productId,
                    new ProductClient.UpdateProductRatingRequest(avg, (int) count));

            log.info("Product {} rating updated: avg={}, count={}", productId, avg, count);
        } catch (Exception e) {
            log.error("Failed to update product rating for product {}", productId, e);
        }
    }

    private List<ReviewResponse> enrichWithUserInfo(List<ReviewEntity> entities) {
        if (entities.isEmpty()) return List.of();

        // Collect non-anonymous user IDs
        Set<UUID> userIds = entities.stream()
                .filter(e -> !e.isAnonymous())
                .map(ReviewEntity::getUserId)
                .collect(Collectors.toSet());

        // Also collect seller IDs from replies
        entities.stream()
                .filter(e -> e.getReply() != null)
                .map(e -> e.getReply().getSellerId())
                .forEach(userIds::add);

        Map<UUID, UserClient.UserPublicInfo> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            try {
                var res = userClient.getUsersByIds(new ArrayList<>(userIds));
                if (res != null && res.getData() != null) {
                    userMap = res.getData().stream()
                            .collect(Collectors.toMap(UserClient.UserPublicInfo::getId, Function.identity(),
                                    (a, b) -> a));
                }
            } catch (Exception e) {
                log.error("Failed to fetch user info", e);
            }
        }

        Map<UUID, UserClient.UserPublicInfo> finalUserMap = userMap;
        return entities.stream()
                .map(entity -> toResponseWithUserInfo(entity, finalUserMap))
                .collect(Collectors.toList());
    }

    private ReviewResponse toResponse(ReviewEntity entity, UUID currentUserId) {
        ReviewResponse response = new ReviewResponse();
        response.setId(entity.getId());
        response.setProductId(entity.getProductId());
        response.setUserId(entity.getUserId());
        response.setOrderId(entity.getOrderId());
        response.setSkuId(entity.getSkuId());
        response.setRating(entity.getRating());
        response.setComment(entity.getComment());
        response.setImages(entity.getImages() != null ? new ArrayList<>(entity.getImages()) : List.of());
        response.setAnonymous(entity.isAnonymous());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        if (entity.isAnonymous()) {
            response.setUserName("Ẩn danh");
            response.setUserAvatar(null);
        }

        if (entity.getReply() != null) {
            response.setReply(toReplyResponse(entity.getReply()));
        }

        return response;
    }

    private ReviewResponse toResponseWithUserInfo(ReviewEntity entity,
                                                   Map<UUID, UserClient.UserPublicInfo> userMap) {
        ReviewResponse response = new ReviewResponse();
        response.setId(entity.getId());
        response.setProductId(entity.getProductId());
        response.setUserId(entity.getUserId());
        response.setOrderId(entity.getOrderId());
        response.setSkuId(entity.getSkuId());
        response.setRating(entity.getRating());
        response.setComment(entity.getComment());
        response.setImages(entity.getImages() != null ? new ArrayList<>(entity.getImages()) : List.of());
        response.setAnonymous(entity.isAnonymous());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        if (entity.isAnonymous()) {
            response.setUserName("Ẩn danh");
            response.setUserAvatar(null);
        } else {
            UserClient.UserPublicInfo user = userMap.get(entity.getUserId());
            if (user != null) {
                response.setUserName(user.getFullName() != null ? user.getFullName() : user.getUsername());
                response.setUserAvatar(user.getAvatarUrl());
            }
        }

        if (entity.getReply() != null) {
            response.setReply(toReplyResponse(entity.getReply()));
        }

        return response;
    }

    private ReplyResponse toReplyResponse(ReviewReplyEntity entity) {
        ReplyResponse response = new ReplyResponse();
        response.setId(entity.getId());
        response.setSellerId(entity.getSellerId());
        response.setComment(entity.getComment());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
