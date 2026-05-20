package com.example.chatbotservice.adapter.out.storage;

import com.example.chatbotservice.application.port.out.ChatSessionStoragePort;
import com.example.chatbotservice.domain.model.ChatSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of ChatSessionStoragePort.
 * Suitable for single-instance deployments.
 * For production with multiple instances, replace with Redis implementation.
 */
@Component
@Slf4j
public class InMemoryChatSessionStorage implements ChatSessionStoragePort {

    private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();

    @Override
    public Optional<ChatSession> findBySessionId(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public void save(ChatSession session) {
        sessions.put(session.getSessionId(), session);
        log.debug("Saved chat session: {}", session.getSessionId());
    }

    @Override
    public void deleteBySessionId(String sessionId) {
        sessions.remove(sessionId);
        log.debug("Deleted chat session: {}", sessionId);
    }
}
