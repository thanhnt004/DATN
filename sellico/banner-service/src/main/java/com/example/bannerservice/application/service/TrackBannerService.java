package com.example.bannerservice.application.service;

import com.example.bannerservice.application.port.in.TrackBannerUseCase;
import com.example.bannerservice.application.port.out.BannerRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrackBannerService implements TrackBannerUseCase {

    private final BannerRepositoryPort bannerRepo;

    @Override
    @Transactional
    public void trackClick(UUID bannerId) {
        bannerRepo.incrementClickCount(bannerId);
    }

    @Override
    @Transactional
    public void trackView(UUID bannerId) {
        bannerRepo.incrementViewCount(bannerId);
    }
}

