package com.example.discountservice.adapter.in.web.request;

import com.example.discountservice.domain.model.enums.RuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateCouponRequest {
    private UUID campaignId;
    private UUID sellerId;

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Coupon type is required")
    private String couponType; // PLATFORM, SHOP

    @NotBlank(message = "Discount type is required")
    private String discountType; // PERCENTAGE, FIXED_AMOUNT

    @NotNull(message = "Discount value is required")
    @Positive(message = "Discount value must be positive")
    private BigDecimal discountValue;

    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;

    @Positive(message = "Total quantity must be positive")
    private int totalQuantity;

    private int maxUsagePerUser = 1;
    private boolean isStackable = false;

    @NotNull(message = "Start date is required")
    private String startDate;

    @NotNull(message = "End date is required")
    private String endDate;

    private List<RuleItem> rules;

    @Data
    public static class RuleItem {
        private RuleType ruleType;
        private UUID targetId;
    }
}

