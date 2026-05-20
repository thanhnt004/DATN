package com.example.bannerservice.application.service;

import com.example.bannerservice.application.command.UpdateBannerCommand;
import com.example.bannerservice.application.port.in.UpdateBannerUseCase;
import com.example.bannerservice.application.port.out.BannerPositionRepositoryPort;
import com.example.bannerservice.application.port.out.BannerRepositoryPort;
import com.example.bannerservice.domain.exception.BannerBusinessException;
import com.example.bannerservice.domain.exception.BannerErrorCode;
import com.example.bannerservice.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UpdateBannerService implements UpdateBannerUseCase {

    private final BannerRepositoryPort bannerRepo;
    private final BannerPositionRepositoryPort positionRepo;

    @Override
    @Transactional
    public Banner updateBanner(UpdateBannerCommand cmd) {
        Banner banner = bannerRepo.findById(cmd.getBannerId())
                .orElseThrow(() -> new BannerBusinessException(BannerErrorCode.BANNER_NOT_FOUND));

        if (cmd.getTitle() != null) banner.setTitle(cmd.getTitle());
        if (cmd.getImageUrl() != null) banner.setImageUrl(cmd.getImageUrl());
        if (cmd.getLinkUrl() != null) banner.setLinkUrl(cmd.getLinkUrl());

        if (cmd.getLinkType() != null) {
            try {
                banner.setLinkType(LinkType.valueOf(cmd.getLinkType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BannerBusinessException(BannerErrorCode.INVALID_LINK_TYPE);
            }
        }
        if (cmd.getLinkValue() != null) banner.setLinkValue(cmd.getLinkValue());

        if (cmd.getPositionCode() != null) {
            positionRepo.findByCode(cmd.getPositionCode())
                    .orElseThrow(() -> new BannerBusinessException(BannerErrorCode.POSITION_NOT_FOUND));
            banner.setPositionCode(cmd.getPositionCode());
        }

        if (cmd.getSortOrder() != null) banner.setSortOrder(cmd.getSortOrder());
        if (cmd.getStartDate() != null) banner.setStartDate(cmd.getStartDate());
        if (cmd.getEndDate() != null) banner.setEndDate(cmd.getEndDate());

        if (banner.getStartDate() != null && banner.getEndDate() != null
                && !banner.getEndDate().isAfter(banner.getStartDate())) {
            throw new BannerBusinessException(BannerErrorCode.INVALID_DATE_RANGE);
        }

        // If the banner was EXPIRED and the end date is updated to the future,
        // move it to SCHEDULED so it can be picked up by the scheduler
        if (banner.getStatus() == BannerStatus.EXPIRED && banner.getEndDate().isAfter(Instant.now())) {
            banner.setStatus(BannerStatus.SCHEDULED);
        }

        if (cmd.getTargetAudience() != null) {
            try {
                banner.setTargetAudience(TargetAudience.valueOf(cmd.getTargetAudience().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BannerBusinessException(BannerErrorCode.INVALID_STATUS);
            }
        }

        banner.setUpdatedAt(Instant.now());
        return bannerRepo.save(banner);
    }
}

