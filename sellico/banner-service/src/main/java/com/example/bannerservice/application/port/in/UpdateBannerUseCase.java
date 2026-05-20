package com.example.bannerservice.application.port.in;

import com.example.bannerservice.application.command.UpdateBannerCommand;
import com.example.bannerservice.domain.model.Banner;

public interface UpdateBannerUseCase {
    Banner updateBanner(UpdateBannerCommand command);
}

