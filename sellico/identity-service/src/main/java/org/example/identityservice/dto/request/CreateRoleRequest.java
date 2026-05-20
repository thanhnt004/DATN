package org.example.identityservice.dto.request;

import lombok.Data;

@Data
public class CreateRoleRequest {
    String name;
    String description;
}
