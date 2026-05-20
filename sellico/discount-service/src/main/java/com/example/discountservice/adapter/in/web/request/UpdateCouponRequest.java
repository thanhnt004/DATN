package com.example.discountservice.adapter.in.web.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateCouponRequest {
    private String code;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Integer totalQuantity;
    private Integer maxUsagePerUser;
    private Boolean isStackable;
    private String status;
    private String startDate;
    private String endDate;
}

