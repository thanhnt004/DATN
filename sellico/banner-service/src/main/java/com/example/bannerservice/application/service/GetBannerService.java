package com.example.bannerservice.application.service;

import com.example.bannerservice.application.port.in.GetBannerUseCase;
import com.example.bannerservice.application.port.out.BannerRepositoryPort;
import com.example.bannerservice.domain.exception.BannerBusinessException;
import com.example.bannerservice.domain.exception.BannerErrorCode;
import com.example.bannerservice.domain.model.Banner;
import com.example.bannerservice.domain.model.BannerStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetBannerService implements GetBannerUseCase {

    private final BannerRepositoryPort bannerRepo;

    @Override
    @Transactional(readOnly = true)
    public Banner getBannerById(UUID id) {
        return bannerRepo.findById(id)
                .orElseThrow(() -> new BannerBusinessException(BannerErrorCode.BANNER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Banner> getActiveBannersByPosition(String positionCode) {
        return bannerRepo.findActiveByPosition(positionCode);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Banner> listBanners(String positionCode, BannerStatus status,
                                    int page, int size, String sortBy, String sortDirection) {
        return bannerRepo.findAllWithFilters(positionCode, status, page, size,
                sortBy != null ? sortBy : "sortOrder",
                sortDirection != null ? sortDirection : "asc");
    }
}

