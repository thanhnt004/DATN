package org.example.identityservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserWithRolesResponse {
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean enabled;
    private List<String> roles;
}

