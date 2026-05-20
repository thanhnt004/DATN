package org.example.identityservice.event.model;

import lombok.Data;

import java.util.UUID;

@Data
public class UserRegistrationDTO {
    private UUID userId;
    private String email;
    private String verificationToken;
}
