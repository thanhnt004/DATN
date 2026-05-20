package com.example.sellerservice.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSellerProfileRequest {

    @Size(min = 3, max = 100, message = "Shop name must be between 3 and 100 characters")
    private String shopName;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private String logoUrl;
    private String bannerUrl;
    private String phone;
    private String address;
    private String ward;
    private String district;
    private String city;
}

