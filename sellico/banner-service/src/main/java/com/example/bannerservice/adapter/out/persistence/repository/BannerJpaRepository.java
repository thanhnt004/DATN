package com.example.bannerservice.adapter.out.persistence.repository;

import com.example.bannerservice.adapter.out.persistence.entity.BannerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BannerJpaRepository extends JpaRepository<BannerEntity, UUID> {

    @Query("""
        SELECT b FROM BannerEntity b
        WHERE (:positionCode IS NULL OR b.positionCode = :positionCode)
          AND (:status IS NULL OR b.status = :status)
    """)
    Page<BannerEntity> findAllWithFilters(
            @Param("positionCode") String positionCode,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
        SELECT b FROM BannerEntity b
        WHERE b.positionCode = :positionCode
          AND b.status = 'ACTIVE'
          AND (b.startDate IS NULL OR b.startDate <= :now)
          AND (b.endDate IS NULL OR b.endDate > :now)
        ORDER BY b.sortOrder ASC
    """)
    List<BannerEntity> findActiveByPosition(
            @Param("positionCode") String positionCode,
            @Param("now") Instant now
    );

    @Query("SELECT COUNT(b) FROM BannerEntity b WHERE b.positionCode = :positionCode AND b.status IN ('ACTIVE','SCHEDULED')")
    int countActiveByPosition(@Param("positionCode") String positionCode);

    @Modifying
    @Query("UPDATE BannerEntity b SET b.clickCount = b.clickCount + 1 WHERE b.id = :id")
    void incrementClickCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE BannerEntity b SET b.viewCount = b.viewCount + 1 WHERE b.id = :id")
    void incrementViewCount(@Param("id") UUID id);

    @Query("SELECT b FROM BannerEntity b WHERE b.status = 'SCHEDULED' AND b.startDate <= :now")
    List<BannerEntity> findScheduledToActivate(@Param("now") Instant now);

    @Query("SELECT b FROM BannerEntity b WHERE b.status = 'ACTIVE' AND b.endDate IS NOT NULL AND b.endDate <= :now")
    List<BannerEntity> findExpired(@Param("now") Instant now);
}

