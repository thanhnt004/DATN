package com.example.shippingservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ShippingOrderResponse {
    @JsonProperty("order_code")
    private String orderCode;
    
    @JsonProperty("sort_code")
    private String sortCode;
    
    @JsonProperty("trans_type")
    private String transType;
    
    @JsonProperty("ward_encode")
    private String wardEncode;
    
    @JsonProperty("district_encode")
    private String districtEncode;
    
    @JsonProperty("fee")
    private Fee fee;
    
    @JsonProperty("total_fee")
    private Integer totalFee;
    
    @JsonProperty("expected_delivery_time")
    private String expectedDeliveryTime;

    @Data
    public static class Fee {
        @JsonProperty("main_service")
        private Integer mainService;
        @JsonProperty("insurance")
        private Integer insurance;
        @JsonProperty("station_do")
        private Integer stationDo;
        @JsonProperty("station_pu")
        private Integer stationPu;
        @JsonProperty("return")
        private Integer returnFee;
        @JsonProperty("r2s")
        private Integer r2s;
        @JsonProperty("coupon")
        private Integer coupon;
    }
}
