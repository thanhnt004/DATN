package com.example.productservice.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(DRAFT|PENDING|ACTIVE|BANNED|DELETED)$",
             message = "Status must be one of: DRAFT, PENDING, ACTIVE, BANNED, DELETED")
    private String status;
}

