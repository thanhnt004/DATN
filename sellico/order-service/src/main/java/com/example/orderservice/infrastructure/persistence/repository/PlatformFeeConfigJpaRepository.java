package com.example.orderservice.infrastructure.persistence.repository;

import com.example.orderservice.infrastructure.persistence.entity.PlatformFeeConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformFeeConfigJpaRepository extends JpaRepository<PlatformFeeConfigJpaEntity, Long> {
    
    @Query("SELECT p FROM PlatformFeeConfigJpaEntity p ORDER BY p.updatedAt DESC LIMIT 1")
    Optional<PlatformFeeConfigJpaEntity> findLatestConfig();
}
