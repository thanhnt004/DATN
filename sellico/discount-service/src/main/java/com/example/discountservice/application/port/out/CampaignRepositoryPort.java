package com.example.discountservice.application.port.out;

import com.example.discountservice.domain.model.Campaign;
import com.example.discountservice.domain.model.enums.CampaignStatus;
import org.springframework.data.domain.Page;

import java.util.Optional;
import java.util.UUID;

public interface CampaignRepositoryPort {
    Campaign save(Campaign campaign);
    Optional<Campaign> findById(UUID id);
    Page<Campaign> findBySellerId(UUID sellerId, CampaignStatus status, int page, int size);
    Page<Campaign> findAll(CampaignStatus status, int page, int size);
    void deleteById(UUID id);
}

