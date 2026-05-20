package com.example.bannerservice.adapter.in.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderBannersRequest {

    @NotEmpty(message = "Banner orders list cannot be empty")
    @Valid
    private List<BannerOrderItem> bannerOrders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BannerOrderItem {
        @NotNull(message = "Banner ID is required")
        private UUID bannerId;

        @NotNull(message = "Sort order is required")
        private Integer sortOrder;
    }
}

