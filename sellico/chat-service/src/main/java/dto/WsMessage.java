package dto;

import lombok.Data;

@Data
public class WsMessage<T> {

    private String traceId;
    private String feature;
    private String action;

    private String from;
    private String to;

    private long timestamp;

    private T payload;
}

