package com.example.discountservice.domain.model;

import com.example.discountservice.domain.model.enums.CampaignStatus;
import com.example.discountservice.domain.model.enums.CampaignType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {
    private UUID id;
    private UUID sellerId;
    private String name;
    private String description;
    private CampaignType campaignType;
    private CampaignStatus status;
    private Instant startDate;
    private Instant endDate;
    private Instant createdAt;
    private Instant updatedAt;
}

