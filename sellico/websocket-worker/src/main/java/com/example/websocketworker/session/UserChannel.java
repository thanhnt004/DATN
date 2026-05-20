package com.example.websocketworker.session;

import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UserChannel {
    private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    public void addSession(WebSocketSession session) {
        sessions.add(session);
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
    }

    public boolean isEmpty() {
        return sessions.isEmpty();
    }

    public Flux<String> getStream() {
        return sink.asFlux();
    }
    public Long getTotalSessions() {
        return (long) sessions.size();
    }
    public void emit(String message) {
        Sinks.EmitResult result = sink.tryEmitNext(message);
        if (result.isFailure()) {
            // log / retry / drop tùy policy
        }
    }

    public void close() {
        sink.tryEmitComplete();
    }
}
