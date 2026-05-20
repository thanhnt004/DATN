package org.example.identityservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class BuyerRegisterResponse {
    private UUID userId;     // keycloak userId
    private String username;
    private String email;
}