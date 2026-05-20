package com.example.productservice.application.port.out;

import java.util.UUID;

public interface CategoryClientPort {
    boolean isLeaf(UUID categoryId);
    boolean isExist(UUID categoryId);
}
