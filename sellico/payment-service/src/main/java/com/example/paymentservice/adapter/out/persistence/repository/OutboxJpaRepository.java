package com.example.paymentservice.adapter.out.persistence.repository;

import com.example.paymentservice.adapter.out.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    @Query(value = "SELECT * FROM outbox_events WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT :limit",
           nativeQuery = true)
    List<OutboxEventJpaEntity> findPendingEventsWithLimit(@Param("limit") int limit);

    @Modifying
    @Query("DELETE FROM OutboxEventJpaEntity e WHERE e.status = 'PUBLISHED' AND e.publishedAt < :cutoff")
    void deletePublishedEventsBefore(@Param("cutoff") Instant cutoff);
}

