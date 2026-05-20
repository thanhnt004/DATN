package com.example.categoryservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCreateRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 255, message = "Tên danh mục không quá 255 ký tự")
    private String name;

    private UUID parentId; // null nếu là danh mục gốc

    private String description;

    private String iconUrl;

    @Builder.Default
    @Pattern(regexp = "^(ACTIVE|INACTIVE|DELETED)$", message = "Trạng thái không hợp lệ")
    private String status = "INACTIVE";

    private String imageUrl;

    @Min(value = 0, message = "Thứ tự sắp xếp phải lớn hơn hoặc bằng 0")
    private Integer sortOrder;

    // Cho phép thêm nhanh các thuộc tính đặc thù ngay khi tạo danh mục
    private List<CategoryAttributeRequest> attributes;
}
