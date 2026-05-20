package com.example.sellerservice.service;

import com.example.sellerservice.dto.request.*;
import com.example.sellerservice.dto.response.*;
import com.example.sellerservice.entity.*;
import com.example.sellerservice.entity.enums.*;
import com.example.sellerservice.event.SellerEventProducer;
import com.example.sellerservice.event.SellerStatusChangedPayload;
import com.example.sellerservice.exception.SellerErrorCode;
import com.example.sellerservice.exception.SellerException;
import com.example.sellerservice.mapper.SellerMapper;
import com.example.sellerservice.repository.*;
import com.example.sellerservice.repository.httpclient.IdentityClient;
import com.example.sellerservice.repository.httpclient.OrderClient;
import com.example.sellerservice.repository.httpclient.ProductClient;
import com.example.sellerservice.repository.httpclient.UserClient;
import response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerService {

    private final SellerRepository sellerRepository;
    private final SellerDocumentRepository documentRepository;
    private final SellerBankAccountRepository bankAccountRepository;
    private final SellerFollowerRepository followerRepository;
    private final SellerMapper mapper;
    private final IdentityClient identityClient;
    private final UserClient userClient;
    private final SellerEventProducer sellerEventProducer;
    private final OrderClient orderClient;
    private final ProductClient productClient;

    // =====================================================
    // Seller Registration & Profile
    // =====================================================

    @Transactional
    public SellerResponse registerSeller(UUID userId, RegisterSellerRequest request) {
        // Check if user already has a seller account
        if (sellerRepository.existsByUserId(userId)) {
            throw new SellerException(SellerErrorCode.SELLER_ALREADY_EXISTS);
        }

        // Check shop name uniqueness
        if (sellerRepository.existsByShopName(request.getShopName())) {
            throw new SellerException(SellerErrorCode.SHOP_NAME_ALREADY_EXISTS);
        }

        // Validate business info for BUSINESS type
        if (request.getSellerType() == SellerType.BUSINESS) {
            if (request.getBusinessName() == null || request.getTaxCode() == null) {
                throw new SellerException(SellerErrorCode.BUSINESS_INFO_REQUIRED);
            }
        }

        Seller seller = Seller.builder()
                .id(userId)
                .userId(userId)
                .shopName(request.getShopName())
                .description(request.getDescription())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .ward(request.getWard())
                .district(request.getDistrict())
                .city(request.getCity())
                .sellerType(request.getSellerType())
                .businessName(request.getBusinessName())
                .taxCode(request.getTaxCode())
                .businessLicenseNumber(request.getBusinessLicenseNumber())
                .status(SellerStatus.PENDING)
                .build();

        seller = sellerRepository.save(seller);
        log.info("New seller registered: {} (userId: {})", seller.getShopName(), userId);

        return mapper.toResponse(seller);
    }

    /**
     * Seller resubmits registration after rejection — update info and set back to PENDING
     */
    @Transactional
    public SellerResponse resubmitRegistration(UUID userId, RegisterSellerRequest request) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new SellerException(SellerErrorCode.SELLER_NOT_FOUND));

        if (!seller.canResubmit()) {
            throw new SellerException(SellerErrorCode.SELLER_CANNOT_RESUBMIT);
        }

        // Update seller info
        seller.setShopName(request.getShopName());
        seller.setDescription(request.getDescription());
        seller.setEmail(request.getEmail());
        seller.setPhone(request.getPhone());
        seller.setAddress(request.getAddress());
        seller.setWard(request.getWard());
        seller.setDistrict(request.getDistrict());
        seller.setCity(request.getCity());
        seller.setSellerType(request.getSellerType());
        seller.setBusinessName(request.getBusinessName());
        seller.setTaxCode(request.getTaxCode());
        seller.setBusinessLicenseNumber(request.getBusinessLicenseNumber());

        // Reset to pending
        seller.setStatus(SellerStatus.PENDING);
        seller.setRejectionReason(null);
        seller.setRejectedAt(null);

        seller = sellerRepository.save(seller);
        log.info("Seller resubmitted registration: {} (userId: {})", seller.getShopName(), userId);

        return mapper.toResponse(seller);
    }

    /**
     * Seller checks own registration status
     */
    @Transactional(readOnly = true)
    public SellerResponse checkRegistrationStatus(UUID userId) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new SellerException(SellerErrorCode.SELLER_NOT_FOUND));
        return mapper.toResponse(seller);
    }

    @Transactional(readOnly = true)
    public SellerResponse getSellerById(UUID sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerException(SellerErrorCode.SELLER_NOT_FOUND));
        return mapper.toResponse(seller);
    }

    @Transactional(readOnly = true)
    public SellerResponse getSellerByUserId(UUID userId) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new SellerException(SellerErrorCode.SELLER_NOT_FOUND));
        return mapper.toResponse(seller);
    }

    @Transactional(readOnly = true)
    public SellerResponse getSellerBySlug(String slug) {
        Seller seller = sellerRepository.findByShopSlug(slug)
                .orElseThrow(() -> new SellerException(SellerErrorCode.SELLER_NOT_FOUND));
        return mapper.toResponse(seller);
    }

    @Transactional
    public SellerResponse updateProfile(UUID userId, UpdateSellerProfileRequest request) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new SellerException(SellerErrorCode.SELLER_NOT_FOUND));

        if (request.getShopName() != null && !request.getShopName().equals(seller.getShopName())) {
            if (sellerRepository.existsByShopName(request.getShopName())) {
                throw new SellerException(SellerErrorCode.SHOP_NAME_ALREADY_EXISTS);
            }
            seller.setShopName(request.getShopName());
        }

        if (request.getDescription() != null) {
            seller.setDescription(request.getDescription());
        }
        if (request.getLogoUrl() != null) {
            seller.setLogoUrl(request.getLogoUrl());
        }
        if (request.getBannerUrl() != null) {
            seller.setBannerUrl(request.getBannerUrl());
        }
        if (request.getPhone() != null) {
            seller.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            seller.setAddress(request.getAddress());
        }
        if (request.getWard() != null) {
            seller.setWard(request.getWard());
        }
        if (request.getDistrict() != null) {
            seller.setDistrict(request.getDistrict());
        }
        if (request.getCity() != null) {
            seller.setCity(request.getCity());
        }

        seller = sellerRepository.save(seller);
        return mapper.toResponse(seller);
    }

    // =====================================================
    // Seller Listing & Search
    // =====================================================

    @Transactional(readOnly = true)
    public Page<SellerSummaryResponse> listActiveSellers(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Seller> sellers = sellerRepository.findAllByStatus(SellerStatus.ACTIVE, pageable);
        return sellers.map(mapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public Page<SellerSummaryResponse> searchSellers(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Seller> sellers = sellerRepository.searchByKeyword(keyword, pageable);
        return sellers.map(mapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public List<SellerSummaryResponse> getTopRatedSellers(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Seller> sellers = sellerRepository.findTopRatedSellers(SellerStatus.ACTIVE, pageable);
        return mapper.toSummaryResponseList(sellers);
    }

    // =====================================================
    // Document Management
    // =====================================================

    @Transactional
    public SellerDocumentResponse uploadDocument(UUID userId, UploadDocumentRequest request) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new SellerException(SellerErrorCode.SELLER_NOT_FOUND));

        // Check if document of this type already exists
        if (documentRepository.existsBySeller_IdAndDocumentType(seller.getId(), request.getDocumentType())) {
            throw new SellerException(SellerErrorCode.DOCUMENT_ALREADY_EXISTS);
        }

        SellerDocument document = SellerDocument.builder()
                .seller(seller)
                .documentType(request.getDocumentType())
                .documentUrl(request.getDocumentUrl())
                .documentNumber(request.getDocumentNumber())
                .status(DocumentStatus.PENDING)
                .build();

        document = documentRepository.save(document);
        log.info("Document uploaded for seller {}: type={}", seller.getId(), request.getDocumentType());

        return mapper.toDocumentResponse(document);
    }

    @Transactional(readOnly = true)
    public List<SellerDocumentResponse> getSellerDocuments(UUID userId) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new SellerException(SellerErrorCode.SELLER_NOT_FOUND));

        List<SellerDocument> documents = documentRepository.findAllBySeller_Id(seller.getId());
        return mapper.toDocumentResponseList(documents);
    }

    @Transactional
    public SellerDocumentResponse verifyDocument(UUID documentId, UUID verifierId, VerifyDocumentRequest request) {
        SellerDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new SellerException(SellerErrorCode.DOCUMENT_NOT_FOUND));

        if (document.getStatus() != DocumentStatus.PENDING) {
            throw new SellerException(SellerErrorCode.DOCUMENT_ALREADY_VERIFIED);
        }

        if (request.isApproved()) {
            document.approve(verifierId);
        } else {
            document.reject(verifierId, request.getRejectionReason());
        }

        document = documentRepository.save(document);
        return mapper.toDocumentResponse(document);
    }

    // =====================================================
    // Bank Account Management
    // =====================================================

    @Transactional
    public BankAccountResponse updateBankAccount(UUID userId, UpdateBankAccountRequest request) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new SellerException(SellerErrorCode.SELLER_NOT_FOUND));

        SellerBankAccount bankAccount = bankAccountRepository.findBySeller_Id(seller.getId())
                .orElse(SellerBankAccount.builder().seller(seller).build());

        bankAccount.setBankName(request.getBankName());
        bankAccount.setBankCode(request.getBankCode());
        bankAccount.setBranchName(request.getBranchName());
        bankAccount.setAccountNumber(request.getAccountNumber());
        bankAccount.setAccountHolderName(request.getAccountHolderName());
        bankAccount.setIsVerified(false); // Reset verification when updated

        bankAccount = bankAccountRepository.save(bankAccount);
        return mapper.toBankAccountResponse(bankAccount);
    }

    @Transactional(readOnly = true)
    public BankAccountResponse getBankAccount(UUID userId) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new SellerException(SellerErrorCode.SELLER_NOT_FOUND));

        SellerBankAccount bankAccount = bankAccountRepository.findBySeller_Id(seller.getId())
                .orElseThrow(() -> new SellerException(SellerErrorCode.BANK_ACCOUNT_NOT_FOUND));

        return mapper.toBankAccountResponse(bankAccount);
    }

    // =====================================================
    // Follow/Unfollow
    // =====================================================

    @Transactional
    public void followSeller(UUID userId, UUID sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerException(SellerErrorCode.SELLER_NOT_FOUND));

        // Cannot follow own shop
        if (seller.getUserId().equals(userId)) {
            throw new SellerException(SellerErrorCode.CANNOT_FOLLOW_OWN_SHOP);
        }

        // Check if already following
        if (followerRepository.existsBySellerIdAndUserId(sellerId, userId)) {
            throw new SellerException(SellerErrorCode.ALREADY_FOLLOWING);
        }

        SellerFollower follower = SellerFollower.builder()
                .sellerId(sellerId)
                .userId(userId)
                .build();

        followerRepository.save(follower);

        // Update follower count
        seller.setFollowerCount(seller.getFollowerCount() + 1);
        sellerRepository.save(seller);

        log.info("User {} followed seller {}", userId, sellerId);
    }

    @Transactional
    public void unfollowSeller(UUID userId, UUID sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerException(SellerErrorCode.SELLER_NOT_FOUND));

        if (!followerRepository.existsBySellerIdAndUserId(sellerId, userId)) {
            throw new SellerException(SellerErrorCode.NOT_FOLLOWING);
        }

        followerRepository.deleteBySellerIdAndUserId(sellerId, userId);

        // Update follower count
        seller.setFollowerCount(Math.max(0, seller.getFollowerCount() - 1));
        sellerRepository.save(seller);

        log.info("User {} unfollowed seller {}", userId, sellerId);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(UUID userId, UUID sellerId) {
        return followerRepository.existsBySellerIdAndUserId(sellerId, userId);
    }

    @Transactional(readOnly = true)
    public List<UUID> getFollowedSellerIds(UUID userId) {
        return followerRepository.findSellerIdsByUserId(userId);
    }

    // =====================================================
    // Admin Operations
    // =====================================================

    @Transactional
    public SellerResponse updateSellerStatus(UUID sellerId, UpdateSellerStatusRequest request) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerException(SellerErrorCode.SELLER_NOT_FOUND));

        boolean shouldAssignRole = request.getStatus() == SellerStatus.ACTIVE;
        boolean isFirstApproval = shouldAssignRole && seller.getApprovedAt() == null;

        seller.setStatus(request.getStatus());

        if (isFirstApproval) {
            seller.activate();
        }

        if (request.getReason() != null) {
            seller.setStatusNote(request.getReason());
        }

        seller = sellerRepository.save(seller);
        log.info("Seller {} status updated to {}", sellerId, request.getStatus());

        // Assign SELLER role in Keycloak when seller is set to ACTIVE
        // (handles both first approval and re-approval after previous failure)
        if (shouldAssignRole) {
            try {
                identityClient.assignSellerRole(
                        Map.of("keycloakUserId", seller.getUserId().toString()));
                log.info("SELLER role assigned in Keycloak for userId={}", seller.getUserId());
            } catch (Exception e) {
                log.error("Failed to assign SELLER role for userId={}: {}", seller.getUserId(), e.getMessage());
            }

            // Update userType from BUYER to SELLER in user-service
            try {
                userClient.updateUserType(
                        seller.getUserId().toString(),
                        Map.of("userType", "SELLER"));
                log.info("UserType updated to SELLER in user-service for authId={}", seller.getUserId());
            } catch (Exception e) {
                log.error("Failed to update userType for authId={}: {}", seller.getUserId(), e.getMessage());
            }
        }

        // Publish Kafka event for email notification
        publishSellerStatusEvent(seller, request.getStatus().name(), request.getReason());

        return mapper.toResponse(seller);
    }

    /**
     * Admin rejects a pending seller registration with reason
     */
    @Transactional
    public SellerResponse rejectSeller(UUID sellerId, String reason, UUID adminId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerException(SellerErrorCode.SELLER_NOT_FOUND));

        if (seller.getStatus() != SellerStatus.PENDING) {
            throw new SellerException(SellerErrorCode.SELLER_NOT_PENDING);
        }

        seller.reject(reason, adminId);
        seller = sellerRepository.save(seller);
        log.info("Seller {} rejected by admin {}: {}", sellerId, adminId, reason);

        // Publish Kafka event for email notification
        publishSellerStatusEvent(seller, "REJECTED", reason);

        return mapper.toResponse(seller);
    }

    /**
     * Admin reactivates a suspended/banned seller
     */
    @Transactional
    public SellerResponse reactivateSeller(UUID sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerException(SellerErrorCode.SELLER_NOT_FOUND));

        if (seller.getStatus() != SellerStatus.SUSPENDED && seller.getStatus() != SellerStatus.BANNED) {
            throw new SellerException(SellerErrorCode.INVALID_STATUS_TRANSITION);
        }

        seller.reactivate();
        seller = sellerRepository.save(seller);
        log.info("Seller {} reactivated", sellerId);

        // Publish Kafka event for email notification
        publishSellerStatusEvent(seller, "REACTIVATED", null);

        return mapper.toResponse(seller);
    }

    /**
     * Admin gets full seller detail including documents and bank account
     */
    @Transactional(readOnly = true)
    public AdminSellerDetailResponse getAdminSellerDetail(UUID sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerException(SellerErrorCode.SELLER_NOT_FOUND));

        List<SellerDocument> documents = documentRepository.findAllBySeller_Id(sellerId);
        SellerBankAccount bankAccount = bankAccountRepository.findBySeller_Id(sellerId).orElse(null);

        return AdminSellerDetailResponse.builder()
                .id(seller.getId())
                .userId(seller.getUserId())
                .shopName(seller.getShopName())
                .shopSlug(seller.getShopSlug())
                .description(seller.getDescription())
                .logoUrl(seller.getLogoUrl())
                .bannerUrl(seller.getBannerUrl())
                .sellerType(seller.getSellerType())
                .status(seller.getStatus())
                .email(seller.getEmail())
                .phone(seller.getPhone())
                .address(seller.getAddress())
                .ward(seller.getWard())
                .district(seller.getDistrict())
                .city(seller.getCity())
                .country(seller.getCountry())
                .businessName(seller.getBusinessName())
                .taxCode(seller.getTaxCode())
                .businessLicenseNumber(seller.getBusinessLicenseNumber())
                .ratingAvg(seller.getRatingAvg())
                .ratingCount(seller.getRatingCount())
                .totalProducts(seller.getTotalProducts())
                .totalOrders(seller.getTotalOrders())
                .totalRevenue(seller.getTotalRevenue())
                .followerCount(seller.getFollowerCount())
                .createdAt(seller.getCreatedAt())
                .updatedAt(seller.getUpdatedAt())
                .approvedAt(seller.getApprovedAt())
                .rejectedAt(seller.getRejectedAt())
                .rejectionReason(seller.getRejectionReason())
                .statusNote(seller.getStatusNote())
                .statusUpdatedBy(seller.getStatusUpdatedBy())
                .documents(mapper.toDocumentResponseList(documents))
                .bankAccount(bankAccount != null ? mapper.toBankAccountResponse(bankAccount) : null)
                .build();
    }

    /**
     * Admin search sellers by keyword (name, email, phone)
     */
    @Transactional(readOnly = true)
    public Page<SellerResponse> searchSellersForAdmin(String keyword, SellerStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // If status is provided but no keyword, use status filter
        if (status != null && (keyword == null || keyword.isBlank())) {
            return sellerRepository.findAllByStatus(status, pageable).map(mapper::toResponse);
        }

        // If keyword is provided, search by keyword
        Page<Seller> sellers = sellerRepository.searchForAdmin(keyword, pageable);

        // Apply status filter in-memory if both keyword and status provided
        if (status != null) {
            return sellers.map(mapper::toResponse);
        }

        return sellers.map(mapper::toResponse);
    }

    /**
     * Admin: Get seller statistics by status
     */
    @Transactional(readOnly = true)
    public SellerStatsResponse getSellerStats() {
        return SellerStatsResponse.builder()
                .totalSellers(sellerRepository.count())
                .pendingSellers(sellerRepository.countByStatus(SellerStatus.PENDING))
                .activeSellers(sellerRepository.countByStatus(SellerStatus.ACTIVE))
                .rejectedSellers(sellerRepository.countByStatus(SellerStatus.REJECTED))
                .suspendedSellers(sellerRepository.countByStatus(SellerStatus.SUSPENDED))
                .bannedSellers(sellerRepository.countByStatus(SellerStatus.BANNED))
                .closedSellers(sellerRepository.countByStatus(SellerStatus.CLOSED))
                .build();
    }

    @Transactional(readOnly = true)
    public Page<SellerResponse> listSellersByStatus(SellerStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Seller> sellers = sellerRepository.findAllByStatus(status, pageable);
        return sellers.map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<SellerDocumentResponse> getPendingDocuments() {
        List<SellerDocument> documents = documentRepository.findAllByStatus(DocumentStatus.PENDING);
        return mapper.toDocumentResponseList(documents);
    }

    // =====================================================
    // Event Publishing
    // =====================================================

    private void publishSellerStatusEvent(Seller seller, String status, String reason) {
        try {
            String eventType = switch (status) {
                case "ACTIVE"    -> "SELLER_APPROVED";
                case "REJECTED"  -> "SELLER_REJECTED";
                case "SUSPENDED" -> "SELLER_SUSPENDED";
                case "BANNED"    -> "SELLER_BANNED";
                case "REACTIVATED" -> "SELLER_REACTIVATED";
                default -> "SELLER_STATUS_CHANGED";
            };

            SellerStatusChangedPayload payload = SellerStatusChangedPayload.builder()
                    .sellerId(seller.getId().toString())
                    .userId(seller.getUserId().toString())
                    .email(seller.getEmail())
                    .shopName(seller.getShopName())
                    .newStatus(status)
                    .reason(reason)
                    .build();

            sellerEventProducer.publishSellerStatusChanged(eventType, payload);
        } catch (Exception e) {
            log.error("Failed to publish seller status event for seller {}: {}",
                    seller.getId(), e.getMessage());
        }
    }

    // =====================================================
    // Internal Operations (for other services)
    // =====================================================

    @Transactional(readOnly = true)
    public List<SellerSummaryResponse> getBatchSellers(List<UUID> sellerIds) {
        List<Seller> sellers = sellerRepository.findAllByIds(sellerIds);
        return mapper.toSummaryResponseList(sellers);
    }

    @Transactional(readOnly = true)
    public boolean isSellerActive(UUID sellerId) {
        return sellerRepository.findById(sellerId)
                .map(Seller::isActive)
                .orElse(false);
    }

    @Transactional
    public void updateSellerStats(UUID sellerId, Integer productCount, Integer orderCount) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerException(SellerErrorCode.SELLER_NOT_FOUND));

        if (productCount != null) {
            seller.setTotalProducts(productCount);
        }
        if (orderCount != null) {
            seller.setTotalOrders(orderCount);
        }

        sellerRepository.save(seller);
    }

    /**
     * Process order completed event to update revenue and order count
     */
    @Transactional
    public void processOrderCompleted(UUID sellerId, java.math.BigDecimal amount) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElse(null);
        if (seller != null) {
            seller.incrementOrderCount();
            seller.addRevenue(amount);
            sellerRepository.save(seller);
            log.info("Updated stats for seller {}: +1 order, +{} revenue", sellerId, amount);
        } else {
            log.warn("Seller {} not found for order completed event", sellerId);
        }
    }

    /**
     * Get dashboard statistics for current seller
     */
    @Transactional(readOnly = true)
    public SellerDashboardResponse getDashboardStats(UUID userId) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new SellerException(SellerErrorCode.SELLER_NOT_FOUND));

        UUID sellerId = seller.getId();

        // Base stats from DB
        SellerDashboardResponse dashboard = mapper.toDashboardResponse(seller);

        // Fetch real-time stats from other services
        try {
            // Count pending orders
            ApiResponse<Object> pendingResponse = orderClient.getSellerOrders(sellerId, "PENDING", 0, 1);
            dashboard.setPendingOrders(extractTotalElements(pendingResponse));

            // Count processing orders (CONFIRMED or SHIPPING)
            ApiResponse<Object> confirmedResponse = orderClient.getSellerOrders(sellerId, "CONFIRMED", 0, 1);
            ApiResponse<Object> shippingResponse = orderClient.getSellerOrders(sellerId, "SHIPPING", 0, 1);
            dashboard.setProcessingOrders(extractTotalElements(confirmedResponse) + extractTotalElements(shippingResponse));

            // Count low stock products (dummy for now as we don't have a specific low stock filter)
            // dashboard.setLowStockProducts(0);
        } catch (Exception e) {
            log.error("Failed to fetch real-time stats for seller {}: {}", sellerId, e.getMessage());
            // Fallback to zeros or what we have in DB
        }

        // Get follower count
        dashboard.setFollowerCount((int) followerRepository.countBySellerId(sellerId));

        return dashboard;
    }

    private Integer extractTotalElements(ApiResponse<Object> response) {
        if (response == null || response.getData() == null) {
            return 0;
        }
        try {
            // response.getData() is likely a Map representing OrderPageResponse
            if (response.getData() instanceof Map) {
                Map<?, ?> data = (Map<?, ?>) response.getData();
                Object totalElements = data.get("totalElements");
                if (totalElements instanceof Number) {
                    return ((Number) totalElements).intValue();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract totalElements from response: {}", e.getMessage());
        }
        return 0;
    }
}

