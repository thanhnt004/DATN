package com.example.bannerservice.application.service;

import com.example.bannerservice.application.command.ReorderBannersCommand;
import com.example.bannerservice.application.port.in.ReorderBannersUseCase;
import com.example.bannerservice.application.port.out.BannerRepositoryPort;
import com.example.bannerservice.domain.exception.BannerBusinessException;
import com.example.bannerservice.domain.exception.BannerErrorCode;
import com.example.bannerservice.domain.model.Banner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ReorderBannersService implements ReorderBannersUseCase {

    private final BannerRepositoryPort bannerRepo;

    @Override
    @Transactional
    public void reorderBanners(ReorderBannersCommand command) {
        for (ReorderBannersCommand.BannerOrder order : command.getBannerOrders()) {
            Banner banner = bannerRepo.findById(order.getBannerId())
                    .orElseThrow(() -> new BannerBusinessException(BannerErrorCode.BANNER_NOT_FOUND));
            banner.setSortOrder(order.getSortOrder());
            banner.setUpdatedAt(Instant.now());
            bannerRepo.save(banner);
        }
    }
}

