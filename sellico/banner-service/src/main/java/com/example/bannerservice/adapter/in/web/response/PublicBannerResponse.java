package com.example.bannerservice.adapter.in.web.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Simplified banner response for public API (no admin fields like click_count, view_count, created_by)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicBannerResponse {
    private UUID id;
    private String title;
    private String imageUrl;
    private String linkUrl;
    private String linkType;
    private String linkValue;
    private Integer sortOrder;
}

