package com.example.bannerservice.adapter.in.web.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBannerRequest {
    private String title;
    private String imageUrl;
    private String linkUrl;
    private String linkType;
    private String linkValue;
    private String positionCode;
    private Integer sortOrder;
    private Instant startDate;
    private Instant endDate;
    private String targetAudience;
}

