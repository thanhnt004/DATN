package com.example.bannerservice.adapter.in.web.controller;

import com.example.bannerservice.adapter.in.web.request.*;
import com.example.bannerservice.adapter.in.web.response.BannerPositionResponse;
import com.example.bannerservice.adapter.in.web.response.BannerResponse;
import com.example.bannerservice.application.command.*;
import com.example.bannerservice.application.port.in.*;
import com.example.bannerservice.domain.model.Banner;
import com.example.bannerservice.domain.model.BannerPosition;
import com.example.bannerservice.domain.model.BannerStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Admin endpoints for banner management (requires authentication).
 */
@RestController
@RequestMapping("/api/v1/admin/banners")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AdminBannerController {

    private final CreateBannerUseCase createBannerUseCase;
    private final UpdateBannerUseCase updateBannerUseCase;
    private final DeleteBannerUseCase deleteBannerUseCase;
    private final GetBannerUseCase getBannerUseCase;
    private final UpdateBannerStatusUseCase updateBannerStatusUseCase;
    private final ReorderBannersUseCase reorderBannersUseCase;
    private final ManagePositionUseCase managePositionUseCase;

    // =====================================================
    // Banner CRUD
    // =====================================================

    /**
     * POST /api/v1/admin/banners
     * Create a new banner.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BannerResponse>> createBanner(
            @Valid @RequestBody CreateBannerRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        CreateBannerCommand command = CreateBannerCommand.builder()
                .title(request.getTitle())
                .imageUrl(request.getImageUrl())
                .linkUrl(request.getLinkUrl())
                .linkType(request.getLinkType())
                .linkValue(request.getLinkValue())
                .positionCode(request.getPositionCode())
                .sortOrder(request.getSortOrder())
                .status(request.getStatus())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .targetAudience(request.getTargetAudience())
                .createdBy(userId)
                .build();

        Banner banner = createBannerUseCase.createBanner(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(toBannerResponse(banner)));
    }

    /**
     * PUT /api/v1/admin/banners/{id}
     * Update banner details.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerResponse>> updateBanner(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateBannerRequest request
    ) {
        UpdateBannerCommand command = UpdateBannerCommand.builder()
                .bannerId(id)
                .title(request.getTitle())
                .imageUrl(request.getImageUrl())
                .linkUrl(request.getLinkUrl())
                .linkType(request.getLinkType())
                .linkValue(request.getLinkValue())
                .positionCode(request.getPositionCode())
                .sortOrder(request.getSortOrder())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .targetAudience(request.getTargetAudience())
                .build();

        Banner banner = updateBannerUseCase.updateBanner(command);
        return ResponseEntity.ok(ApiResponse.success(toBannerResponse(banner)));
    }

    /**
     * DELETE /api/v1/admin/banners/{id}
     * Delete a banner.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBanner(@PathVariable("id") UUID id) {
        deleteBannerUseCase.deleteBanner(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * GET /api/v1/admin/banners/{id}
     * Get banner details (admin view with stats).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerResponse>> getBanner(@PathVariable("id") UUID id) {
        Banner banner = getBannerUseCase.getBannerById(id);
        return ResponseEntity.ok(ApiResponse.success(toBannerResponse(banner)));
    }

    /**
     * GET /api/v1/admin/banners
     * List banners with pagination and filters.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<BannerResponse>>> listBanners(
            @RequestParam(value = "positionCode", required = false) String positionCode,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", defaultValue = "sortOrder") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "asc") String sortDirection
    ) {
        BannerStatus bannerStatus = null;
        if (status != null) {
            bannerStatus = BannerStatus.valueOf(status.toUpperCase());
        }

        Page<Banner> banners = getBannerUseCase.listBanners(
                positionCode, bannerStatus, page, size, sortBy, sortDirection
        );
        Page<BannerResponse> response = banners.map(this::toBannerResponse);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // Status management
    // =====================================================

    /**
     * PATCH /api/v1/admin/banners/{id}/status
     * Change banner status (DRAFT -> ACTIVE, ACTIVE -> INACTIVE, etc.)
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BannerResponse>> updateBannerStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateBannerStatusRequest request
    ) {
        UpdateBannerStatusCommand command = UpdateBannerStatusCommand.builder()
                .bannerId(id)
                .status(request.getStatus())
                .build();

        Banner banner = updateBannerStatusUseCase.updateStatus(command);
        return ResponseEntity.ok(ApiResponse.success(toBannerResponse(banner)));
    }

    // =====================================================
    // Reorder banners within a position
    // =====================================================

    /**
     * PUT /api/v1/admin/banners/positions/{positionCode}/reorder
     * Reorder banners within a position (drag-and-drop support).
     */
    @PutMapping("/positions/{positionCode}/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderBanners(
            @PathVariable("positionCode") String positionCode,
            @Valid @RequestBody ReorderBannersRequest request
    ) {
        ReorderBannersCommand command = ReorderBannersCommand.builder()
                .positionCode(positionCode)
                .bannerOrders(request.getBannerOrders().stream()
                        .map(item -> ReorderBannersCommand.BannerOrder.builder()
                                .bannerId(item.getBannerId())
                                .sortOrder(item.getSortOrder())
                                .build())
                        .collect(Collectors.toList()))
                .build();

        reorderBannersUseCase.reorderBanners(command);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // =====================================================
    // Position management
    // =====================================================

    /**
     * GET /api/v1/admin/banners/positions
     * List all banner positions (including inactive).
     */
    @GetMapping("/positions")
    public ResponseEntity<ApiResponse<List<BannerPositionResponse>>> getAllPositions() {
        List<BannerPosition> positions = managePositionUseCase.getAllPositions();
        List<BannerPositionResponse> response = positions.stream()
                .map(this::toPositionResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/admin/banners/positions/{code}
     * Get a single position by code.
     */
    @GetMapping("/positions/{code}")
    public ResponseEntity<ApiResponse<BannerPositionResponse>> getPosition(@PathVariable("code") String code) {
        BannerPosition position = managePositionUseCase.getPositionByCode(code);
        return ResponseEntity.ok(ApiResponse.success(toPositionResponse(position)));
    }

    /**
     * POST /api/v1/admin/banners/positions
     * Create a new banner position.
     */
    @PostMapping("/positions")
    public ResponseEntity<ApiResponse<BannerPositionResponse>> createPosition(
            @Valid @RequestBody CreatePositionRequest request
    ) {
        CreatePositionCommand command = CreatePositionCommand.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .maxBanners(request.getMaxBanners())
                .build();

        BannerPosition position = managePositionUseCase.createPosition(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(toPositionResponse(position)));
    }

    /**
     * PUT /api/v1/admin/banners/positions/{code}
     * Update an existing banner position.
     */
    @PutMapping("/positions/{code}")
    public ResponseEntity<ApiResponse<BannerPositionResponse>> updatePosition(
            @PathVariable("code") String code,
            @Valid @RequestBody UpdatePositionRequest request
    ) {
        UpdatePositionCommand command = UpdatePositionCommand.builder()
                .code(code)
                .name(request.getName())
                .description(request.getDescription())
                .maxBanners(request.getMaxBanners())
                .isActive(request.getIsActive())
                .build();

        BannerPosition position = managePositionUseCase.updatePosition(command);
        return ResponseEntity.ok(ApiResponse.success(toPositionResponse(position)));
    }

    /**
     * DELETE /api/v1/admin/banners/positions/{code}
     * Delete a banner position.
     */
    @DeleteMapping("/positions/{code}")
    public ResponseEntity<ApiResponse<Void>> deletePosition(@PathVariable("code") String code) {
        managePositionUseCase.deletePosition(code);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // =====================================================
    // Mapper helpers
    // =====================================================

    private BannerResponse toBannerResponse(Banner b) {
        return BannerResponse.builder()
                .id(b.getId())
                .title(b.getTitle())
                .imageUrl(b.getImageUrl())
                .linkUrl(b.getLinkUrl())
                .linkType(b.getLinkType() != null ? b.getLinkType().name() : null)
                .linkValue(b.getLinkValue())
                .positionCode(b.getPositionCode())
                .sortOrder(b.getSortOrder())
                .status(b.getStatus() != null ? b.getStatus().name() : null)
                .startDate(b.getStartDate())
                .endDate(b.getEndDate())
                .targetAudience(b.getTargetAudience() != null ? b.getTargetAudience().name() : null)
                .clickCount(b.getClickCount())
                .viewCount(b.getViewCount())
                .createdBy(b.getCreatedBy())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
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

