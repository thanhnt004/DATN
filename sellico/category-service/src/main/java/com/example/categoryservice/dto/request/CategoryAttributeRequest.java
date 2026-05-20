package com.example.categoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAttributeRequest {

    @NotBlank(message = "Tên thuộc tính không được để trống")
    private String name;

    @Builder.Default
    private Boolean isRequired = false;

    @Builder.Default
    private Boolean isFilterable = true;

    @Pattern(regexp = "^(string|number|boolean|enum)$")
    private String dataType;

    // Danh sách các giá trị gợi ý nếu dataType là 'enum'
    private List<CategoryAttributeValueRequest> predefinedValues;
}
