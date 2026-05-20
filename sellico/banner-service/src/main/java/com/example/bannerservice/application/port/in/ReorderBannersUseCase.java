package com.example.bannerservice.application.port.in;

import com.example.bannerservice.application.command.ReorderBannersCommand;

public interface ReorderBannersUseCase {
    void reorderBanners(ReorderBannersCommand command);
}

