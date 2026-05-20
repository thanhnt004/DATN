package com.example.bannerservice.application.port.in;

import java.util.UUID;

public interface TrackBannerUseCase {
    void trackClick(UUID bannerId);
    void trackView(UUID bannerId);
}

