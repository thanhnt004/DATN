package com.example.bannerservice.application.service;

import com.example.bannerservice.application.port.in.DeleteBannerUseCase;
import com.example.bannerservice.application.port.out.BannerRepositoryPort;
import com.example.bannerservice.domain.exception.BannerBusinessException;
import com.example.bannerservice.domain.exception.BannerErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteBannerService implements DeleteBannerUseCase {

    private final BannerRepositoryPort bannerRepo;

    @Override
    @Transactional
    public void deleteBanner(UUID bannerId) {
        bannerRepo.findById(bannerId)
                .orElseThrow(() -> new BannerBusinessException(BannerErrorCode.BANNER_NOT_FOUND));
        bannerRepo.deleteById(bannerId);
    }
}

