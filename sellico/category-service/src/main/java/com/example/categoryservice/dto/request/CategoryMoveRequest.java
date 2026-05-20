package com.example.categoryservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryMoveRequest {
    UUID parentId; // Có thể null nếu đưa ra làm danh mục gốc
    Integer sortOrder;
}
