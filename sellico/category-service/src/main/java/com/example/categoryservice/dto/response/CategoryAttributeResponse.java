package com.example.categoryservice.dto.response;

import com.example.categoryservice.model.CategoryAttribute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CategoryAttributeResponse {
    private UUID id;
    private String value;
    private boolean required;
    private boolean filterable;
    private String dataType;
    private List<CategoryAttributeValueResponse> predefinedValues;
}
