package com.example.productservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import model.SpecAttribute;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@Value
public class UpdateProductSpecificationsCommand {
    UUID productId;
    UUID sellerId;
    List<SpecAttribute> specifications;
}

