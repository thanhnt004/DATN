package com.example.discountservice.adapter.out.persistence;

import com.example.discountservice.adapter.out.persistence.entity.CampaignEntity;
import com.example.discountservice.adapter.out.persistence.repository.CampaignJpaRepository;
import com.example.discountservice.application.port.out.CampaignRepositoryPort;
import com.example.discountservice.domain.model.Campaign;
import com.example.discountservice.domain.model.enums.CampaignStatus;
import com.example.discountservice.domain.model.enums.CampaignType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CampaignJpaAdapter implements CampaignRepositoryPort {

    private final CampaignJpaRepository repo;

    @Override
    public Campaign save(Campaign c) {
        CampaignEntity entity = toEntity(c);
        return toDomain(repo.save(entity));
    }

    @Override
    public Optional<Campaign> findById(UUID id) {
        return repo.findById(id).map(this::toDomain);
    }

    @Override
    public Page<Campaign> findBySellerId(UUID sellerId, CampaignStatus status, int page, int size) {
        return repo.search(sellerId, status != null ? status.name() : null, PageRequest.of(page, size))
                .map(this::toDomain);
    }

    @Override
    public Page<Campaign> findAll(CampaignStatus status, int page, int size) {
        return repo.search(null, status != null ? status.name() : null, PageRequest.of(page, size))
                .map(this::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repo.deleteById(id);
    }

    private CampaignEntity toEntity(Campaign c) {
        return CampaignEntity.builder()
                .id(c.getId()).sellerId(c.getSellerId()).name(c.getName())
                .description(c.getDescription())
                .campaignType(c.getCampaignType() != null ? c.getCampaignType().name() : null)
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .startDate(c.getStartDate()).endDate(c.getEndDate())
                .build();
    }

    private Campaign toDomain(CampaignEntity e) {
        return Campaign.builder()
                .id(e.getId()).sellerId(e.getSellerId()).name(e.getName())
                .description(e.getDescription())
                .campaignType(e.getCampaignType() != null ? CampaignType.valueOf(e.getCampaignType()) : null)
                .status(e.getStatus() != null ? CampaignStatus.valueOf(e.getStatus()) : null)
                .startDate(e.getStartDate()).endDate(e.getEndDate())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }
}

