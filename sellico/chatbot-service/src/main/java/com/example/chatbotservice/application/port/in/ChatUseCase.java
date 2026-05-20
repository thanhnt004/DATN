package com.example.chatbotservice.application.port.in;

import com.example.chatbotservice.adapter.in.web.request.ChatRequest;
import com.example.chatbotservice.adapter.in.web.response.ChatResponse;

public interface ChatUseCase {
    ChatResponse chat(ChatRequest request, String userId);
}
