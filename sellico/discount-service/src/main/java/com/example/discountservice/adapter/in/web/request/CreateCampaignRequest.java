package com.example.discountservice.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCampaignRequest {
    private java.util.UUID sellerId;
    @NotBlank(message = "Name is required")
    private String name;
    private String description;
    @NotBlank(message = "Campaign type is required")
    private String campaignType; // PLATFORM, SHOP
    @NotNull(message = "Start date is required")
    private String startDate;
    @NotNull(message = "End date is required")
    private String endDate;
}

