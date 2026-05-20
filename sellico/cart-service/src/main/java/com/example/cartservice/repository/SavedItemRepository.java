package com.example.cartservice.repository;

import com.example.cartservice.entity.SavedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavedItemRepository extends JpaRepository<SavedItem, UUID> {

    List<SavedItem> findAllByUserId(UUID userId);

    Optional<SavedItem> findByUserIdAndSkuId(UUID userId, UUID skuId);

    boolean existsByUserIdAndSkuId(UUID userId, UUID skuId);

    void deleteByUserIdAndSkuId(UUID userId, UUID skuId);

    long countByUserId(UUID userId);
}

