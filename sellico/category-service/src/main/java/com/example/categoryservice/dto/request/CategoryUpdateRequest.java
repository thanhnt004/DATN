package com.example.categoryservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryUpdateRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 255)
    private String name;

    private String description;

    @Size(max = 500)
    private String iconUrl;

    @Size(max = 500)
    private String imageUrl;

    private UUID parentId; // Có thể null nếu là danh mục gốc

    @Min(0)
    private Integer sortOrder;

    @Pattern(regexp = "^(ACTIVE|INACTIVE|DELETED)$", message = "Trạng thái không hợp lệ")
    private String status;

    // Getters và Setters
}
