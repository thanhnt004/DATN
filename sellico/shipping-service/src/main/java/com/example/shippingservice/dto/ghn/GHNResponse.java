package com.example.shippingservice.dto.ghn;

import lombok.Data;

@Data
public class GHNResponse<T> {
    private int code;
    private String message;
    private T data;
}
