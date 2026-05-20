package com.example.bannerservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class UpdateBannerCommand {
    private final UUID bannerId;
    private final String title;
    private final String imageUrl;
    private final String linkUrl;
    private final String linkType;
    private final String linkValue;
    private final String positionCode;
    private final Integer sortOrder;
    private final Instant startDate;
    private final Instant endDate;
    private final String targetAudience;
}

