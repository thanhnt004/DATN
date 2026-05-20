package com.example.bannerservice.adapter.out.persistence.mapper;

import com.example.bannerservice.adapter.out.persistence.entity.BannerEntity;
import com.example.bannerservice.adapter.out.persistence.entity.BannerPositionEntity;
import com.example.bannerservice.domain.model.*;
import org.springframework.stereotype.Component;

@Component
public class BannerPersistenceMapper {

    public Banner toDomain(BannerEntity entity) {
        if (entity == null) return null;
        return Banner.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .imageUrl(entity.getImageUrl())
                .linkUrl(entity.getLinkUrl())
                .linkType(safeEnum(LinkType.class, entity.getLinkType()))
                .linkValue(entity.getLinkValue())
                .positionCode(entity.getPositionCode())
                .sortOrder(entity.getSortOrder())
                .status(safeEnum(BannerStatus.class, entity.getStatus()))
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .targetAudience(safeEnum(TargetAudience.class, entity.getTargetAudience()))
                .clickCount(entity.getClickCount())
                .viewCount(entity.getViewCount())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public BannerEntity toEntity(Banner domain) {
        if (domain == null) return null;
        return BannerEntity.builder()
                .id(domain.getId())
                .title(domain.getTitle())
                .imageUrl(domain.getImageUrl())
                .linkUrl(domain.getLinkUrl())
                .linkType(domain.getLinkType() != null ? domain.getLinkType().name() : "NONE")
                .linkValue(domain.getLinkValue())
                .positionCode(domain.getPositionCode())
                .sortOrder(domain.getSortOrder())
                .status(domain.getStatus() != null ? domain.getStatus().name() : "DRAFT")
                .startDate(domain.getStartDate())
                .endDate(domain.getEndDate())
                .targetAudience(domain.getTargetAudience() != null ? domain.getTargetAudience().name() : "ALL")
                .clickCount(domain.getClickCount() != null ? domain.getClickCount() : 0L)
                .viewCount(domain.getViewCount() != null ? domain.getViewCount() : 0L)
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public BannerPosition toDomain(BannerPositionEntity entity) {
        if (entity == null) return null;
        return BannerPosition.builder()
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .maxBanners(entity.getMaxBanners())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public BannerPositionEntity toEntity(BannerPosition domain) {
        if (domain == null) return null;
        return BannerPositionEntity.builder()
                .code(domain.getCode())
                .name(domain.getName())
                .description(domain.getDescription())
                .maxBanners(domain.getMaxBanners() != null ? domain.getMaxBanners() : 10)
                .isActive(domain.getIsActive() != null ? domain.getIsActive() : true)
                .createdAt(domain.getCreatedAt())
                .build();
    }

    private <E extends Enum<E>> E safeEnum(Class<E> clazz, String value) {
        if (value == null) return null;
        try {
            return Enum.valueOf(clazz, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

