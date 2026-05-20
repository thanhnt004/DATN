package com.example.sellerservice.controller;

import com.example.sellerservice.dto.request.*;
import com.example.sellerservice.dto.response.*;
import com.example.sellerservice.entity.enums.SellerStatus;
import com.example.sellerservice.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

/**
 * Admin seller management endpoints
 * Base path: /api/v1/admin/sellers
 */
@RestController
@RequestMapping("/api/v1/admin/sellers")
@RequiredArgsConstructor
public class AdminSellerController {

    private final SellerService sellerService;

    // =====================================================
    // List & Search
    // =====================================================

    /**
     * GET /api/v1/admin/sellers — List sellers by status (default: PENDING)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<SellerResponse>>> listSellers(
            @RequestParam(value = "status", required = false) SellerStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Page<SellerResponse> response = sellerService.listSellersByStatus(
                status != null ? status : SellerStatus.PENDING, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/admin/sellers/search — Search sellers by keyword + status filter
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<SellerResponse>>> searchSellers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) SellerStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Page<SellerResponse> response = sellerService.searchSellersForAdmin(keyword, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/admin/sellers/stats — Get seller statistics by status
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SellerStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(sellerService.getSellerStats()));
    }

    // =====================================================
    // Detail
    // =====================================================

    /**
     * GET /api/v1/admin/sellers/{sellerId} — Full seller detail (with documents & bank account)
     */
    @GetMapping("/{sellerId}")
    public ResponseEntity<ApiResponse<AdminSellerDetailResponse>> getSellerDetail(@PathVariable("sellerId") UUID sellerId) {
        AdminSellerDetailResponse response = sellerService.getAdminSellerDetail(sellerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // Status Management
    // =====================================================

    /**
     * PATCH /api/v1/admin/sellers/{sellerId}/status — Generic status update
     */
    @PatchMapping("/{sellerId}/status")
    public ResponseEntity<ApiResponse<SellerResponse>> updateSellerStatus(
            @PathVariable("sellerId") UUID sellerId,
            @Valid @RequestBody UpdateSellerStatusRequest request
    ) {
        SellerResponse response = sellerService.updateSellerStatus(sellerId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/admin/sellers/{sellerId}/approve — Approve pending seller
     */
    @PostMapping("/{sellerId}/approve")
    public ResponseEntity<ApiResponse<SellerResponse>> approveSeller(@PathVariable("sellerId") UUID sellerId) {
        UpdateSellerStatusRequest request = UpdateSellerStatusRequest.builder()
                .status(SellerStatus.ACTIVE)
                .build();
        SellerResponse response = sellerService.updateSellerStatus(sellerId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/admin/sellers/{sellerId}/reject — Reject pending seller with reason
     */
    @PostMapping("/{sellerId}/reject")
    public ResponseEntity<ApiResponse<SellerResponse>> rejectSeller(
            @PathVariable("sellerId") UUID sellerId,
            @Valid @RequestBody RejectSellerRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        SellerResponse response = sellerService.rejectSeller(sellerId, request.getReason(), adminId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/admin/sellers/{sellerId}/suspend — Suspend active seller
     */
    @PostMapping("/{sellerId}/suspend")
    public ResponseEntity<ApiResponse<SellerResponse>> suspendSeller(
            @PathVariable("sellerId") UUID sellerId,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        UpdateSellerStatusRequest request = UpdateSellerStatusRequest.builder()
                .status(SellerStatus.SUSPENDED)
                .reason(reason)
                .build();
        SellerResponse response = sellerService.updateSellerStatus(sellerId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/admin/sellers/{sellerId}/ban — Ban seller permanently
     */
    @PostMapping("/{sellerId}/ban")
    public ResponseEntity<ApiResponse<SellerResponse>> banSeller(
            @PathVariable("sellerId") UUID sellerId,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        UpdateSellerStatusRequest request = UpdateSellerStatusRequest.builder()
                .status(SellerStatus.BANNED)
                .reason(reason)
                .build();
        SellerResponse response = sellerService.updateSellerStatus(sellerId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/admin/sellers/{sellerId}/reactivate — Reactivate suspended/banned seller
     */
    @PostMapping("/{sellerId}/reactivate")
    public ResponseEntity<ApiResponse<SellerResponse>> reactivateSeller(@PathVariable("sellerId") UUID sellerId) {
        SellerResponse response = sellerService.reactivateSeller(sellerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // Document Verification
    // =====================================================

    /**
     * GET /api/v1/admin/sellers/documents/pending — Get all pending documents
     */
    @GetMapping("/documents/pending")
    public ResponseEntity<ApiResponse<List<SellerDocumentResponse>>> getPendingDocuments() {
        List<SellerDocumentResponse> response = sellerService.getPendingDocuments();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/admin/sellers/documents/{documentId}/verify — Verify/reject a document
     */
    @PostMapping("/documents/{documentId}/verify")
    public ResponseEntity<ApiResponse<SellerDocumentResponse>> verifyDocument(
            @PathVariable("documentId") UUID documentId,
            @Valid @RequestBody VerifyDocumentRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID verifierId = UUID.fromString(jwt.getSubject());
        SellerDocumentResponse response = sellerService.verifyDocument(documentId, verifierId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

