package com.example.sellerservice.repository;

import com.example.sellerservice.entity.SellerFollower;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerFollowerRepository extends JpaRepository<SellerFollower, UUID> {

    Optional<SellerFollower> findBySellerIdAndUserId(UUID sellerId, UUID userId);

    boolean existsBySellerIdAndUserId(UUID sellerId, UUID userId);

    List<SellerFollower> findAllBySellerId(UUID sellerId);

    Page<SellerFollower> findAllBySellerId(UUID sellerId, Pageable pageable);

    List<SellerFollower> findAllByUserId(UUID userId);

    Page<SellerFollower> findAllByUserId(UUID userId, Pageable pageable);

    long countBySellerId(UUID sellerId);

    void deleteBySellerIdAndUserId(UUID sellerId, UUID userId);

    @Query("SELECT sf.sellerId FROM SellerFollower sf WHERE sf.userId = :userId")
    List<UUID> findSellerIdsByUserId(@Param("userId") UUID userId);
}

