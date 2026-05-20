package com.example.chatbotservice.application.port.out;

import com.example.chatbotservice.domain.model.ChatSession;

import java.util.Optional;

/**
 * Port for persisting chat sessions (conversation history).
 */
public interface ChatSessionStoragePort {

    Optional<ChatSession> findBySessionId(String sessionId);

    void save(ChatSession session);

    void deleteBySessionId(String sessionId);
}
