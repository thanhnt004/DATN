package com.example.bannerservice.application.port.out;

import com.example.bannerservice.domain.model.BannerPosition;

import java.util.List;
import java.util.Optional;

public interface BannerPositionRepositoryPort {

    BannerPosition save(BannerPosition position);

    Optional<BannerPosition> findByCode(String code);

    boolean existsByCode(String code);

    List<BannerPosition> findAll();

    List<BannerPosition> findAllActive();

    void deleteByCode(String code);
}

