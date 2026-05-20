package com.example.bannerservice.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Banner {
    private UUID id;
    private String title;
    private String imageUrl;
    private String linkUrl;
    private LinkType linkType;
    private String linkValue;
    private String positionCode;
    private Integer sortOrder;
    private BannerStatus status;
    private Instant startDate;
    private Instant endDate;
    private TargetAudience targetAudience;
    private Long clickCount;
    private Long viewCount;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public boolean isCurrentlyActive() {
        if (status != BannerStatus.ACTIVE && status != BannerStatus.SCHEDULED) {
            return false;
        }
        Instant now = Instant.now();
        boolean afterStart = (startDate == null || !now.isBefore(startDate));
        boolean beforeEnd = (endDate == null || now.isBefore(endDate));
        return afterStart && beforeEnd;
    }

    public boolean isExpired() {
        return endDate != null && Instant.now().isAfter(endDate);
    }

    public void incrementClickCount() {
        this.clickCount = (this.clickCount != null ? this.clickCount : 0) + 1;
    }

    public void incrementViewCount() {
        this.viewCount = (this.viewCount != null ? this.viewCount : 0) + 1;
    }
}

