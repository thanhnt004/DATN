package com.example.bannerservice.application.service;

import com.example.bannerservice.application.command.CreateBannerCommand;
import com.example.bannerservice.application.port.in.CreateBannerUseCase;
import com.example.bannerservice.application.port.out.BannerPositionRepositoryPort;
import com.example.bannerservice.application.port.out.BannerRepositoryPort;
import com.example.bannerservice.domain.exception.BannerBusinessException;
import com.example.bannerservice.domain.exception.BannerErrorCode;
import com.example.bannerservice.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateBannerService implements CreateBannerUseCase {

    private final BannerRepositoryPort bannerRepo;
    private final BannerPositionRepositoryPort positionRepo;

    @Override
    @Transactional
    public Banner createBanner(CreateBannerCommand cmd) {
        // Validate position exists and is active
        BannerPosition position = positionRepo.findByCode(cmd.getPositionCode())
                .orElseThrow(() -> new BannerBusinessException(BannerErrorCode.POSITION_NOT_FOUND));
        if (!Boolean.TRUE.equals(position.getIsActive())) {
            throw new BannerBusinessException(BannerErrorCode.POSITION_NOT_ACTIVE);
        }

        // Check max banners for position
        int activeCount = bannerRepo.countActiveByPosition(cmd.getPositionCode());
        if (activeCount >= position.getMaxBanners()) {
            throw new BannerBusinessException(BannerErrorCode.POSITION_MAX_BANNERS_REACHED);
        }

        // Parse enums
        LinkType linkType = parseEnum(LinkType.class, cmd.getLinkType(), BannerErrorCode.INVALID_LINK_TYPE);
        BannerStatus status = cmd.getStatus() != null
                ? parseEnum(BannerStatus.class, cmd.getStatus(), BannerErrorCode.INVALID_STATUS)
                : BannerStatus.DRAFT;
        TargetAudience audience = cmd.getTargetAudience() != null
                ? parseEnum(TargetAudience.class, cmd.getTargetAudience(), BannerErrorCode.INVALID_STATUS)
                : TargetAudience.ALL;

        // Validate scheduled
        if (status == BannerStatus.SCHEDULED) {
            if (cmd.getStartDate() == null || cmd.getEndDate() == null) {
                throw new BannerBusinessException(BannerErrorCode.SCHEDULE_DATES_REQUIRED);
            }
        }
        if (cmd.getStartDate() != null && cmd.getEndDate() != null && !cmd.getEndDate().isAfter(cmd.getStartDate())) {
            throw new BannerBusinessException(BannerErrorCode.INVALID_DATE_RANGE);
        }

        Instant now = Instant.now();
        Banner banner = Banner.builder()
                .id(UUID.randomUUID())
                .title(cmd.getTitle())
                .imageUrl(cmd.getImageUrl())
                .linkUrl(cmd.getLinkUrl())
                .linkType(linkType)
                .linkValue(cmd.getLinkValue())
                .positionCode(cmd.getPositionCode())
                .sortOrder(cmd.getSortOrder() != null ? cmd.getSortOrder() : 0)
                .status(status)
                .startDate(cmd.getStartDate())
                .endDate(cmd.getEndDate())
                .targetAudience(audience)
                .clickCount(0L)
                .viewCount(0L)
                .createdBy(cmd.getCreatedBy())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return bannerRepo.save(banner);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, BannerErrorCode errorCode) {
        if (value == null) return null;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BannerBusinessException(errorCode);
        }
    }
}

