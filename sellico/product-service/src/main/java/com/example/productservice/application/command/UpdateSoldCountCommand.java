package com.example.productservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@Value
public class UpdateSoldCountCommand {
    UUID productId;
    int quantity; // positive = increment, can be used to add sold count
}

