package com.example.bannerservice.adapter.in.web.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePositionRequest {
    private String name;
    private String description;
    private Integer maxBanners;
    private Boolean isActive;
}

