package com.example.reviewservice.infrastructure.persistence.repository;

import com.example.reviewservice.infrastructure.persistence.entity.ReviewReplyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewReplyRepository extends JpaRepository<ReviewReplyEntity, UUID> {
    Optional<ReviewReplyEntity> findByReviewId(UUID reviewId);
    boolean existsByReviewId(UUID reviewId);
}
