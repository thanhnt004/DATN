package org.example.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.request.CreateAddressRequest;
import org.example.userservice.dto.request.UpdateAddressRequest;
import org.example.userservice.dto.response.AddressResponse;
import org.example.userservice.service.AddressService;
import org.example.userservice.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * API controller for user address management
 */
@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final UserProfileService userProfileService;

    /**
     * Get all addresses for current user
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses(
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = getUserId(jwt);
        List<AddressResponse> addresses = addressService.getAddresses(userId);
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }

    /**
     * Get address by ID
     */
    @GetMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("addressId") UUID addressId) {
        UUID userId = getUserId(jwt);
        AddressResponse address = addressService.getAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success(address));
    }

    /**
     * Get default address
     */
    @GetMapping("/default")
    public ResponseEntity<ApiResponse<AddressResponse>> getDefaultAddress(
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = getUserId(jwt);
        AddressResponse address = addressService.getDefaultAddress(userId);
        return ResponseEntity.ok(ApiResponse.success(address));
    }

    /**
     * Create new address
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateAddressRequest request) {
        UUID userId = getUserId(jwt);
        AddressResponse address = addressService.createAddress(userId, request);
        URI location = URI.create("/api/v1/users/me/addresses/" + address.getId());
        return ResponseEntity.created(location).body(ApiResponse.success(address));
    }

    /**
     * Update address
     */
    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("addressId") UUID addressId,
            @Valid @RequestBody UpdateAddressRequest request) {
        UUID userId = getUserId(jwt);
        AddressResponse address = addressService.updateAddress(userId, addressId, request);
        return ResponseEntity.ok(ApiResponse.success(address));
    }

    /**
     * Delete address
     */
    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("addressId") UUID addressId) {
        UUID userId = getUserId(jwt);
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Set address as default
     */
    @PatchMapping("/{addressId}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("addressId") UUID addressId) {
        UUID userId = getUserId(jwt);
        AddressResponse address = addressService.setDefaultAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success(address));
    }

    private UUID getUserId(Jwt jwt) {
        UUID authId = UUID.fromString(jwt.getSubject());
        return userProfileService.getUserIdByAuthId(authId);
    }
}

