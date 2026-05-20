package org.example.identityservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileResponse {
    UUID id;
    String username;
    String email;
    String phone;
    String userType;
    String status;
    Boolean isVerified;

    // Từ bảng user_profiles
    String fullName;
    String gender;
    LocalDate dateOfBirth;
    String avatarUrl;

    Instant createdAt;
}
