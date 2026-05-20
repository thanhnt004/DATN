package org.example.notificationservice.infrastructure.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.example.notificationservice.domain.port.out.EmailSenderPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Brevo (formerly Sendinblue) email sender implementation
 */
@Component
@Slf4j
public class BrevoEmailSender implements EmailSenderPort {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String defaultSenderEmail;
    private final String defaultSenderName;

    public BrevoEmailSender(
            @Value("${brevo.api-key:}") String apiKey,
            @Value("${brevo.sender.email:noreply@sellico.com}") String defaultSenderEmail,
            @Value("${brevo.sender.name:Sellico}") String defaultSenderName) {
        this.apiKey = apiKey;
        this.defaultSenderEmail = defaultSenderEmail;
        this.defaultSenderName = defaultSenderName;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public SendEmailResult sendEmail(SendEmailRequest request) {
        log.info("Sending email via Brevo to: {}", request.getTo());

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Brevo API key not configured. Email will not be sent.");
            return SendEmailResult.failure("CONFIG_ERROR", "Brevo API key not configured");
        }

        try {
            String jsonBody = buildRequestBody(request);

            Request httpRequest = new Request.Builder()
                    .url(BREVO_API_URL)
                    .header("accept", "application/json")
                    .header("api-key", apiKey)
                    .header("content-type", "application/json")
                    .post(RequestBody.create(jsonBody, JSON))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    JsonNode jsonResponse = objectMapper.readTree(responseBody);
                    String messageId = jsonResponse.has("messageId")
                            ? jsonResponse.get("messageId").asText()
                            : "unknown";
                    log.info("Email sent successfully. MessageId: {}", messageId);
                    return SendEmailResult.success(messageId);
                } else {
                    log.error("Brevo API error. Status: {}, Body: {}", response.code(), responseBody);
                    return SendEmailResult.failure(
                            String.valueOf(response.code()),
                            "Brevo API error: " + responseBody
                    );
                }
            }
        } catch (IOException e) {
            log.error("Failed to send email via Brevo: {}", e.getMessage(), e);
            return SendEmailResult.failure("IO_ERROR", e.getMessage());
        }
    }

    private String buildRequestBody(SendEmailRequest request) throws IOException {
        BrevoEmailRequest brevoRequest = BrevoEmailRequest.builder()
                .sender(new BrevoEmailRequest.Contact(
                        request.getFrom() != null ? request.getFrom() : defaultSenderEmail,
                        request.getFromName() != null ? request.getFromName() : defaultSenderName
                ))
                .to(new BrevoEmailRequest.Contact[]{
                        new BrevoEmailRequest.Contact(request.getTo(), request.getToName())
                })
                .subject(request.getSubject())
                .htmlContent(request.getHtmlContent())
                .textContent(request.getTextContent())
                .build();

        return objectMapper.writeValueAsString(brevoRequest);
    }

    @Override
    public String getProviderName() {
        return "BREVO";
    }

    /**
     * Internal DTO for Brevo API request
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class BrevoEmailRequest {
        private Contact sender;
        private Contact[] to;
        private String subject;
        private String htmlContent;
        private String textContent;

        @lombok.Data
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        static class Contact {
            private String email;
            private String name;
        }
    }
}

