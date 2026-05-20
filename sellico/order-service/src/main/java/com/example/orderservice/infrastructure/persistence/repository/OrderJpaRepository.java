package com.example.orderservice.infrastructure.persistence.repository;

import com.example.orderservice.domain.model.enums.OrderStatus;
import com.example.orderservice.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    Optional<OrderJpaEntity> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM OrderJpaEntity o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<OrderJpaEntity> findByIdWithItems(@Param("id") UUID id);

    Page<OrderJpaEntity> findAllByUserId(UUID userId, Pageable pageable);

    Page<OrderJpaEntity> findAllByUserIdAndStatus(UUID userId, OrderStatus status, Pageable pageable);

    Page<OrderJpaEntity> findAllBySellerId(UUID sellerId, Pageable pageable);

    Page<OrderJpaEntity> findAllBySellerIdAndStatus(UUID sellerId, OrderStatus status, Pageable pageable);

    Page<OrderJpaEntity> findAllBySellerIdAndCreatedAtBetween(UUID sellerId, Instant startDate, Instant endDate, Pageable pageable);

    Page<OrderJpaEntity> findAllBySellerIdAndStatusAndCreatedAtBetween(UUID sellerId, OrderStatus status, Instant startDate, Instant endDate, Pageable pageable);

    long countByUserId(UUID userId);

    long countByUserIdAndStatus(UUID userId, OrderStatus status);

    long countBySellerId(UUID sellerId);

    long countBySellerIdAndStatus(UUID sellerId, OrderStatus status);

    @Query("SELECT COUNT(o) FROM OrderJpaEntity o WHERE o.sellerId = :sellerId AND o.createdAt BETWEEN :startDate AND :endDate")
    long countBySellerIdAndCreatedAtBetween(@Param("sellerId") UUID sellerId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(o) FROM OrderJpaEntity o WHERE o.sellerId = :sellerId AND o.status = :status AND o.createdAt BETWEEN :startDate AND :endDate")
    long countBySellerIdAndStatusAndCreatedAtBetween(@Param("sellerId") UUID sellerId, @Param("status") OrderStatus status, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT o.status, COUNT(o) FROM OrderJpaEntity o GROUP BY o.status")
    List<Object[]> countOrdersByStatus();

    @Query("SELECT o.status, SUM(o.totalAmount) FROM OrderJpaEntity o GROUP BY o.status")
    List<Object[]> sumRevenueByStatus();

    @Query("SELECT SUM(o.totalAmount) FROM OrderJpaEntity o WHERE o.status = 'COMPLETED'")
    BigDecimal sumTotalCompletedRevenue();

    @Query("SELECT SUM(o.totalAmount) FROM OrderJpaEntity o WHERE o.sellerId = :sellerId AND o.status = 'COMPLETED'")
    BigDecimal sumSellerCompletedRevenue(@Param("sellerId") UUID sellerId);

    @Query("SELECT CAST(o.createdAt AS date) as date, SUM(CASE WHEN o.status = 'COMPLETED' THEN o.totalAmount ELSE 0 END), COUNT(o) " +
           "FROM OrderJpaEntity o " +
           "WHERE o.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY CAST(o.createdAt AS date) " +
           "ORDER BY CAST(o.createdAt AS date) ASC")
    List<Object[]> getDailyRevenueReport(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT CAST(o.createdAt AS date) as date, SUM(CASE WHEN o.status = 'COMPLETED' THEN o.totalAmount ELSE 0 END), COUNT(o) " +
            "FROM OrderJpaEntity o " +
            "WHERE o.sellerId = :sellerId AND o.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY CAST(o.createdAt AS date) " +
            "ORDER BY CAST(o.createdAt AS date) ASC")
    List<Object[]> getSellerDailyRevenueReport(@Param("sellerId") UUID sellerId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT o FROM OrderJpaEntity o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<OrderJpaEntity> findAllByCreatedAtBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT o FROM OrderJpaEntity o WHERE o.sellerId = :sellerId AND o.createdAt BETWEEN :startDate AND :endDate")
    List<OrderJpaEntity> findAllBySellerIdAndCreatedAtBetween(@Param("sellerId") UUID sellerId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    List<OrderJpaEntity> findAllByIdIn(List<UUID> orderIds);
    @Query("SELECT o.status, COUNT(o) FROM OrderJpaEntity o WHERE o.sellerId = :sellerId GROUP BY o.status")
    List<Object[]> countSellerOrdersByStatus(@Param("sellerId") UUID sellerId);

    @Query("SELECT o.status, SUM(o.totalAmount) FROM OrderJpaEntity o WHERE o.sellerId = :sellerId GROUP BY o.status")
    List<Object[]> sumSellerRevenueByStatus(@Param("sellerId") UUID sellerId);
}

