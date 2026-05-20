/**
 * adminUserApi.ts
 * Covers AdminUserController → /api/v1/admin/users
 *
 * ⚠️  These routes are NOT exposed through the API Gateway.
 *     They are intended for use by an internal admin panel that connects
 *     directly to user-service or via a separate admin gateway.
 *     Adjust ADMIN_BASE_URL if routing differs in your environment.
 *
 * All endpoints require a valid JWT (admin user).
 */

import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";
import type {
  UserProfileResponse,
  PageResponse,
  AdminUpdateUserStatusRequest,
  AdminUserSearchParams,
} from "../types/user";

const ADMIN_USERS = "/api/v1/admin/users";

/**
 * GET /api/v1/admin/users?keyword=&status=&page=0&size=20  (JWT — admin)
 * Paginated search across all user profiles.
 */
export async function adminSearchUsers(
  params: AdminUserSearchParams = {}
): Promise<PageResponse<UserProfileResponse>> {
  const { data } = await axiosInstance.get<
    ApiResponse<PageResponse<UserProfileResponse>>
  >(ADMIN_USERS, {
    params: {
      keyword: params.keyword,
      status: params.status,
      page: params.page ?? 0,
      size: params.size ?? 20,
    },
  });
  return data.result;
}

/**
 * GET /api/v1/admin/users/{userId}  (JWT — admin)
 * Returns full profile of any user by their user-service UUID.
 */
export async function adminGetUser(
  userId: string
): Promise<UserProfileResponse> {
  const { data } = await axiosInstance.get<ApiResponse<UserProfileResponse>>(
    `${ADMIN_USERS}/${userId}`
  );
  return data.result;
}

/**
 * PATCH /api/v1/admin/users/{userId}/status  (JWT — admin)
 * Updates a user's status (ACTIVE | INACTIVE | BANNED | DEACTIVATED).
 * The caller's authId is automatically captured server-side for the audit log.
 */
export async function adminUpdateUserStatus(
  userId: string,
  payload: AdminUpdateUserStatusRequest
): Promise<UserProfileResponse> {
  const { data } = await axiosInstance.patch<ApiResponse<UserProfileResponse>>(
    `${ADMIN_USERS}/${userId}/status`,
    payload
  );
  return data.result;
}
