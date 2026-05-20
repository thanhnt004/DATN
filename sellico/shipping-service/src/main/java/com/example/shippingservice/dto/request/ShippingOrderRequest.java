package com.example.shippingservice.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingOrderRequest {
    @JsonProperty("payment_type_id")
    private Integer paymentTypeId; // 1: Shop/Seller, 2: Buyer/Consignee

    private String note;
    
    @JsonProperty("required_note")
    private String requiredNote; // CHOTXEM, CHOVERSHIP, KHONGCHOXEM

    @JsonProperty("return_phone")
    private String returnPhone;

    @JsonProperty("return_address")
    private String returnAddress;

    @JsonProperty("return_district_id")
    private Integer returnDistrictId;

    @JsonProperty("return_ward_code")
    private String returnWardCode;

    @JsonProperty("from_name")
    private String fromName;

    @JsonProperty("from_phone")
    private String fromPhone;

    @JsonProperty("from_address")
    private String fromAddress;

    @JsonProperty("from_ward_name")
    private String fromWardName;

    @JsonProperty("from_district_name")
    private String fromDistrictName;

    @JsonProperty("from_province_name")
    private String fromProvinceName;

    @JsonProperty("client_order_code")
    private String clientOrderCode;

    @JsonProperty("to_name")
    private String toName;

    @JsonProperty("to_phone")
    private String toPhone;

    @JsonProperty("to_address")
    private String toAddress;

    @JsonProperty("to_ward_code")
    private String toWardCode;

    @JsonProperty("to_district_id")
    private Integer toDistrictId;

    @JsonProperty("cod_amount")
    private Integer codAmount;

    @JsonProperty("content")
    private String content;

    private Integer weight;
    private Integer length;
    private Integer width;
    private Integer height;

    @JsonProperty("pick_station_id")
    private Integer pickStationId = 1444;

    @JsonProperty("insurance_value")
    private Integer insuranceValue = 0;

    @JsonProperty("service_id")
    private Integer serviceId = 0;

    @JsonProperty("service_type_id")
    private Integer serviceTypeId = 2;

    @JsonProperty("coupon")
    private String coupon;

    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String name;
        private String code;
        private Integer quantity;
        private Integer price;
        private Integer weight;
    }
}
