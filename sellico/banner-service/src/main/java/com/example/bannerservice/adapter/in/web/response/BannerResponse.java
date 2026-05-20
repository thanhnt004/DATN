package com.example.bannerservice.adapter.in.web.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerResponse {
    private UUID id;
    private String title;
    private String imageUrl;
    private String linkUrl;
    private String linkType;
    private String linkValue;
    private String positionCode;
    private Integer sortOrder;
    private String status;
    private Instant startDate;
    private Instant endDate;
    private String targetAudience;
    private Long clickCount;
    private Long viewCount;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}

