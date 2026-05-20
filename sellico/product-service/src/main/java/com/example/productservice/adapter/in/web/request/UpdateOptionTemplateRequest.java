package com.example.productservice.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateOptionTemplateRequest {

    @NotBlank(message = "Tên option không được để trống")
    @Size(max = 100, message = "Tên option tối đa 100 ký tự")
    private String name;

    /** Danh sách giá trị mới (sẽ thay thế hoàn toàn) */
    private List<String> values;
}
