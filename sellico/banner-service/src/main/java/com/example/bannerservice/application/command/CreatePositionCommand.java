package com.example.bannerservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CreatePositionCommand {
    private final String code;
    private final String name;
    private final String description;
    private final Integer maxBanners;
}

