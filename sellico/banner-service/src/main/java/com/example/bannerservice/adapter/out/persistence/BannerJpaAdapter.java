package com.example.bannerservice.adapter.out.persistence;

import com.example.bannerservice.adapter.out.persistence.entity.BannerEntity;
import com.example.bannerservice.adapter.out.persistence.mapper.BannerPersistenceMapper;
import com.example.bannerservice.adapter.out.persistence.repository.BannerJpaRepository;
import com.example.bannerservice.application.port.out.BannerRepositoryPort;
import com.example.bannerservice.domain.model.Banner;
import com.example.bannerservice.domain.model.BannerStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BannerJpaAdapter implements BannerRepositoryPort {

    private final BannerJpaRepository repo;
    private final BannerPersistenceMapper mapper;

    @Override
    public Banner save(Banner banner) {
        BannerEntity entity = mapper.toEntity(banner);
        return mapper.toDomain(repo.save(entity));
    }

    @Override
    public Optional<Banner> findById(UUID id) {
        return repo.findById(id).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Banner> findAllWithFilters(String positionCode, BannerStatus status,
                                           int page, int size, String sortBy, String sortDirection) {
        Sort sort = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String statusStr = status != null ? status.name() : null;
        return repo.findAllWithFilters(positionCode, statusStr, pageable).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Banner> findActiveByPosition(String positionCode) {
        return repo.findActiveByPosition(positionCode, Instant.now()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public int countActiveByPosition(String positionCode) {
        return repo.countActiveByPosition(positionCode);
    }

    @Override
    @Transactional
    public void incrementClickCount(UUID id) {
        repo.incrementClickCount(id);
    }

    @Override
    @Transactional
    public void incrementViewCount(UUID id) {
        repo.incrementViewCount(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Banner> findScheduledBannersToActivate() {
        return repo.findScheduledToActivate(Instant.now()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Banner> findExpiredBanners() {
        return repo.findExpired(Instant.now()).stream()
                .map(mapper::toDomain)
                .toList();
    }
}

