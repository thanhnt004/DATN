package com.example.shippingservice.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GHNFeeResponse {
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
