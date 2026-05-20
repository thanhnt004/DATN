package com.example.orderservice.adapter.web;

import com.example.orderservice.adapter.web.dto.CreateCheckOutSessionRequest;
import com.example.orderservice.adapter.web.dto.UpdateAddressRequest;
import com.example.orderservice.adapter.web.dto.UpdateBuyerNoteRequest;
import com.example.orderservice.adapter.web.dto.UpdateQuantityRequest;
import com.example.orderservice.adapter.web.dto.UpdateVoucherRequest;
import com.example.orderservice.adapter.web.mapper.CheckoutSessionMapper;
import com.example.orderservice.application.dto.command.CreateCheckoutSessionCommand;
import com.example.orderservice.application.dto.response.CheckoutSessionResponse;
import com.example.orderservice.application.port.input.CreateCheckOutSessionUseCase;
import com.example.orderservice.application.port.input.ManageCheckoutSessionUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import response.ApiResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
public class    CheckoutSessionController {
    private final CreateCheckOutSessionUseCase createCheckOutSessionUseCase;
    private final ManageCheckoutSessionUseCase manageCheckoutSessionUseCase;
    private final CheckoutSessionMapper checkoutSessionMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<CheckoutSessionResponse>> createCheckoutSession(@Valid @RequestBody CreateCheckOutSessionRequest request,
                                                                                      @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("preferred_username");
        var command = checkoutSessionMapper.toCommand(request, userId, email, name);
        CheckoutSessionResponse response = createCheckOutSessionUseCase.createCheckOutSession(command);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<CheckoutSessionResponse>> getCheckoutSession(@PathVariable UUID sessionId,
                                                                                   @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        CheckoutSessionResponse response = manageCheckoutSessionUseCase.getCheckoutSession(sessionId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{sessionId}/quantity")
    public ResponseEntity<ApiResponse<CheckoutSessionResponse>> updateQuantity(@PathVariable UUID sessionId,
                                                                               @Valid @RequestBody UpdateQuantityRequest request,
                                                                               @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        CheckoutSessionResponse response = manageCheckoutSessionUseCase.updateQuantity(sessionId, userId, request.getSkuId(), request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{sessionId}/address")
    public ResponseEntity<ApiResponse<CheckoutSessionResponse>> updateAddress(@PathVariable UUID sessionId,
                                                                              @Valid @RequestBody UpdateAddressRequest request,
                                                                              @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var address = new com.example.orderservice.domain.model.valueobject.ShippingAddress(
                request.getRecipientName(),
                request.getRecipientPhone(),
                request.getAddress(),
                request.getWard(),
                request.getDistrict(),
                request.getCity()
        );
        CheckoutSessionResponse response = manageCheckoutSessionUseCase.updateAddress(sessionId, userId, address);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{sessionId}/sellers/{sellerId}/voucher")
    public ResponseEntity<ApiResponse<CheckoutSessionResponse>> updateVoucher(@PathVariable UUID sessionId,
                                                                              @PathVariable UUID sellerId,
                                                                              @Valid @RequestBody UpdateVoucherRequest request,
                                                                              @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        CheckoutSessionResponse response = manageCheckoutSessionUseCase.updateVoucher(sessionId, userId, sellerId, request.getVoucherId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{sessionId}/voucher")
    public ResponseEntity<ApiResponse<CheckoutSessionResponse>> updatePlatformVoucher(@PathVariable UUID sessionId,
                                                                                    @Valid @RequestBody UpdateVoucherRequest request,
                                                                                    @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        CheckoutSessionResponse response = manageCheckoutSessionUseCase.updatePlatformVoucher(sessionId, userId, request.getVoucherId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{sessionId}/sellers/{sellerId}/buyer-note")
    public ResponseEntity<ApiResponse<CheckoutSessionResponse>> updateBuyerNote(@PathVariable UUID sessionId,
                                                                                @PathVariable UUID sellerId,
                                                                                @Valid @RequestBody UpdateBuyerNoteRequest request,
                                                                                @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        CheckoutSessionResponse response = manageCheckoutSessionUseCase.updateBuyerNote(sessionId, userId, sellerId, request.getBuyerNote());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
