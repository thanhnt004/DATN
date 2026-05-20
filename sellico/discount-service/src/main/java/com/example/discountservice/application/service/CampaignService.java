package com.example.discountservice.application.service;

import com.example.discountservice.application.port.in.CampaignUseCase;
import com.example.discountservice.application.port.out.CampaignRepositoryPort;
import com.example.discountservice.domain.exception.DiscountBusinessException;
import com.example.discountservice.domain.exception.DiscountErrorCode;
import com.example.discountservice.domain.model.Campaign;
import com.example.discountservice.domain.model.enums.CampaignStatus;
import com.example.discountservice.domain.model.enums.CampaignType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignService implements CampaignUseCase {

    private final CampaignRepositoryPort campaignRepo;

    @Override
    @Transactional
    public Campaign createCampaign(CreateCampaignCommand command) {
        Instant start = Instant.parse(command.startDate());
        Instant end = Instant.parse(command.endDate());

        if (!end.isAfter(start)) {
            throw new DiscountBusinessException(DiscountErrorCode.INVALID_DATE_RANGE);
        }

        Campaign campaign = Campaign.builder()
                .sellerId(command.sellerId())
                .name(command.name())
                .description(command.description())
                .campaignType(CampaignType.valueOf(command.campaignType()))
                .status(CampaignStatus.DRAFT)
                .startDate(start)
                .endDate(end)
                .build();

        Campaign saved = campaignRepo.save(campaign);
        log.info("Created campaign: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Campaign updateCampaign(UUID id, UpdateCampaignCommand command) {
        Campaign campaign = campaignRepo.findById(id)
                .orElseThrow(() -> new DiscountBusinessException(DiscountErrorCode.CAMPAIGN_NOT_FOUND));

        if (command.name() != null) campaign.setName(command.name());
        if (command.description() != null) campaign.setDescription(command.description());
        if (command.status() != null) campaign.setStatus(CampaignStatus.valueOf(command.status()));
        if (command.startDate() != null) campaign.setStartDate(Instant.parse(command.startDate()));
        if (command.endDate() != null) campaign.setEndDate(Instant.parse(command.endDate()));

        return campaignRepo.save(campaign);
    }

    @Override
    @Transactional(readOnly = true)
    public Campaign getCampaign(UUID id) {
        return campaignRepo.findById(id)
                .orElseThrow(() -> new DiscountBusinessException(DiscountErrorCode.CAMPAIGN_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Campaign> listCampaigns(UUID sellerId, String status, int page, int size) {
        CampaignStatus cs = (status != null) ? CampaignStatus.valueOf(status) : null;
        if (sellerId != null) {
            return campaignRepo.findBySellerId(sellerId, cs, page, size);
        }
        return campaignRepo.findAll(cs, page, size);
    }

    @Override
    @Transactional
    public void deleteCampaign(UUID id, UUID sellerId) {
        Campaign campaign = campaignRepo.findById(id)
                .orElseThrow(() -> new DiscountBusinessException(DiscountErrorCode.CAMPAIGN_NOT_FOUND));
        if (sellerId != null && !sellerId.equals(campaign.getSellerId())) {
            throw new DiscountBusinessException(DiscountErrorCode.SELLER_MISMATCH);
        }
        campaignRepo.deleteById(id);
    }
}

