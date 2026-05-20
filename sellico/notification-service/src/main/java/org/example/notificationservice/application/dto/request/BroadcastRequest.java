package org.example.notificationservice.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.notificationservice.domain.model.Priority;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastRequest {
    private List<UUID> userIds; // If null or empty, send to all (depending on logic)
    @NotBlank
    private String notificationType;
    private Map<String, Object> payload;
    private Priority priority = Priority.NORMAL;
    private String language = "vi";
}
