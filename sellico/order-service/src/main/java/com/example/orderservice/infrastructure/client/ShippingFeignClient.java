package com.example.orderservice.infrastructure.client;

import com.example.orderservice.domain.model.valueobject.ShippingAddress;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "shipping-service",fallback = ShippingFeignClientFallback.class)
public interface ShippingFeignClient {

    @PostMapping("/api/v1/internal/shipping/calculate-fee")
    GHNFeeResponse calculateFee(@RequestBody ShippingFeeRequest request);

    @Data
    @Builder
    class ShippingFeeRequest {

        private UUID sellerId;
        private String ward;
        private String district;
        private String city;
        private Integer weight;
        private Integer length;
        private Integer width;
        private Integer height;
        public static ShippingFeeRequest create(ShippingAddress shippingAddress,UUID sellerId,Integer weight,Integer length,Integer width,Integer height) {
            return ShippingFeeRequest.builder()
                    .sellerId(sellerId)
                    .ward(shippingAddress.ward())
                    .district(shippingAddress.district())
                    .city(shippingAddress.city())
                    .weight(weight)
                    .length(length)
                    .width(width)
                    .height(height)
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    class GHNFeeResponse {
        @JsonProperty("total")
        private int total;
        @JsonProperty("service_fee")
        private int serviceFee;
        @JsonProperty("insurance_fee")
        private int insuranceFee;
        @JsonProperty("pick_station_fee")
        private int pickStationFee;
        @JsonProperty("coupon_value")
        private int couponValue;
        @JsonProperty("r2s_fee")
        private int r2sFee;
    }
}
