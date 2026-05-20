package org.example.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignRoleRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String roleName;
    @NotBlank
    private String roleId;
}