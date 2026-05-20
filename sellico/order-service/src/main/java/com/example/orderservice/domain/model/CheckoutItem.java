package com.example.orderservice.domain.model;

import com.example.orderservice.domain.model.valueobject.Money;
import com.example.orderservice.infrastructure.client.InventoryFeignClient;
import com.example.orderservice.infrastructure.client.ProductFeignClient;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckoutItem {
    private UUID skuId;
    private UUID productId;
    private String productName;
    private String skuCode;
    private String imageUrl;
    private Money unitPrice;
    private Integer quantity;
    private Money subtotal;

    private Integer availableStock;
    private Boolean isAvailable;
    private Boolean isLowStock;

    private Integer weightGram;
    private Integer widthCm;
    private Integer lengthCm;
    private Integer heightCm;

    private Map<String, String> variantInfo;

    public void updateQuantity(Integer newQuantity, InventoryFeignClient.StockAvailabilityResponse stockAvailability) {
        this.quantity = newQuantity;
        this.subtotal = this.unitPrice.multiply(newQuantity);
        this.availableStock = stockAvailability.getAvailableStock();
        this.isAvailable = stockAvailability.getIsAvailable();
        this.isLowStock = stockAvailability.getIsLowStock();
    }

    public static CheckoutItem create(ProductFeignClient.SkuDetailResponse skuDetailResponse, Integer quantity, InventoryFeignClient.StockAvailabilityResponse stockAvailability) {
        return CheckoutItem.builder()
                .skuId(skuDetailResponse.getId())
                .productId(skuDetailResponse.getProductId())
                .productName(skuDetailResponse.getProductName())
                .skuCode(skuDetailResponse.getSkuCode())
                .imageUrl(skuDetailResponse.getImageUrl())
                .unitPrice(Money.of(skuDetailResponse.getPrice()))
                .quantity(quantity)
                .subtotal(Money.of(skuDetailResponse.getPrice()).multiply(quantity))
                .availableStock(stockAvailability.getAvailableStock())
                .isAvailable(stockAvailability.getIsAvailable())
                .isLowStock(stockAvailability.getIsLowStock())
                .weightGram(skuDetailResponse.getWeightGram())
                .widthCm(skuDetailResponse.getWidthCm())
                .lengthCm(skuDetailResponse.getLengthCm())
                .heightCm(skuDetailResponse.getHeightCm())
                .build();
    }
}
