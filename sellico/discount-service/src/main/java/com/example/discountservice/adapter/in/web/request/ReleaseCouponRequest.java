package com.example.discountservice.adapter.in.web.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class ReleaseCouponRequest {
    @NotNull private UUID couponId;
    @NotNull private UUID userId;
    @NotNull private UUID orderId;
}

