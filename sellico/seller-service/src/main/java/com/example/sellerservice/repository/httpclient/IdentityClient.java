package com.example.sellerservice.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign client for communicating with identity-service.
 * Used to assign Keycloak roles when a seller is approved.
 */
@FeignClient(name = "identity-service")
public interface IdentityClient {

    /**
     * Assign the SELLER role to a Keycloak user.
     * Called after seller registration is approved.
     *
     * @param body map containing "keycloakUserId"
     */
    @PostMapping("/internal/v1/roles/assign-seller")
    void assignSellerRole(@RequestBody Map<String, String> body);
}
