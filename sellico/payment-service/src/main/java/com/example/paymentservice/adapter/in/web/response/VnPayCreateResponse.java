package com.example.paymentservice.adapter.in.web.response;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VnPayCreateResponse {
    private String paymentUrl;
}

