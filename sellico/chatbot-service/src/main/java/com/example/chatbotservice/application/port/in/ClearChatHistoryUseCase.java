package com.example.chatbotservice.application.port.in;

public interface ClearChatHistoryUseCase {
    void clearHistory(String sessionId, String userId);
}
