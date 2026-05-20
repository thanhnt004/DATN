package com.example.productservice.application.port.in.image;

import java.util.UUID;

public interface DeleteImageUseCase {
    void deleteImage(UUID productId, UUID imageId);
}
