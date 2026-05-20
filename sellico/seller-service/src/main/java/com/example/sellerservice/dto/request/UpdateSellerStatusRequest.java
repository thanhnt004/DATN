package com.example.sellerservice.dto.request;

import com.example.sellerservice.entity.enums.SellerStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSellerStatusRequest {

    @NotNull(message = "Status is required")
    private SellerStatus status;

    private String reason;
}

