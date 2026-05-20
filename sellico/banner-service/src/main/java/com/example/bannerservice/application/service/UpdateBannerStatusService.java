package com.example.bannerservice.application.service;

import com.example.bannerservice.application.command.UpdateBannerStatusCommand;
import com.example.bannerservice.application.port.in.UpdateBannerStatusUseCase;
import com.example.bannerservice.application.port.out.BannerRepositoryPort;
import com.example.bannerservice.domain.exception.BannerBusinessException;
import com.example.bannerservice.domain.exception.BannerErrorCode;
import com.example.bannerservice.domain.model.Banner;
import com.example.bannerservice.domain.model.BannerStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UpdateBannerStatusService implements UpdateBannerStatusUseCase {

    private static final Map<BannerStatus, Set<BannerStatus>> TRANSITIONS = Map.of(
            BannerStatus.DRAFT,     Set.of(BannerStatus.ACTIVE, BannerStatus.SCHEDULED, BannerStatus.INACTIVE),
            BannerStatus.ACTIVE,    Set.of(BannerStatus.INACTIVE, BannerStatus.EXPIRED),
            BannerStatus.INACTIVE,  Set.of(BannerStatus.ACTIVE, BannerStatus.SCHEDULED, BannerStatus.DRAFT),
            BannerStatus.SCHEDULED, Set.of(BannerStatus.ACTIVE, BannerStatus.INACTIVE, BannerStatus.DRAFT),
            BannerStatus.EXPIRED,   Set.of(BannerStatus.DRAFT, BannerStatus.INACTIVE, BannerStatus.ACTIVE, BannerStatus.SCHEDULED)
    );

    private final BannerRepositoryPort bannerRepo;

    @Override
    @Transactional
    public Banner updateStatus(UpdateBannerStatusCommand command) {
        Banner banner = bannerRepo.findById(command.getBannerId())
                .orElseThrow(() -> new BannerBusinessException(BannerErrorCode.BANNER_NOT_FOUND));

        BannerStatus newStatus;
        try {
            newStatus = BannerStatus.valueOf(command.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BannerBusinessException(BannerErrorCode.INVALID_STATUS);
        }

        Set<BannerStatus> allowed = TRANSITIONS.getOrDefault(banner.getStatus(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw new BannerBusinessException(BannerErrorCode.INVALID_STATUS_TRANSITION);
        }

        if (newStatus == BannerStatus.SCHEDULED || newStatus == BannerStatus.ACTIVE) {
            if (banner.getStartDate() == null || banner.getEndDate() == null) {
                throw new BannerBusinessException(BannerErrorCode.SCHEDULE_DATES_REQUIRED);
            }
            if (banner.isExpired() && newStatus == BannerStatus.ACTIVE) {
                 throw new BannerBusinessException(BannerErrorCode.INVALID_DATE_RANGE);
            }
        }

        banner.setStatus(newStatus);
        banner.setUpdatedAt(Instant.now());
        return bannerRepo.save(banner);
    }
}

