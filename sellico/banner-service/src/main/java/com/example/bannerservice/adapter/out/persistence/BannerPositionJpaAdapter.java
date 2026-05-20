package com.example.bannerservice.adapter.out.persistence;

import com.example.bannerservice.adapter.out.persistence.mapper.BannerPersistenceMapper;
import com.example.bannerservice.adapter.out.persistence.repository.BannerPositionJpaRepository;
import com.example.bannerservice.application.port.out.BannerPositionRepositoryPort;
import com.example.bannerservice.domain.model.BannerPosition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BannerPositionJpaAdapter implements BannerPositionRepositoryPort {

    private final BannerPositionJpaRepository repo;
    private final BannerPersistenceMapper mapper;

    @Override
    public BannerPosition save(BannerPosition position) {
        return mapper.toDomain(repo.save(mapper.toEntity(position)));
    }

    @Override
    public Optional<BannerPosition> findByCode(String code) {
        return repo.findById(code).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return repo.existsByCode(code);
    }

    @Override
    public List<BannerPosition> findAll() {
        return repo.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<BannerPosition> findAllActive() {
        return repo.findByIsActiveTrue().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteByCode(String code) {
        repo.deleteById(code);
    }
}

