package com.example.bannerservice.application.service;

import com.example.bannerservice.application.port.out.BannerRepositoryPort;
import com.example.bannerservice.domain.model.Banner;
import com.example.bannerservice.domain.model.BannerStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled service to auto-activate SCHEDULED banners and auto-expire ended banners.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BannerSchedulerService {

    private final BannerRepositoryPort bannerRepo;

    /**
     * Every minute: activate scheduled banners whose start_date has passed
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void activateScheduledBanners() {
        List<Banner> scheduled = bannerRepo.findScheduledBannersToActivate();
        for (Banner banner : scheduled) {
            banner.setStatus(BannerStatus.ACTIVE);
            banner.setUpdatedAt(Instant.now());
            bannerRepo.save(banner);
            log.info("Auto-activated banner: id={}, title={}", banner.getId(), banner.getTitle());
        }
    }

    /**
     * Every minute: expire active banners whose end_date has passed
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireBanners() {
        List<Banner> expired = bannerRepo.findExpiredBanners();
        for (Banner banner : expired) {
            banner.setStatus(BannerStatus.EXPIRED);
            banner.setUpdatedAt(Instant.now());
            bannerRepo.save(banner);
            log.info("Auto-expired banner: id={}, title={}", banner.getId(), banner.getTitle());
        }
    }
}

