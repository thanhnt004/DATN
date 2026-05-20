package org.example.identityservice.dto.request;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserProfileCreationRequest {
    private UUID authId;    // keycloak userId
    private String username;
    private String email;
    private String phone;
    private String userType; // BUYER | SELLER
}
