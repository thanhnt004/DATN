package com.example.bannerservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ReorderBannersCommand {
    private final String positionCode;
    private final List<BannerOrder> bannerOrders;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class BannerOrder {
        private final UUID bannerId;
        private final int sortOrder;
    }
}

