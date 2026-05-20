package com.example.discountservice.adapter.in.web.request;

import lombok.Data;

@Data
public class UpdateCampaignRequest {
    private String name;
    private String description;
    private String status;
    private String startDate;
    private String endDate;
}

