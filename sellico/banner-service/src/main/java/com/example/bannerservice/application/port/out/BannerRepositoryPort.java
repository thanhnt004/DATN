package com.example.bannerservice.application.port.out;

import com.example.bannerservice.domain.model.Banner;
import com.example.bannerservice.domain.model.BannerStatus;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BannerRepositoryPort {

    Banner save(Banner banner);

    Optional<Banner> findById(UUID id);

    void deleteById(UUID id);

    Page<Banner> findAllWithFilters(String positionCode, BannerStatus status, int page, int size, String sortBy, String sortDirection);

    List<Banner> findActiveByPosition(String positionCode);

    int countActiveByPosition(String positionCode);

    void incrementClickCount(UUID id);

    void incrementViewCount(UUID id);

    List<Banner> findScheduledBannersToActivate();

    List<Banner> findExpiredBanners();
}

