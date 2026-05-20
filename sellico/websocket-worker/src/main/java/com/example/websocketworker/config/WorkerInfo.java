package com.example.websocketworker.config;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WorkerInfo {
    private final String workerId;

    public WorkerInfo() {
        this.workerId = resolveWorkerId();
    }

    private String resolveWorkerId() {
        String hostname = System.getenv("HOSTNAME");
        if (hostname != null && !hostname.isBlank()) {
            return hostname;
        }
        return UUID.randomUUID().toString();
    }

    public String getWorkerId() {
        return workerId;
    }
}
