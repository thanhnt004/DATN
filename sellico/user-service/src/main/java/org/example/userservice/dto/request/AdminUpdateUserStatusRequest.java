package org.example.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.example.userservice.entity.UserStatus;

import java.util.UUID;

@Data
public class AdminUpdateUserStatusRequest {

    @NotNull(message = "Status is required")
    private UserStatus status;

    @Size(max = 500, message = "Reason must be less than 500 characters")
    private String reason;
}

