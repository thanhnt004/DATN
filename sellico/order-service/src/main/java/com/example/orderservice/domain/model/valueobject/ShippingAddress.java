package com.example.orderservice.domain.model.valueobject;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Shipping Address Value Object
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public record ShippingAddress(
        String recipientName,
        String recipientPhone,
        String address,
        String ward,
        String district,
        String city
) {
    public ShippingAddress {
        if (recipientName == null || recipientName.isBlank()) {
            throw new IllegalArgumentException("Recipient name is required");
        }
        if (recipientPhone == null || recipientPhone.isBlank()) {
            throw new IllegalArgumentException("Recipient phone is required");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address is required");
        }
    }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder(address);
        if (ward != null && !ward.isBlank()) {
            sb.append(", ").append(ward);
        }
        if (district != null && !district.isBlank()) {
            sb.append(", ").append(district);
        }
        if (city != null && !city.isBlank()) {
            sb.append(", ").append(city);
        }
        return sb.toString();
    }
}

