package com.example.categoryservice.dto.projection;

import java.util.UUID;

public interface CategoryProjection {
    UUID getId();
    UUID getParentId(); // JPA tự hiểu là lấy parent_id từ bảng
    String getName();
    String getSlug();
    String getPath();
    Integer getLevel();
    Integer getSortOrder();
    String getImageUrl();
    String getIconUrl();
    String getStatus();
    String getDescription();
}
