package com.example.bannerservice.adapter.in.web.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerPositionResponse {
    private String code;
    private String name;
    private String description;
    private Integer maxBanners;
    private Boolean isActive;
    private Instant createdAt;
}

