package com.example.orderservice.application.service;

import com.example.orderservice.application.dto.command.CreateCheckoutSessionCommand;
import com.example.orderservice.application.dto.command.ItemCommand;
import com.example.orderservice.application.dto.response.CheckoutItemResponse;
import com.example.orderservice.application.dto.response.CheckoutSessionResponse;
import com.example.orderservice.application.dto.response.SellerSessionResponse;
import com.example.orderservice.application.dto.response.ShippingAddressResponse;
import com.example.orderservice.application.port.input.CreateCheckOutSessionUseCase;
import com.example.orderservice.domain.model.CheckoutItem;
import com.example.orderservice.domain.model.CheckoutSession;
import com.example.orderservice.domain.model.SellerSession;
import com.example.orderservice.domain.model.valueobject.Money;
import com.example.orderservice.domain.model.valueobject.ShippingAddress;
import com.example.orderservice.infrastructure.client.DiscountFeignClient;
import com.example.orderservice.infrastructure.client.InventoryFeignClient;
import com.example.orderservice.infrastructure.client.ProductFeignClient;
import com.example.orderservice.infrastructure.client.ShippingFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreateCheckoutSessionService implements CreateCheckOutSessionUseCase {
    private final InventoryFeignClient inventoryFeignClientClient;
    private final ProductFeignClient productFeignClient;
    private final DiscountFeignClient discountFeignClient;
    private final ShippingFeignClient shippingFeignClient;
    private final CheckoutSessionRedisService checkoutSessionRedisService;

    @Override
    public CheckoutSessionResponse createCheckOutSession(CreateCheckoutSessionCommand command) {
        //Generate checkou session
        ShippingAddress shippingAddress = new ShippingAddress(
                command.getRecipientName(),
                command.getRecipientPhone(),
                command.getShippingAddress(),
                command.getShippingWard(),
                command.getShippingDistrict(),
                command.getShippingCity()
        );
        //create check out session with userID, shippingAddress, voucherId, cartId
        CheckoutSession checkoutSession = CheckoutSession.create(command.getUserId(), shippingAddress, command.getVoucherId(), command.getCartId());
        //create seller mapping
        Map<UUID, String> buyerNotes = command.getBuyerNotes() != null ? command.getBuyerNotes() : Map.of();
        Map<UUID, UUID> sellerVoucherMap = command.getSellerVoucherID() != null ? command.getSellerVoucherID() : Map.of();
        //get skus info
        var skusResponse =  productFeignClient.getBatchSkus(ProductFeignClient.BatchSkusRequest.builder()
                        .skuIds(command.getItems().stream().map(ItemCommand::getSkuId).toList())
                .build());
        var skus = skusResponse.getData();
        Map<UUID, List<ProductFeignClient.SkuDetailResponse>> groupedBySeller =
                skus.stream()
                        .collect(Collectors.groupingBy(ProductFeignClient.SkuDetailResponse::getSellerId));
        Map<UUID,Integer> skusQuantityMap = command.getItems().stream()
                .collect(Collectors.toMap(
                        ItemCommand::getSkuId,
                        ItemCommand::getQuantity
                ));
        var skusInventoryResponse = inventoryFeignClientClient.getInventories(command.getItems().stream().map(ItemCommand::getSkuId).toList());
        var skusInventory = skusInventoryResponse.getData();
        Map<UUID, InventoryFeignClient.StockAvailabilityResponse> inventoriesMap =
                skusInventory.stream()
                        .collect(Collectors.toMap(
                                InventoryFeignClient.StockAvailabilityResponse::getSkuId,
                                Function.identity()
                        ));
        //add information to seller session
        for (Map.Entry<UUID, List<ProductFeignClient.SkuDetailResponse>> entry : groupedBySeller.entrySet()) {
            int totalWeight = 0;
            int totalLength = 0;
            int totalHeight = 0;
            int totalWith = 0;
            UUID sellerId = entry.getKey();
            String buyerNote = buyerNotes.getOrDefault(sellerId, "");

            UUID voucherId = sellerVoucherMap.getOrDefault(sellerId,null);
            SellerSession sellerSession = SellerSession.create(sellerId,voucherId,buyerNote);

            for (ProductFeignClient.SkuDetailResponse sku : entry.getValue()) {
                totalWeight+=sku.getWeightGram();
                totalWith+=sku.getWidthCm();
                totalLength+=sku.getLengthCm();
                totalHeight+=sku.getHeightCm();
                sellerSession.addItem(CheckoutItem.create(sku,skusQuantityMap.getOrDefault(sku.getId(),0),inventoriesMap.get(sku.getId())));
            }

            if (voucherId != null) {
                //check and calculate
                var coupon = discountFeignClient.validateCouponForSeller(DiscountFeignClient.ValidateCouponRequest.builder()
                        .couponId(voucherId)
                        .userId(command.getUserId())
                        .orderAmount(sellerSession.getTotalAmount().amount())
                        .sellerId(sellerId)
                        .build()).getData();
                if (!coupon.valid())
                    sellerSession.setVoucherId(null);
                else
                    sellerSession.applyDiscount(Money.of(coupon.discountAmount()));
            }
            //calculate shipping fee

            var shippingFee = shippingFeignClient.calculateFee(ShippingFeignClient.ShippingFeeRequest.create(shippingAddress,sellerId,totalWeight,totalLength,totalWith,totalHeight)).getTotal();
            sellerSession.setShippingFee(Money.of(shippingFee));

            checkoutSession.addSellerSession(sellerSession);
        }
        //apply flatform voucher
        if (command.getVoucherId() != null) {
            var couponResponse = discountFeignClient.validateCoupon(DiscountFeignClient.ValidateCouponRequest.builder()
                            .couponId(command.getVoucherId())
                            .userId(command.getUserId())
                            .orderAmount(checkoutSession.getTotalAmount().amount())
                    .build());
            var couponData = couponResponse.getData();
            if (couponData != null) {
                checkoutSession.applyDiscount(Money.of(couponData.discountAmount()));
            }
        }

        checkoutSessionRedisService.saveSession(checkoutSession.getId().toString(), checkoutSession);
        //build item responses
        List<CheckoutItemResponse> itemResponses = new ArrayList<>();
        checkoutSession.getSellerSessions().forEach(seller -> {
            seller.getItems().forEach(item -> {
                itemResponses.add(CheckoutItemResponse.builder()
                        .sellerId(seller.getSellerId())
                        .skuId(item.getSkuId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .skuCode(item.getSkuCode())
                        .imageUrl(item.getImageUrl())
                        .unitPrice(item.getUnitPrice().amount())
                        .quantity(item.getQuantity())
                        .variantInfo(item.getVariantInfo())
                        .build());
            });
        });
        // build seller responses
        var sellerResponses = checkoutSession.getSellerSessions().stream()
                .map(seller -> SellerSessionResponse.builder()
                        .sellerId(seller.getSellerId())
                        .voucherId(seller.getVoucherId())
                        .buyerNote(seller.getBuyerNote())
                        .totalAmount(seller.getTotalAmount().amount())
                        .shippingFee(seller.getShippingFee().amount())
                        .discount(seller.getDiscountAmount().amount())
                        .finalAmount(seller.getFinalAmount().amount())
                        .build())
                .toList();
        //build response
        return CheckoutSessionResponse.builder()
                .sessionId(checkoutSession.getId())
                .items(itemResponses)
                .shippingAddress(ShippingAddressResponse.builder()
                        .recipientName(checkoutSession.getShippingAddress().recipientName())
                        .recipientPhone(checkoutSession.getShippingAddress().recipientPhone())
                        .address(checkoutSession.getShippingAddress().address())
                        .ward(checkoutSession.getShippingAddress().ward())
                        .district(checkoutSession.getShippingAddress().district())
                        .city(checkoutSession.getShippingAddress().city())
                        .fullAddress(checkoutSession.getShippingAddress().getFullAddress())
                        .build())
                .sellerSessions(sellerResponses)
                .voucherId(checkoutSession.getVoucherId())
                .cartId(checkoutSession.getCartId())
                .totalAmount(checkoutSession.getTotalAmount().amount())
                .shippingFee(checkoutSession.getShippingFee().amount())
                .discount(checkoutSession.getDiscountAmount().amount())
                .finalAmount(checkoutSession.getFinalAmount().amount())
                .expiredAt(checkoutSession.getExpiredAt())
                .build();
    }
}
