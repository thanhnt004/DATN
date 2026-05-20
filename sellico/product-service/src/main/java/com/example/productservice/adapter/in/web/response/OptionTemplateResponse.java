package com.example.productservice.adapter.in.web.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionTemplateResponse {

    private UUID id;
    private String name;
    private String source;   // ADMIN / SELLER
    private UUID sellerId;   // null if ADMIN
    private Instant createdAt;
    private List<OptionTemplateValueResponse> values;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionTemplateValueResponse {
        private UUID id;
        private String value;
        private Integer sortOrder;
    }
}
