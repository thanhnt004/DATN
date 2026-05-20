package com.example.productservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@Value
public class GetBatchProductsCommand {
    List<UUID> productIds;
}

