package org.example.notificationservice.infrastructure.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User event payload DTOs
 */
public class UserEvents {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRegisteredPayload {
        private String userId;
        private String email;
        private String name;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordResetRequestedPayload {
        private String userId;
        private String email;
        private String name;
        private String resetLink;
        private String expiresAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailVerificationPayload {
        private String userId;
        private String email;
        private String name;
        private String verificationLink;
        private String expiresAt;
    }
}

