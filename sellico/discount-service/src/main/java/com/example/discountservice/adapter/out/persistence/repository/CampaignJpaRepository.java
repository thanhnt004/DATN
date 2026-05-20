package com.example.discountservice.adapter.out.persistence.repository;

import com.example.discountservice.adapter.out.persistence.entity.CampaignEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

public interface CampaignJpaRepository extends JpaRepository<CampaignEntity, UUID> {

    @Query("SELECT c FROM CampaignEntity c WHERE (:sellerId IS NULL OR c.sellerId = :sellerId) AND (:status IS NULL OR c.status = :status) ORDER BY c.createdAt DESC")
    Page<CampaignEntity> search(@Param("sellerId") UUID sellerId, @Param("status") String status, Pageable pageable);
}

