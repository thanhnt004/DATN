package com.example.discountservice.application.port.in;

import com.example.discountservice.domain.model.Campaign;
import com.example.discountservice.domain.model.Coupon;
import com.example.discountservice.domain.model.UserCouponClaim;
import com.example.discountservice.domain.model.CouponUsageHistory;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Consolidated use cases for discount service
 */
public interface CampaignUseCase {
    Campaign createCampaign(CreateCampaignCommand command);
    Campaign updateCampaign(UUID id, UpdateCampaignCommand command);
    Campaign getCampaign(UUID id);
    Page<Campaign> listCampaigns(UUID sellerId, String status, int page, int size);
    void deleteCampaign(UUID id, UUID sellerId);

    // ---- Commands ----
    record CreateCampaignCommand(UUID sellerId, String name, String description,
                                  String campaignType, String startDate, String endDate) {}

    record UpdateCampaignCommand(String name, String description, String status,
                                  String startDate, String endDate) {}
}

