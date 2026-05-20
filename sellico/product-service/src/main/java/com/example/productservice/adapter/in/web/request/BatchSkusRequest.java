package com.example.productservice.adapter.in.web.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for batch fetching SKUs
 * Endpoint: POST /internal/v1/skus/batch
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSkusRequest {

    private List<UUID> skuIds;

    private List<String> skuCodes;
}

