package com.example.chatbotservice.adapter.in.web.response;

import com.example.chatbotservice.domain.model.ProductInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String sessionId;
    private String reply;
    private List<ProductInfo> suggestedProducts;
}
