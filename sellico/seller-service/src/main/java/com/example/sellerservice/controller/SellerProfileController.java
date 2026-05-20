package com.example.sellerservice.controller;

import com.example.sellerservice.dto.request.*;
import com.example.sellerservice.dto.response.*;
import com.example.sellerservice.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

/**
 * Controller for seller's own profile management
 * Base path: /api/v1/seller
 */
@RestController
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
public class SellerProfileController {

    private final SellerService sellerService;

    /**
     * GET /api/v1/seller/profile - Get current seller profile
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<SellerResponse>> getMyProfile(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        SellerResponse response = sellerService.getSellerByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * PUT /api/v1/seller/profile - Update seller profile
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<SellerResponse>> updateProfile(
            @Valid @RequestBody UpdateSellerProfileRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        SellerResponse response = sellerService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // Documents
    // =====================================================

    /**
     * GET /api/v1/seller/documents - Get my documents
     */
    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<SellerDocumentResponse>>> getMyDocuments(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<SellerDocumentResponse> response = sellerService.getSellerDocuments(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/seller/documents - Upload document
     */
    @PostMapping("/documents")
    public ResponseEntity<ApiResponse<SellerDocumentResponse>> uploadDocument(
            @Valid @RequestBody UploadDocumentRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        SellerDocumentResponse response = sellerService.uploadDocument(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // Bank Account
    // =====================================================

    /**
     * GET /api/v1/seller/bank-account - Get bank account
     */
    @GetMapping("/bank-account")
    public ResponseEntity<ApiResponse<BankAccountResponse>> getBankAccount(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        BankAccountResponse response = sellerService.getBankAccount(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/seller/dashboard - Get my dashboard statistics
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<SellerDashboardResponse>> getMyDashboard(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        SellerDashboardResponse response = sellerService.getDashboardStats(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * PUT /api/v1/seller/bank-account - Update bank account
     */
    @PutMapping("/bank-account")
    public ResponseEntity<ApiResponse<BankAccountResponse>> updateBankAccount(
            @Valid @RequestBody UpdateBankAccountRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        BankAccountResponse response = sellerService.updateBankAccount(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

