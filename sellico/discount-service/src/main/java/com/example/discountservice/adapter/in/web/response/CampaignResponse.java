package com.example.discountservice.adapter.in.web.response;

import com.example.discountservice.domain.model.Campaign;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignResponse {
    private UUID id;
    private UUID sellerId;
    private String name;
    private String description;
    private String campaignType;
    private String status;
    private Instant startDate;
    private Instant endDate;
    private Instant createdAt;
    private Instant updatedAt;

    public static CampaignResponse from(Campaign c) {
        return CampaignResponse.builder()
                .id(c.getId()).sellerId(c.getSellerId()).name(c.getName())
                .description(c.getDescription())
                .campaignType(c.getCampaignType() != null ? c.getCampaignType().name() : null)
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .startDate(c.getStartDate()).endDate(c.getEndDate())
                .createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt())
                .build();
    }
}

