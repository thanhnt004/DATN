package org.example.identityservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.identityservice.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.Map;

/**
 * Internal endpoints for inter-service communication.
 * These endpoints are NOT exposed publicly — they are accessible only within the cluster.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/roles")
public class InternalRoleController {

    private final RoleService roleService;

    /**
     * POST /internal/v1/roles/assign-seller
     * Assign the SELLER role to a Keycloak user by their Keycloak user ID.
     * Called by seller-service when a seller registration is approved.
     *
     * Request body: { "keycloakUserId": "<uuid>" }
     */
    @PostMapping("/assign-seller")
    public ResponseEntity<ApiResponse<String>> assignSellerRole(
            @RequestBody Map<String, String> body) {
        String keycloakUserId = body.get("keycloakUserId");
        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            throw new IllegalArgumentException("keycloakUserId is required");
        }
        roleService.assignSellerRole(keycloakUserId);
        return ResponseEntity.ok(ApiResponse.success("SELLER role assigned to user " + keycloakUserId));
    }
}
