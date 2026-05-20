package com.example.bannerservice.application.port.in;

import com.example.bannerservice.domain.model.Banner;
import com.example.bannerservice.domain.model.BannerStatus;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface GetBannerUseCase {
    Banner getBannerById(UUID id);
    List<Banner> getActiveBannersByPosition(String positionCode);
    Page<Banner> listBanners(String positionCode, BannerStatus status, int page, int size, String sortBy, String sortDirection);
}

