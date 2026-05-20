package com.example.shippingservice.dto.request;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ShippingFeeRequest {
    private UUID sellerId;
    private String ward;
    private String district;
    private String city;
    private Integer weight;
    private Integer length;
    private Integer width;
    private Integer height;
}
