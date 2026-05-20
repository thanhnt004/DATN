package com.example.bannerservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class UpdateBannerStatusCommand {
    private final UUID bannerId;
    private final String status;
}

