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
public class UpdateProductImagesCommand {
    UUID productId;
    UUID sellerId;
    List<ImageItem> images;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ImageItem {
        private final String url;
        private final Boolean isPrimary;
        private final Integer sortOrder;
    }
}

