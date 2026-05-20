package com.example.bannerservice.application.port.in;

import com.example.bannerservice.application.command.CreateBannerCommand;
import com.example.bannerservice.domain.model.Banner;

public interface CreateBannerUseCase {
    Banner createBanner(CreateBannerCommand command);
}

