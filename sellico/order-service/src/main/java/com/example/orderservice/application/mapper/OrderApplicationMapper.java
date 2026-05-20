package com.example.orderservice.application.mapper;

import com.example.orderservice.application.dto.response.OrderItemResponse;
import com.example.orderservice.application.dto.response.OrderResponse;
import com.example.orderservice.domain.model.Order;
import com.example.orderservice.domain.model.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderApplicationMapper {

    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
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
                .items(toItemResponses(order.getItems()))
                .build();
    }

    private List<OrderItemResponse> toItemResponses(List<OrderItem> items) {
        return items.stream()
                .map(this::toItemResponse)
                .toList();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
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
}

