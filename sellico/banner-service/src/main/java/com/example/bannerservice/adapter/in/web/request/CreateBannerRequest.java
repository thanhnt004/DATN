package com.example.bannerservice.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBannerRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Image URL is required")
    @URL(message = "Invalid image URL format")
    private String imageUrl;

    private String linkUrl;

    @Builder.Default
    private String linkType = "NONE";

    private String linkValue;

    @NotBlank(message = "Position code is required")
    private String positionCode;

    @Builder.Default
    private Integer sortOrder = 0;

    @Builder.Default
    private String status = "DRAFT";

    private Instant startDate;
    private Instant endDate;

    @Builder.Default
    private String targetAudience = "ALL";
}

