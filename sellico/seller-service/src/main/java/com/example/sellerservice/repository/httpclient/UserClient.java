package com.example.sellerservice.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign client for communicating with user-service.
 * Used to update user type when a seller is approved.
 */
@FeignClient(name = "user-service")
public interface UserClient {

    /**
     * Update user type by auth ID (Keycloak ID).
     * Called after seller registration is approved to change userType from BUYER to SELLER.
     *
     * @param authId the Keycloak user ID
     * @param body   map containing "userType" (e.g., "SELLER")
     */
    @PatchMapping("/internal/v1/users/by-auth/{authId}/user-type")
    void updateUserType(@PathVariable("authId") String authId, @RequestBody Map<String, String> body);
}
