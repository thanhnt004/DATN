package com.example.orderservice.domain.model;

import com.example.orderservice.domain.model.valueobject.Money;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

/**
 * Order Item - Domain Entity
 */
@Getter
@Builder
public class OrderItem {
    private UUID id;
    private UUID skuId;
    private UUID productId;
    private String productName;
    private String skuCode;
    private String imageUrl;
    private Money unitPrice;
    private Integer quantity;
    private Money subtotal;
    private Money discountAmount;
    private Map<String, String> variantInfo;

    public static OrderItem create(UUID skuId, UUID productId, String productName,
                                    String skuCode, String imageUrl, Money unitPrice,
                                    Integer quantity, Map<String, String> variantInfo) {
        Money subtotal = unitPrice.multiply(quantity);

        return OrderItem.builder()
                .id(UUID.randomUUID())
                .skuId(skuId)
                .productId(productId)
                .productName(productName)
                .skuCode(skuCode)
                .imageUrl(imageUrl)
                .unitPrice(unitPrice)
                .quantity(quantity)
                .subtotal(subtotal)
                .discountAmount(Money.ZERO)
                .variantInfo(variantInfo)
                .build();
    }

    public void applyDiscount(Money discount) {
        this.discountAmount = discount;
        this.subtotal = unitPrice.multiply(quantity).subtract(discount);
    }
}

