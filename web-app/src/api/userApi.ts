/**
 * userApi.ts
 * Covers:
 *   UserController    → /api/v1/users  (JWT required except GET /{userId})
 *   AddressController → /api/v1/users/me/addresses  (JWT required)
 *
 * All responses are wrapped: { code, result, message? }
 * axiosInstance automatically injects the Bearer token from Redux store.
 */

import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";
import type {
  UserProfileResponse,
  UserPublicResponse,
  AddressResponse,
  UpdateProfileRequest,
  ChangeEmailRequest,
  DeactivateAccountRequest,
  CreateAddressRequest,
  UpdateAddressRequest,
} from "../types/user";

const USERS = "/api/v1/users";
const ME = `${USERS}/me`;
const ADDRESSES = `${ME}/addresses`;

// ─────────────────────────────────────────────────────────────────────────────
// User profile — /api/v1/users
// ─────────────────────────────────────────────────────────────────────────────

/**
 * GET /api/v1/users/me  (JWT)
 * Returns the full profile of the authenticated user.
 */
export async function getMyProfile(): Promise<UserProfileResponse> {
  const { data } = await axiosInstance.get<ApiResponse<UserProfileResponse>>(ME);
  return data.result;
}

/**
 * PUT /api/v1/users/me  (JWT)
 * Updates editable profile fields.
 */
export async function updateMyProfile(
  payload: UpdateProfileRequest
): Promise<UserProfileResponse> {
  const { data } = await axiosInstance.put<ApiResponse<UserProfileResponse>>(
    ME,
    payload
  );
  return data.result;
}

/**
 * PATCH /api/v1/users/me/email  (JWT)
 * Requests an email change (may trigger verification).
 */
export async function changeMyEmail(
  payload: ChangeEmailRequest
): Promise<UserProfileResponse> {
  const { data } = await axiosInstance.patch<ApiResponse<UserProfileResponse>>(
    `${ME}/email`,
    payload
  );
  return data.result;
}

/**
 * POST /api/v1/users/me/deactivate  (JWT)
 * Self-deactivates the authenticated user's account.
 */
export async function deactivateMyAccount(
  payload: DeactivateAccountRequest
): Promise<void> {
  await axiosInstance.post(`${ME}/deactivate`, payload);
}

/**
 * GET /api/v1/users/{userId}  (PUBLIC — no auth required)
 * Returns limited public info about any user.
 */
export async function getUserPublicProfile(
  userId: string
): Promise<UserPublicResponse> {
  const { data } = await axiosInstance.get<ApiResponse<UserPublicResponse>>(
    `${USERS}/${userId}`
  );
  return data.result;
}

// ─────────────────────────────────────────────────────────────────────────────
// Addresses — /api/v1/users/me/addresses  (JWT)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * GET /api/v1/users/me/addresses
 * Returns all addresses of the authenticated user.
 */
export async function getMyAddresses(): Promise<AddressResponse[]> {
  const { data } = await axiosInstance.get<ApiResponse<AddressResponse[]>>(
    ADDRESSES
  );
  return data.result;
}

/**
 * GET /api/v1/users/me/addresses/default
 * Returns the default address of the authenticated user.
 */
export async function getMyDefaultAddress(): Promise<AddressResponse> {
  const { data } = await axiosInstance.get<ApiResponse<AddressResponse>>(
    `${ADDRESSES}/default`
  );
  return data.result;
}

/**
 * GET /api/v1/users/me/addresses/{addressId}
 * Returns a specific address by ID.
 */
export async function getMyAddress(addressId: string): Promise<AddressResponse> {
  const { data } = await axiosInstance.get<ApiResponse<AddressResponse>>(
    `${ADDRESSES}/${addressId}`
  );
  return data.result;
}

/**
 * POST /api/v1/users/me/addresses  → HTTP 201 Created
 * Creates a new address. Returns the created address.
 */
export async function createAddress(
  payload: CreateAddressRequest
): Promise<AddressResponse> {
  const { data } = await axiosInstance.post<ApiResponse<AddressResponse>>(
    ADDRESSES,
    payload
  );
  return data.result;
}

/**
 * PUT /api/v1/users/me/addresses/{addressId}
 * Updates an existing address.
 */
export async function updateAddress(
  addressId: string,
  payload: UpdateAddressRequest
): Promise<AddressResponse> {
  const { data } = await axiosInstance.put<ApiResponse<AddressResponse>>(
    `${ADDRESSES}/${addressId}`,
    payload
  );
  return data.result;
}

/**
 * DELETE /api/v1/users/me/addresses/{addressId}
 * Deletes an address. Cannot delete the default address if it's the only one.
 */
export async function deleteAddress(addressId: string): Promise<void> {
  await axiosInstance.delete(`${ADDRESSES}/${addressId}`);
}

/**
 * PATCH /api/v1/users/me/addresses/{addressId}/default
 * Sets an address as the new default.
 */
export async function setDefaultAddress(
  addressId: string
): Promise<AddressResponse> {
  const { data } = await axiosInstance.patch<ApiResponse<AddressResponse>>(
    `${ADDRESSES}/${addressId}/default`
  );
  return data.result;
}
