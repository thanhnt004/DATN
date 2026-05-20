package com.example.categoryservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CategoryResponse {
    private UUID id;
    private UUID parentId;
    private String name;
    private String slug;
    private String path;   // Server trả về để Frontend biết cấp độ (VD: uuid1/uuid2)
    private Integer level; // Cấp độ (1, 2, 3...)
    private String status;
    private Instant createdAt;

    // Gửi kèm breadcrumb đơn giản để Frontend hiển thị ngay
    private List<CategoryBreadcrumb> breadcrumbs;
}
