package com.example.chatbotservice.adapter.in.web.controller;

import com.example.chatbotservice.adapter.in.web.request.ChatRequest;
import com.example.chatbotservice.adapter.in.web.response.ChatResponse;
import com.example.chatbotservice.application.port.in.ChatUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

/**
 * Guest chatbot endpoint — no authentication required.
 * Provides limited chatbot functionality for unauthenticated users.
 */
@RestController
@RequestMapping("/api/v1/chatbot/guest")
@RequiredArgsConstructor
public class GuestChatbotController {

    private final ChatUseCase chatUseCase;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @Valid @RequestBody ChatRequest request
    ) {
        ChatResponse response = chatUseCase.chat(request, "guest");
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
