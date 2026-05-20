package com.example.chatbotservice.adapter.in.web.controller;

import com.example.chatbotservice.adapter.in.web.request.ChatRequest;
import com.example.chatbotservice.adapter.in.web.response.ChatResponse;
import com.example.chatbotservice.application.port.in.ChatUseCase;
import com.example.chatbotservice.application.port.in.ClearChatHistoryUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatUseCase chatUseCase;
    private final ClearChatHistoryUseCase clearChatHistoryUseCase;

    /**
     * Send a message to the chatbot and receive product suggestions.
     */
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        ChatResponse response = chatUseCase.chat(request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Clear chat history for a specific session.
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> clearHistory(
            @PathVariable String sessionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        clearChatHistoryUseCase.clearHistory(sessionId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
