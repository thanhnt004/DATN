package com.example.bannerservice.adapter.in.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePositionRequest {

    @NotBlank(message = "Position code is required")
    private String code;

    @NotBlank(message = "Position name is required")
    private String name;

    private String description;

    @Min(value = 1, message = "Max banners must be at least 1")
    @Builder.Default
    private Integer maxBanners = 10;
}

