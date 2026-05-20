package org.example.identityservice.service;

import org.example.identityservice.dto.request.AssignRoleRequest;
import org.example.identityservice.dto.request.CreateRoleRequest;
import org.example.identityservice.dto.request.UpdateRoleRequest;
import org.example.identityservice.dto.response.RoleResponse;
import org.example.identityservice.dto.response.UserWithRolesResponse;

import java.util.List;

public interface RoleService {

    /**
     * Get all roles
     */
    List<RoleResponse> getRoles();

    /**
     * Get role by ID
     */
    RoleResponse getRoleById(String roleId);

    /**
     * Get role by name
     */
    RoleResponse getRoleByName(String roleName);

    /**
     * Create new role
     */
    void createRole(CreateRoleRequest request);

    /**
     * Update role
     */
    RoleResponse updateRole(String roleId, UpdateRoleRequest request);

    /**
     * Delete role
     */
    void deleteRole(String roleId);

    /**
     * Assign role to user
     */
    RoleResponse assignRoleToUser(AssignRoleRequest request);

    /**
     * Remove role from user
     */
    void removeRoleFromUser(String userId, String roleName);

    /**
     * Get all users with a specific role
     */
    List<UserWithRolesResponse> getUsersByRole(String roleName);

    /**
     * Assign SELLER role to a Keycloak user by their Keycloak user ID.
     * Used internally when a seller registration is approved.
     */
    void assignSellerRole(String keycloakUserId);

    /**
     * Get roles of a specific user
     */
    List<RoleResponse> getUserRoles(String userId);
}
