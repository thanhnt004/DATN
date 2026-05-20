package com.example.orderservice.infrastructure.persistence.repository;

import com.example.orderservice.domain.model.OutboxEvent.OutboxStatus;
import com.example.orderservice.infrastructure.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    @Query("SELECT o FROM OutboxEventJpaEntity o WHERE o.status = 'PENDING' ORDER BY o.sequenceNumber ASC")
    List<OutboxEventJpaEntity> findPendingEvents();

    @Query("SELECT o FROM OutboxEventJpaEntity o WHERE o.status = 'PENDING' ORDER BY o.sequenceNumber ASC LIMIT :limit")
    List<OutboxEventJpaEntity> findPendingEventsWithLimit(@Param("limit") int limit);

    @Modifying
    @Query("UPDATE OutboxEventJpaEntity o SET o.status = :status, o.publishedAt = :publishedAt WHERE o.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") OutboxStatus status, @Param("publishedAt") Instant publishedAt);

    @Modifying
    @Query("DELETE FROM OutboxEventJpaEntity o WHERE o.status = 'PUBLISHED' AND o.publishedAt < :cutoffTime")
    void deletePublishedEventsBefore(@Param("cutoffTime") Instant cutoffTime);
}

