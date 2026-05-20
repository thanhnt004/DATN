package com.example.bannerservice.adapter.out.persistence.repository;

import com.example.bannerservice.adapter.out.persistence.entity.BannerPositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BannerPositionJpaRepository extends JpaRepository<BannerPositionEntity, String> {

    List<BannerPositionEntity> findByIsActiveTrue();

    boolean existsByCode(String code);
}

