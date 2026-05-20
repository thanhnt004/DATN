package org.example.userservice.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import lombok.*;

import java.util.UUID;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileCreationRequest {
    private UUID authId;    // keycloak userId
    private String username;
    private String email;
    private String phone;
    private String userType; // BUYER | SELLER
}
