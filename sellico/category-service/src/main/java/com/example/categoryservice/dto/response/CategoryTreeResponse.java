package com.example.categoryservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CategoryTreeResponse {
    private UUID id;
    private UUID parentId;
    private String name;
    private String slug;
    private String iconUrl;
    private String imageUrl;
    private Integer level;
    private String path;
    private Integer sortOrder;
    private String status;
    private String description;
    private List<CategoryTreeResponse> children;
}
