package com.example.shippingservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerResponse {
    private UUID id;
    private String shopName;
    private String address;
    private String ward;
    private String district;
    private String city;
}
