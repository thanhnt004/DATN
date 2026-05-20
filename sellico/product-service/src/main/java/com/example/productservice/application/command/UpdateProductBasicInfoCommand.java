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
public class UpdateProductBasicInfoCommand {
    UUID productId;
    UUID sellerId;
    String name;
    String slug;
    String description;
    UUID categoryId;
}

