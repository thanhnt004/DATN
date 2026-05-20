package com.example.productservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

import java.util.UUID;

@Getter
@AllArgsConstructor
@Value
public class AddImageCommand {
    UUID productId;
    String url;
    Boolean isPrimary;
    Integer sortOrder;
}
