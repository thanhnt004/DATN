package com.example.bannerservice.application.service;

import com.example.bannerservice.application.command.CreatePositionCommand;
import com.example.bannerservice.application.command.UpdatePositionCommand;
import com.example.bannerservice.application.port.in.ManagePositionUseCase;
import com.example.bannerservice.application.port.out.BannerPositionRepositoryPort;
import com.example.bannerservice.domain.exception.BannerBusinessException;
import com.example.bannerservice.domain.exception.BannerErrorCode;
import com.example.bannerservice.domain.model.BannerPosition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagePositionService implements ManagePositionUseCase {

    private final BannerPositionRepositoryPort positionRepo;

    @Override
    @Transactional
    public BannerPosition createPosition(CreatePositionCommand cmd) {
        if (positionRepo.existsByCode(cmd.getCode())) {
            throw new BannerBusinessException(BannerErrorCode.POSITION_ALREADY_EXISTS);
        }

        BannerPosition position = BannerPosition.builder()
                .code(cmd.getCode().toUpperCase())
                .name(cmd.getName())
                .description(cmd.getDescription())
                .maxBanners(cmd.getMaxBanners() != null ? cmd.getMaxBanners() : 10)
                .isActive(true)
                .createdAt(Instant.now())
                .build();

        return positionRepo.save(position);
    }

    @Override
    @Transactional
    public BannerPosition updatePosition(UpdatePositionCommand cmd) {
        BannerPosition position = positionRepo.findByCode(cmd.getCode())
                .orElseThrow(() -> new BannerBusinessException(BannerErrorCode.POSITION_NOT_FOUND));

        if (cmd.getName() != null) position.setName(cmd.getName());
        if (cmd.getDescription() != null) position.setDescription(cmd.getDescription());
        if (cmd.getMaxBanners() != null) position.setMaxBanners(cmd.getMaxBanners());
        if (cmd.getIsActive() != null) position.setIsActive(cmd.getIsActive());

        return positionRepo.save(position);
    }

    @Override
    @Transactional
    public void deletePosition(String code) {
        if (!positionRepo.existsByCode(code)) {
            throw new BannerBusinessException(BannerErrorCode.POSITION_NOT_FOUND);
        }
        positionRepo.deleteByCode(code);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerPosition> getAllPositions() {
        return positionRepo.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerPosition> getActivePositions() {
        return positionRepo.findAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public BannerPosition getPositionByCode(String code) {
        return positionRepo.findByCode(code)
                .orElseThrow(() -> new BannerBusinessException(BannerErrorCode.POSITION_NOT_FOUND));
    }
}

