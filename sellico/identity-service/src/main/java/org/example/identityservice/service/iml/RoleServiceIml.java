package org.example.identityservice.service.iml;

import jakarta.ws.rs.NotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.identityservice.config.KeycloakProperties;
import org.example.identityservice.dto.request.AssignRoleRequest;
import org.example.identityservice.dto.request.CreateRoleRequest;
import org.example.identityservice.dto.request.UpdateRoleRequest;
import org.example.identityservice.dto.response.RoleResponse;
import org.example.identityservice.dto.response.UserWithRolesResponse;
import org.example.identityservice.exception.AuthErrorCode;
import org.example.identityservice.exception.KeycloakException;
import org.example.identityservice.service.RoleService;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RoleServiceIml implements RoleService {
    KeycloakProperties keycloakProperties;
    Keycloak keycloak;

    @Override
    public List<RoleResponse> getRoles() {
        try {
            List<RoleRepresentation> roleRepresentation = keycloak.realm(keycloakProperties.realm()).roles().list();
            return roleRepresentation.stream()
                    .map(this::toRoleResponse)
                    .toList();
        } catch (Exception e) {
            log.error("Error getting roles: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public RoleResponse getRoleById(String roleId) {
        try {
            // Keycloak doesn't have direct get by ID, need to get all and filter
            List<RoleRepresentation> roles = keycloak.realm(keycloakProperties.realm()).roles().list();
            RoleRepresentation role = roles.stream()
                    .filter(r -> r.getId().equals(roleId))
                    .findFirst()
                    .orElseThrow(() -> new KeycloakException(AuthErrorCode.ROLE_NOT_FOUND));
            return toRoleResponse(role);
        } catch (KeycloakException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting role by ID: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public RoleResponse getRoleByName(String roleName) {
        try {
            RoleRepresentation role = keycloak.realm(keycloakProperties.realm())
                    .roles()
                    .get(roleName)
                    .toRepresentation();
            return toRoleResponse(role);
        } catch (NotFoundException e) {
            throw new KeycloakException(AuthErrorCode.ROLE_NOT_FOUND);
        } catch (Exception e) {
            log.error("Error getting role by name: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createRole(CreateRoleRequest request) {
        try {
            keycloak.realm(keycloakProperties.realm()).roles().get(request.getName()).toRepresentation();
            throw new KeycloakException(AuthErrorCode.ROLE_ALREADY_EXISTS);
        } catch (NotFoundException e) {
            RoleRepresentation role = new RoleRepresentation();
            role.setName(request.getName());
            role.setDescription(request.getDescription());
            keycloak.realm(keycloakProperties.realm()).roles().create(role);
            log.info("Created role: {}", request.getName());
        }
    }

    @Override
    public RoleResponse updateRole(String roleId, UpdateRoleRequest request) {
        try {
            // Find role by ID first
            List<RoleRepresentation> roles = keycloak.realm(keycloakProperties.realm()).roles().list();
            RoleRepresentation existingRole = roles.stream()
                    .filter(r -> r.getId().equals(roleId))
                    .findFirst()
                    .orElseThrow(() -> new KeycloakException(AuthErrorCode.ROLE_NOT_FOUND));

            String oldName = existingRole.getName();

            // Update role
            existingRole.setName(request.getName());
            if (request.getDescription() != null) {
                existingRole.setDescription(request.getDescription());
            }

            keycloak.realm(keycloakProperties.realm())
                    .roles()
                    .get(oldName)
                    .update(existingRole);

            log.info("Updated role: {} -> {}", oldName, request.getName());
            return toRoleResponse(existingRole);
        } catch (KeycloakException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating role: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteRole(String roleId) {
        try {
            // Find role by ID first
            List<RoleRepresentation> roles = keycloak.realm(keycloakProperties.realm()).roles().list();
            RoleRepresentation role = roles.stream()
                    .filter(r -> r.getId().equals(roleId))
                    .findFirst()
                    .orElseThrow(() -> new KeycloakException(AuthErrorCode.ROLE_NOT_FOUND));

            keycloak.realm(keycloakProperties.realm())
                    .roles()
                    .deleteRole(role.getName());

            log.info("Deleted role: {}", role.getName());
        } catch (KeycloakException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting role: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public RoleResponse assignRoleToUser(AssignRoleRequest request) {
        try {
            RoleRepresentation role = new RoleRepresentation();
            role.setId(request.getRoleId());
            role.setName(request.getRoleName());

            keycloak.realm(keycloakProperties.realm())
                    .users()
                    .get(request.getUserId())
                    .roles()
                    .realmLevel()
                    .add(Collections.singletonList(role));

            log.info("Assigned role {} to user {}", request.getRoleName(), request.getUserId());
            return toRoleResponse(role);
        } catch (NotFoundException e) {
            throw new KeycloakException(AuthErrorCode.USER_NOT_FOUND);
        } catch (Exception e) {
            log.error("Error assigning role: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeRoleFromUser(String userId, String roleName) {
        try {
            RoleRepresentation role = keycloak.realm(keycloakProperties.realm())
                    .roles()
                    .get(roleName)
                    .toRepresentation();

            keycloak.realm(keycloakProperties.realm())
                    .users()
                    .get(userId)
                    .roles()
                    .realmLevel()
                    .remove(Collections.singletonList(role));

            log.info("Removed role {} from user {}", roleName, userId);
        } catch (NotFoundException e) {
            throw new KeycloakException(AuthErrorCode.ROLE_NOT_FOUND);
        } catch (Exception e) {
            log.error("Error removing role: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void assignSellerRole(String keycloakUserId) {
        try {
            // Fetch the SELLER role representation from Keycloak
            RoleRepresentation sellerRole = keycloak.realm(keycloakProperties.realm())
                    .roles()
                    .get("SELLER")
                    .toRepresentation();

            // Assign the SELLER role to the user
            keycloak.realm(keycloakProperties.realm())
                    .users()
                    .get(keycloakUserId)
                    .roles()
                    .realmLevel()
                    .add(Collections.singletonList(sellerRole));

            log.info("Assigned SELLER role to Keycloak user {}", keycloakUserId);
        } catch (NotFoundException e) {
            log.error("SELLER role not found in Keycloak realm '{}'", keycloakProperties.realm());
            throw new KeycloakException(AuthErrorCode.ROLE_NOT_FOUND);
        } catch (Exception e) {
            log.error("Error assigning SELLER role to user {}: {}", keycloakUserId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<UserWithRolesResponse> getUsersByRole(String roleName) {
        try {
            // Get users with this role
            List<UserRepresentation> users = keycloak.realm(keycloakProperties.realm())
                    .roles()
                    .get(roleName)
                    .getUserMembers();

            return users.stream()
                    .map(user -> {
                        // Get all roles for this user
                        List<String> userRoles = keycloak.realm(keycloakProperties.realm())
                                .users()
                                .get(user.getId())
                                .roles()
                                .realmLevel()
                                .listEffective()
                                .stream()
                                .map(RoleRepresentation::getName)
                                .collect(Collectors.toList());

                        return UserWithRolesResponse.builder()
                                .userId(user.getId())
                                .username(user.getUsername())
                                .email(user.getEmail())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .enabled(user.isEnabled())
                                .roles(userRoles)
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (NotFoundException e) {
            throw new KeycloakException(AuthErrorCode.ROLE_NOT_FOUND);
        } catch (Exception e) {
            log.error("Error getting users by role: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<RoleResponse> getUserRoles(String userId) {
        try {
            List<RoleRepresentation> roles = keycloak.realm(keycloakProperties.realm())
                    .users()
                    .get(userId)
                    .roles()
                    .realmLevel()
                    .listEffective();

            return roles.stream()
                    .map(this::toRoleResponse)
                    .collect(Collectors.toList());
        } catch (NotFoundException e) {
            throw new KeycloakException(AuthErrorCode.USER_NOT_FOUND);
        } catch (Exception e) {
            log.error("Error getting user roles: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private RoleResponse toRoleResponse(RoleRepresentation role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .composite(role.isComposite())
                .build();
    }
}
