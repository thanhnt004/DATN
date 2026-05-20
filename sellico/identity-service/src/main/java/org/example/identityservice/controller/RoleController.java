package org.example.identityservice.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.identityservice.dto.request.AssignRoleRequest;
import org.example.identityservice.dto.request.CreateRoleRequest;
import org.example.identityservice.dto.request.UpdateRoleRequest;
import org.example.identityservice.dto.response.RoleResponse;
import org.example.identityservice.dto.response.UserWithRolesResponse;
import org.example.identityservice.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/roles")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleController {

    RoleService roleService;

    /**
     * Get all roles
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        List<RoleResponse> roles = roleService.getRoles();
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    /**
     * Get role by ID
     */
    @GetMapping("/{roleId}")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(@PathVariable("roleId") String roleId) {
        RoleResponse role = roleService.getRoleById(roleId);
        return ResponseEntity.ok(ApiResponse.success(role));
    }

    /**
     * Get role by name
     */
    @GetMapping("/by-name/{roleName}")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleByName(@PathVariable("roleName") String roleName) {
        RoleResponse role = roleService.getRoleByName(roleName);
        return ResponseEntity.ok(ApiResponse.success(role));
    }

    /**
     * Create new role
     */
    @PostMapping
    public ResponseEntity<ApiResponse<String>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        roleService.createRole(request);
        return ResponseEntity.ok(ApiResponse.success("Role created successfully"));
    }

    /**
     * Update role
     */
    @PutMapping("/{roleId}")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable("roleId") String roleId,
            @Valid @RequestBody UpdateRoleRequest request) {
        RoleResponse role = roleService.updateRole(roleId, request);
        return ResponseEntity.ok(ApiResponse.success(role));
    }

    /**
     * Delete role
     */
    @DeleteMapping("/{roleId}")
    public ResponseEntity<ApiResponse<String>> deleteRole(@PathVariable("roleId") String roleId) {
        roleService.deleteRole(roleId);
        return ResponseEntity.ok(ApiResponse.success("Role deleted successfully"));
    }

    /**
     * Assign role to user
     */
    @PostMapping("/assign")
    public ResponseEntity<ApiResponse<RoleResponse>> assignRoleToUser(
            @Valid @RequestBody AssignRoleRequest request) {
        RoleResponse role = roleService.assignRoleToUser(request);
        return ResponseEntity.ok(ApiResponse.success(role));
    }

    /**
     * Remove role from user
     */
    @DeleteMapping("/users/{userId}/roles/{roleName}")
    public ResponseEntity<ApiResponse<String>> removeRoleFromUser(
            @PathVariable("userId") String userId,
            @PathVariable("roleName") String roleName) {
        roleService.removeRoleFromUser(userId, roleName);
        return ResponseEntity.ok(ApiResponse.success("Role removed from user successfully"));
    }

    /**
     * Get all users with a specific role
     */
    @GetMapping("/{roleName}/users")
    public ResponseEntity<ApiResponse<List<UserWithRolesResponse>>> getUsersByRole(
            @PathVariable("roleName") String roleName) {
        List<UserWithRolesResponse> users = roleService.getUsersByRole(roleName);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * Get roles of a specific user
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getUserRoles(@PathVariable("userId") String userId) {
        List<RoleResponse> roles = roleService.getUserRoles(userId);
        return ResponseEntity.ok(ApiResponse.success(roles));
    }
}
