package com.example.bannerservice.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBannerStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(DRAFT|ACTIVE|INACTIVE|SCHEDULED|EXPIRED)$",
             message = "Status must be one of: DRAFT, ACTIVE, INACTIVE, SCHEDULED, EXPIRED")
    private String status;
}

