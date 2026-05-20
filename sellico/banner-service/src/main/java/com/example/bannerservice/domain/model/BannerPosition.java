package com.example.bannerservice.domain.model;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BannerPosition {
    private String code;
    private String name;
    private String description;
    private Integer maxBanners;
    private Boolean isActive;
    private Instant createdAt;
}

