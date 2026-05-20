package org.example.notificationservice.domain.port.out;

import lombok.Builder;
import lombok.Getter;

/**
 * Port for sending emails via external providers (Brevo, AWS SES, etc.)
 */
public interface EmailSenderPort {

    /**
     * Send an email
     * @param request the email request
     * @return SendEmailResult with success/failure status
     */
    SendEmailResult sendEmail(SendEmailRequest request);

    /**
     * Get the provider name (e.g., "BREVO", "AWS_SES")
     */
    String getProviderName();

    @Getter
    @Builder
    class SendEmailRequest {
        private String to;
        private String toName;
        private String subject;
        private String htmlContent;
        private String textContent;
        private String from;
        private String fromName;
        private String replyTo;
    }

    @Getter
    @Builder
    class SendEmailResult {
        private boolean success;
        private String messageId;
        private String errorCode;
        private String errorMessage;

        public static SendEmailResult success(String messageId) {
            return SendEmailResult.builder()
                    .success(true)
                    .messageId(messageId)
                    .build();
        }

        public static SendEmailResult failure(String errorCode, String errorMessage) {
            return SendEmailResult.builder()
                    .success(false)
                    .errorCode(errorCode)
                    .errorMessage(errorMessage)
                    .build();
        }
    }
}

