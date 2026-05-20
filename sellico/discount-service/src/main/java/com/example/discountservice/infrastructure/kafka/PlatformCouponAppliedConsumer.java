package com.example.discountservice.infrastructure.kafka;

import com.example.discountservice.application.port.in.CouponUseCase;
import com.example.discountservice.application.port.in.CouponUseCase.OrderShare;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Consumes the PlatformCouponAppliedEvent published by order-service via the
 * Outbox/Kafka pipeline. Marks the user's coupon claim USED and writes one
 * usage-history row per pro-rated allocation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformCouponAppliedConsumer {

    private final CouponUseCase couponUseCase;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "discount.platform-coupon.applied", groupId = "discount-service")
    public void onPlatformCouponApplied(String payload) {
        log.info("Received discount.platform-coupon.applied: {}", payload);
        try {
            JsonNode root = objectMapper.readTree(payload);
            UUID couponId = UUID.fromString(root.get("couponId").asText());
            UUID userId = UUID.fromString(root.get("userId").asText());

            List<OrderShare> allocations = new ArrayList<>();
            JsonNode allocs = root.get("allocations");
            if (allocs != null && allocs.isArray()) {
                for (JsonNode a : allocs) {
                    allocations.add(new OrderShare(
                            UUID.fromString(a.get("orderId").asText()),
                            new BigDecimal(a.get("share").asText())
                    ));
                }
            }

            couponUseCase.applyPlatformCouponMulti(couponId, userId, allocations);
        } catch (Exception e) {
            log.error("Failed to process platform-coupon.applied event", e);
            throw new RuntimeException(e); // let DefaultErrorHandler retry
        }
    }
}
