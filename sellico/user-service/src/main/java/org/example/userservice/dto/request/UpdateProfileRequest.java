package org.example.userservice.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    @Size(max = 100, message = "Full name must be less than 100 characters")
    private String fullName;

    private String gender; // MALE, FEMALE, OTHER

    private LocalDate dateOfBirth;

    private String avatarUrl;

    @Size(max = 20, message = "Phone must be less than 20 characters")
    private String phone;
}

