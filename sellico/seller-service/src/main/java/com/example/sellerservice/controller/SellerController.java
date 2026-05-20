package com.example.sellerservice.controller;

import com.example.sellerservice.dto.request.*;
import com.example.sellerservice.dto.response.*;
import com.example.sellerservice.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

/**
 * Controller for seller operations
 * Base path: /api/v1/sellers
 */
@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    // =====================================================
    // Public Endpoints (no auth required)
    // =====================================================

    /**
     * GET /api/v1/sellers - List active sellers
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<SellerSummaryResponse>>> listSellers(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", defaultValue = "ratingAvg") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir
    ) {
        Page<SellerSummaryResponse> response = sellerService.listActiveSellers(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/sellers/search - Search sellers
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<SellerSummaryResponse>>> searchSellers(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Page<SellerSummaryResponse> response = sellerService.searchSellers(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/sellers/top-rated - Get top rated sellers
     */
    @GetMapping("/top-rated")
    public ResponseEntity<ApiResponse<List<SellerSummaryResponse>>> getTopRatedSellers(
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        List<SellerSummaryResponse> response = sellerService.getTopRatedSellers(limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/sellers/{id} - Get seller by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SellerResponse>> getSellerById(@PathVariable("id") UUID id) {
        SellerResponse response = sellerService.getSellerById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/sellers/slug/{slug} - Get seller by shop slug
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<SellerResponse>> getSellerBySlug(@PathVariable("slug") String slug) {
        SellerResponse response = sellerService.getSellerBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // Authenticated Endpoints
    // =====================================================

    /**
     * POST /api/v1/sellers/register - Register as seller
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<SellerResponse>> registerSeller(
            @Valid @RequestBody RegisterSellerRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        SellerResponse response = sellerService.registerSeller(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * PUT /api/v1/sellers/register/resubmit - Resubmit after rejection
     */
    @PutMapping("/register/resubmit")
    public ResponseEntity<ApiResponse<SellerResponse>> resubmitRegistration(
            @Valid @RequestBody RegisterSellerRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        SellerResponse response = sellerService.resubmitRegistration(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/sellers/register/status - Check registration status
     */
    @GetMapping("/register/status")
    public ResponseEntity<ApiResponse<SellerResponse>> checkRegistrationStatus(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        SellerResponse response = sellerService.checkRegistrationStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/sellers/{sellerId}/follow - Follow a seller
     */
    @PostMapping("/{sellerId}/follow")
    public ResponseEntity<ApiResponse<Void>> followSeller(
            @PathVariable("sellerId") UUID sellerId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        sellerService.followSeller(userId, sellerId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * DELETE /api/v1/sellers/{sellerId}/follow - Unfollow a seller
     */
    @DeleteMapping("/{sellerId}/follow")
    public ResponseEntity<ApiResponse<Void>> unfollowSeller(
            @PathVariable("sellerId") UUID sellerId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        sellerService.unfollowSeller(userId, sellerId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * GET /api/v1/sellers/{sellerId}/following - Check if following
     */
    @GetMapping("/{sellerId}/following")
    public ResponseEntity<ApiResponse<Boolean>> isFollowing(
            @PathVariable("sellerId") UUID sellerId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        boolean following = sellerService.isFollowing(userId, sellerId);
        return ResponseEntity.ok(ApiResponse.success(following));
    }

    /**
     * GET /api/v1/sellers/following - Get followed sellers
     */
    @GetMapping("/following")
    public ResponseEntity<ApiResponse<List<UUID>>> getFollowedSellers(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<UUID> sellerIds = sellerService.getFollowedSellerIds(userId);
        return ResponseEntity.ok(ApiResponse.success(sellerIds));
    }
}

