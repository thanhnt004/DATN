package com.example.productservice.application.port.in.image;

import java.util.UUID;

public interface SetPrimaryImageUseCase {
    void setPrimaryImage(UUID productId, UUID imageId);
}
