package com.example.websocketworker.dto;

import lombok.Data;

@Data
public class WsMessage<T> {

    private String traceId;
    private Feature feature;
    private Action action;

    private String from;
    private String to;

    private long timestamp;

    private T payload;
}
