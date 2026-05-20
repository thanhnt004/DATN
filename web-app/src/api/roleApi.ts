import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";
import type {
  RoleResponse,
  UserWithRolesResponse,
  CreateRoleRequest,
  UpdateRoleRequest,
  AssignRoleRequest,
} from "../types/role";

const BASE = "/api/v1/roles";

// ─── Read ─────────────────────────────────────────────────────────────────────

/** GET /api/v1/roles  →  all roles */
export async function getAllRoles(): Promise<RoleResponse[]> {
  const { data } = await axiosInstance.get<ApiResponse<RoleResponse[]>>(BASE);
  return data.result;
}

/** GET /api/v1/roles/{roleId} */
export async function getRoleById(roleId: string): Promise<RoleResponse> {
  const { data } = await axiosInstance.get<ApiResponse<RoleResponse>>(
    `${BASE}/${roleId}`
  );
  return data.result;
}

/** GET /api/v1/roles/by-name/{roleName} */
export async function getRoleByName(roleName: string): Promise<RoleResponse> {
  const { data } = await axiosInstance.get<ApiResponse<RoleResponse>>(
    `${BASE}/by-name/${roleName}`
  );
  return data.result;
}

/** GET /api/v1/roles/{roleName}/users  →  all users that have this role */
export async function getUsersByRole(
  roleName: string
): Promise<UserWithRolesResponse[]> {
  const { data } = await axiosInstance.get<ApiResponse<UserWithRolesResponse[]>>(
    `${BASE}/${roleName}/users`
  );
  return data.result;
}

/** GET /api/v1/roles/users/{userId}  →  all roles for a user */
export async function getRolesForUser(userId: string): Promise<RoleResponse[]> {
  const { data } = await axiosInstance.get<ApiResponse<RoleResponse[]>>(
    `${BASE}/users/${userId}`
  );
  return data.result;
}

// ─── Create / Update / Delete ─────────────────────────────────────────────────

/** POST /api/v1/roles */
export async function createRole(payload: CreateRoleRequest): Promise<string> {
  const { data } = await axiosInstance.post<ApiResponse<string>>(BASE, payload);
  return data.result;
}

/** PUT /api/v1/roles/{roleId} */
export async function updateRole(
  roleId: string,
  payload: UpdateRoleRequest
): Promise<RoleResponse> {
  const { data } = await axiosInstance.put<ApiResponse<RoleResponse>>(
    `${BASE}/${roleId}`,
    payload
  );
  return data.result;
}

/** DELETE /api/v1/roles/{roleId} */
export async function deleteRole(roleId: string): Promise<string> {
  const { data } = await axiosInstance.delete<ApiResponse<string>>(
    `${BASE}/${roleId}`
  );
  return data.result;
}

// ─── Assign / Remove roles for users ─────────────────────────────────────────

/** POST /api/v1/roles/assign */
export async function assignRoleToUser(
  payload: AssignRoleRequest
): Promise<RoleResponse> {
  const { data } = await axiosInstance.post<ApiResponse<RoleResponse>>(
    `${BASE}/assign`,
    payload
  );
  return data.result;
}

/** DELETE /api/v1/roles/users/{userId}/roles/{roleName} */
export async function removeRoleFromUser(
  userId: string,
  roleName: string
): Promise<string> {
  const { data } = await axiosInstance.delete<ApiResponse<string>>(
    `${BASE}/users/${userId}/roles/${roleName}`
  );
  return data.result;
}
