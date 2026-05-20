package com.example.orderservice.infrastructure.persistence.mapper;

import com.example.orderservice.domain.model.Order;
import com.example.orderservice.domain.model.OrderItem;
import com.example.orderservice.domain.model.valueobject.Money;
import com.example.orderservice.domain.model.valueobject.OrderId;
import com.example.orderservice.domain.model.valueobject.ShippingAddress;
import com.example.orderservice.infrastructure.persistence.entity.OrderItemJpaEntity;
import com.example.orderservice.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderPersistenceMapper {

    public OrderJpaEntity toEntity(Order order) {
        OrderJpaEntity entity = OrderJpaEntity.builder()
                .id(order.getId().value())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .sellerId(order.getSellerId())
                .status(order.getStatus())
                .subtotal(order.getSubtotal().amount())
                .shippingFee(order.getShippingFee().amount())
                .discountAmount(order.getDiscountAmount().amount())
                .totalAmount(order.getTotalAmount().amount())
                .recipientName(order.getShippingAddress().recipientName())
                .recipientPhone(order.getShippingAddress().recipientPhone())
                .shippingAddress(order.getShippingAddress().address())
                .shippingWard(order.getShippingAddress().ward())
                .shippingDistrict(order.getShippingAddress().district())
                .shippingCity(order.getShippingAddress().city())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .paidAt(order.getPaidAt())
                .shippingProvider(order.getShippingProvider())
                .trackingNumber(order.getTrackingNumber())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .buyerNote(order.getBuyerNote())
                .sellerNote(order.getSellerNote())
                .cancelReason(order.getCancelReason())
                .failureReason(order.getFailureReason())
                .voucherCode(order.getVoucherCode())
                .platformCouponId(order.getPlatformCouponId())
                .platformVoucherShare(
                        order.getPlatformVoucherShare() != null
                                ? order.getPlatformVoucherShare().amount()
                                : java.math.BigDecimal.ZERO)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .confirmedAt(order.getConfirmedAt())
                .completedAt(order.getCompletedAt())
                .cancelledAt(order.getCancelledAt())
                .version(order.getVersion())
                .build();

        // Map items
        order.getItems().forEach(item -> {
            OrderItemJpaEntity itemEntity = toItemEntity(item);
            entity.addItem(itemEntity);
        });

        return entity;
    }

    public Order toDomain(OrderJpaEntity entity) {
        ShippingAddress shippingAddress = new ShippingAddress(
                entity.getRecipientName(),
                entity.getRecipientPhone(),
                entity.getShippingAddress(),
                entity.getShippingWard(),
                entity.getShippingDistrict(),
                entity.getShippingCity()
        );

        List<OrderItem> items = entity.getItems().stream()
                .map(this::toItemDomain)
                .collect(Collectors.toList());

        return Order.builder()
                .id(new OrderId(entity.getId()))
                .orderNumber(entity.getOrderNumber())
                .userId(entity.getUserId())
                .sellerId(entity.getSellerId())
                .status(entity.getStatus())
                .subtotal(Money.of(entity.getSubtotal()))
                .shippingFee(Money.of(entity.getShippingFee()))
                .discountAmount(Money.of(entity.getDiscountAmount()))
                .totalAmount(Money.of(entity.getTotalAmount()))
                .shippingAddress(shippingAddress)
                .paymentMethod(entity.getPaymentMethod())
                .paymentStatus(entity.getPaymentStatus())
                .paidAt(entity.getPaidAt())
                .shippingProvider(entity.getShippingProvider())
                .trackingNumber(entity.getTrackingNumber())
                .shippedAt(entity.getShippedAt())
                .deliveredAt(entity.getDeliveredAt())
                .buyerNote(entity.getBuyerNote())
                .sellerNote(entity.getSellerNote())
                .cancelReason(entity.getCancelReason())
                .failureReason(entity.getFailureReason())
                .voucherCode(entity.getVoucherCode())
                .platformCouponId(entity.getPlatformCouponId())
                .platformVoucherShare(
                        entity.getPlatformVoucherShare() != null
                                ? Money.of(entity.getPlatformVoucherShare())
                                : Money.ZERO)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .confirmedAt(entity.getConfirmedAt())
                .completedAt(entity.getCompletedAt())
                .cancelledAt(entity.getCancelledAt())
                .items(items)
                .version(entity.getVersion())
                .build();
    }

    private OrderItemJpaEntity toItemEntity(OrderItem item) {
        return OrderItemJpaEntity.builder()
                .id(item.getId())
                .skuId(item.getSkuId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .skuCode(item.getSkuCode())
                .imageUrl(item.getImageUrl())
                .unitPrice(item.getUnitPrice().amount())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal().amount())
                .discountAmount(item.getDiscountAmount().amount())
                .variantInfo(item.getVariantInfo())
                .build();
    }

    private OrderItem toItemDomain(OrderItemJpaEntity entity) {
        return OrderItem.builder()
                .id(entity.getId())
                .skuId(entity.getSkuId())
                .productId(entity.getProductId())
                .productName(entity.getProductName())
                .skuCode(entity.getSkuCode())
                .imageUrl(entity.getImageUrl())
                .unitPrice(Money.of(entity.getUnitPrice()))
                .quantity(entity.getQuantity())
                .subtotal(Money.of(entity.getSubtotal()))
                .discountAmount(Money.of(entity.getDiscountAmount()))
                .variantInfo(entity.getVariantInfo())
                .build();
    }
}

