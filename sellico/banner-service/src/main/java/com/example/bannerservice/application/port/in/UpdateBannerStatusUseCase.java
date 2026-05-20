package com.example.bannerservice.application.port.in;

import com.example.bannerservice.application.command.UpdateBannerStatusCommand;
import com.example.bannerservice.domain.model.Banner;

public interface UpdateBannerStatusUseCase {
    Banner updateStatus(UpdateBannerStatusCommand command);
}

