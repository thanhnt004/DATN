package com.example.bannerservice.application.port.in;

import java.util.UUID;

public interface DeleteBannerUseCase {
    void deleteBanner(UUID bannerId);
}

