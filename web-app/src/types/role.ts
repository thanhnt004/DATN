// ─────────────────────────────────────────────────────────────────────────────
// Role – Response DTOs
// ─────────────────────────────────────────────────────────────────────────────

/** Returned by most /api/v1/roles endpoints */
export interface RoleResponse {
  id: string;
  name: string;
  description: string;
  composite: boolean;
}

/** Returned by GET /api/v1/roles/{roleName}/users */
export interface UserWithRolesResponse {
  userId: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  enabled: boolean;
  roles: string[];
}

// ─────────────────────────────────────────────────────────────────────────────
// Role – Request DTOs
// ─────────────────────────────────────────────────────────────────────────────

/** POST /api/v1/roles */
export interface CreateRoleRequest {
  name: string;
  description?: string;
}

/** PUT /api/v1/roles/{roleId} */
export interface UpdateRoleRequest {
  name: string;
  description?: string;
}

/** POST /api/v1/roles/assign */
export interface AssignRoleRequest {
  userId: string;
  roleName: string;
  roleId: string;
}
