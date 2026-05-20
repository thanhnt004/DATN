package com.example.discountservice.adapter.out.persistence;

import com.example.discountservice.adapter.out.persistence.entity.UserCouponClaimEntity;
import com.example.discountservice.adapter.out.persistence.repository.UserCouponClaimJpaRepository;
import com.example.discountservice.application.port.out.UserCouponClaimRepositoryPort;
import com.example.discountservice.domain.model.UserCouponClaim;
import com.example.discountservice.domain.model.enums.ClaimStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserCouponClaimJpaAdapter implements UserCouponClaimRepositoryPort {

    private final UserCouponClaimJpaRepository repo;

    @Override
    public UserCouponClaim save(UserCouponClaim c) { return toDomain(repo.save(toEntity(c))); }

    @Override
    public Optional<UserCouponClaim> findById(UUID id) { return repo.findById(id).map(this::toDomain); }

    @Override
    public Optional<UserCouponClaim> findByCouponIdAndUserIdAndStatus(UUID couponId, UUID userId, ClaimStatus status) {
        return repo.findByCouponIdAndUserIdAndStatus(couponId, userId, status.name()).map(this::toDomain);
    }

    @Override
    public List<UserCouponClaim> findByUserId(UUID userId, ClaimStatus status, int page, int size) {
        return repo.findByUserIdAndStatusOrderByClaimedAtDesc(userId, status.name(), PageRequest.of(page, size))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long countByCouponIdAndUserId(UUID couponId, UUID userId) {
        return repo.countByCouponIdAndUserId(couponId, userId);
    }

    @Override
    public boolean existsByCouponIdAndUserIdAndStatus(UUID couponId, UUID userId, ClaimStatus status) {
        return repo.existsByCouponIdAndUserIdAndStatus(couponId, userId, status.name());
    }

    private UserCouponClaimEntity toEntity(UserCouponClaim c) {
        return UserCouponClaimEntity.builder()
                .id(c.getId()).couponId(c.getCouponId()).userId(c.getUserId())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .claimedAt(c.getClaimedAt()).usedAt(c.getUsedAt()).build();
    }

    private UserCouponClaim toDomain(UserCouponClaimEntity e) {
        return UserCouponClaim.builder()
                .id(e.getId()).couponId(e.getCouponId()).userId(e.getUserId())
                .status(e.getStatus() != null ? ClaimStatus.valueOf(e.getStatus()) : null)
                .claimedAt(e.getClaimedAt()).usedAt(e.getUsedAt()).build();
    }
}

