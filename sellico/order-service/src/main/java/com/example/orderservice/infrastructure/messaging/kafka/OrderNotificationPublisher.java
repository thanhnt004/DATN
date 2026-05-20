package com.example.orderservice.infrastructure.messaging.kafka;

import com.example.orderservice.domain.model.Order;
import com.example.orderservice.domain.model.OrderItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import event.EventMetadata;
import event.EventWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes order notification events to the "order-events" topic
 * in EventWrapper format for notification-service consumption.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationPublisher {

    private final KafkaEventProducer kafkaProducer;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "order-events";

    public void publishOrderCreated(Order order, String buyerEmail, String buyerName) {
        try {
            // Build items list
            List<Map<String, Object>> items = order.getItems().stream()
                    .map(item -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("productId", item.getProductId().toString());
                        m.put("productName", item.getProductName());
                        m.put("quantity", item.getQuantity());
                        m.put("price", item.getUnitPrice().amount());
                        m.put("imageUrl", item.getImageUrl());
                        m.put("variantInfo", item.getVariantInfo());
                        m.put("subtotal", item.getSubtotal().amount());
                        return m;
                    })
                    .toList();

            // Build shipping address string
            var addr = order.getShippingAddress();
            String shippingAddress = String.join(", ",
                    addr.address(),
                    addr.ward() != null ? addr.ward() : "",
                    addr.district() != null ? addr.district() : "",
                    addr.city() != null ? addr.city() : ""
            ).replaceAll(",\\s*,", ",").replaceAll("^,\\s*|,\\s*$", "");

            // Build payload
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("orderId", order.getId().value().toString());
            payload.put("orderCode", order.getOrderNumber());
            payload.put("userId", order.getUserId().toString());
            payload.put("email", buyerEmail);
            payload.put("customerName", buyerName != null ? buyerName : addr.recipientName());
            payload.put("recipientName", addr.recipientName());
            payload.put("recipientPhone", addr.recipientPhone());
            payload.put("totalAmount", order.getTotalAmount().amount());
            payload.put("subtotal", order.getSubtotal().amount());
            payload.put("shippingFee", order.getShippingFee() != null ? order.getShippingFee().amount() : 0);
            payload.put("discountAmount", order.getDiscountAmount() != null ? order.getDiscountAmount().amount() : 0);
            payload.put("shippingAddress", shippingAddress);
            payload.put("paymentMethod", order.getPaymentMethod().name());
            payload.put("items", items);

            // Wrap in EventWrapper
            EventMetadata metadata = new EventMetadata(
                    UUID.randomUUID().toString(),
                    "ORDER_CREATED",
                    "order-service",
                    Instant.now()
            );
            EventWrapper<Map<String, Object>> wrapper = new EventWrapper<>(metadata, payload);

            kafkaProducer.send(TOPIC, order.getId().value().toString(), wrapper);

            log.info("[Notification] Published ORDER_CREATED event for order {} to {}", order.getOrderNumber(), TOPIC);
        } catch (Exception e) {
            log.error("[Notification] Failed to publish ORDER_CREATED event for order {}: {}",
                    order.getOrderNumber(), e.getMessage(), e);
        }
    }
}
