package com.example.bannerservice.adapter.in.web.controller;

import com.example.bannerservice.adapter.in.web.response.BannerPositionResponse;
import com.example.bannerservice.adapter.in.web.response.PublicBannerResponse;
import com.example.bannerservice.application.port.in.GetBannerUseCase;
import com.example.bannerservice.application.port.in.ManagePositionUseCase;
import com.example.bannerservice.application.port.in.TrackBannerUseCase;
import com.example.bannerservice.domain.model.Banner;
import com.example.bannerservice.domain.model.BannerPosition;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

/**
 * Public endpoints for storefront banner display.
 * No authentication required.
 */
@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
public class PublicBannerController {

    private final GetBannerUseCase getBannerUseCase;
    private final TrackBannerUseCase trackBannerUseCase;
    private final ManagePositionUseCase managePositionUseCase;

    // =====================================================
    // Get active banners by position (for storefront rendering)
    // =====================================================

    /**
     * GET /api/v1/banners/active/{positionCode}
     * Returns banners currently active at a given position, sorted by sort_order.
     * Used by: Homepage carousel, category page, flash sale section, etc.
     */
    @GetMapping("/active/{positionCode}")
    public ResponseEntity<ApiResponse<List<PublicBannerResponse>>> getActiveBanners(
            @PathVariable("positionCode") String positionCode
    ) {
        List<Banner> banners = getBannerUseCase.getActiveBannersByPosition(positionCode);
        List<PublicBannerResponse> response = banners.stream()
                .map(this::toPublicResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // Get all active positions (for frontend to know what to render)
    // =====================================================

    /**
     * GET /api/v1/banners/positions
     * Returns all active banner positions.
     */
    @GetMapping("/positions")
    public ResponseEntity<ApiResponse<List<BannerPositionResponse>>> getActivePositions() {
        List<BannerPosition> positions = managePositionUseCase.getActivePositions();
        List<BannerPositionResponse> response = positions.stream()
                .map(this::toPositionResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // Click & View tracking
    // =====================================================

    /**
     * POST /api/v1/banners/{id}/click
     * Track a banner click event.
     */
    @PostMapping("/{id}/click")
    public ResponseEntity<ApiResponse<Void>> trackClick(@PathVariable("id") UUID id) {
        trackBannerUseCase.trackClick(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * POST /api/v1/banners/{id}/view
     * Track a banner view/impression event.
     */
    @PostMapping("/{id}/view")
    public ResponseEntity<ApiResponse<Void>> trackView(@PathVariable("id") UUID id) {
        trackBannerUseCase.trackView(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // =====================================================
    // Mapper helpers
    // =====================================================

    private PublicBannerResponse toPublicResponse(Banner b) {
        return PublicBannerResponse.builder()
                .id(b.getId())
                .title(b.getTitle())
                .imageUrl(b.getImageUrl())
                .linkUrl(b.getLinkUrl())
                .linkType(b.getLinkType() != null ? b.getLinkType().name() : null)
                .linkValue(b.getLinkValue())
                .sortOrder(b.getSortOrder())
                .build();
    }

    private BannerPositionResponse toPositionResponse(BannerPosition p) {
        return BannerPositionResponse.builder()
                .code(p.getCode())
                .name(p.getName())
                .description(p.getDescription())
                .maxBanners(p.getMaxBanners())
                .isActive(p.getIsActive())
                .createdAt(p.getCreatedAt())
                .build();
    }
}

