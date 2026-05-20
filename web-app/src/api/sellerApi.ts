/**
 * sellerApi.ts
 * Covers seller-service endpoints under /api/v1/sellers
 * All authenticated endpoints require Bearer token (injected by axiosInstance interceptor).
 */

import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";
import type { SellerResponse, RegisterSellerRequest } from "../types/seller";

const BASE = "/api/v1/sellers";

// ─────────────────────────────────────────────────────────────────────────────
// Registration flow
// ─────────────────────────────────────────────────────────────────────────────

/**
 * GET /api/v1/sellers/register/status  (JWT)
 * Check the current user's seller registration status.
 * Returns null if the user has not applied yet (404).
 */
export async function getMySellerStatus(): Promise<SellerResponse | null> {
  try {
    const { data } = await axiosInstance.get<ApiResponse<SellerResponse>>(
      `${BASE}/register/status`
    );
    return data.result;
  } catch (err: unknown) {
    // 404 = no seller application yet → not an error, just return null
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    if ((err as any)?.response?.status === 404) return null;
    throw err;
  }
}

/**
 * POST /api/v1/sellers/register  (JWT)
 * Submit a new seller registration.
 */
export async function registerSeller(
  payload: RegisterSellerRequest
): Promise<SellerResponse> {
  const { data } = await axiosInstance.post<ApiResponse<SellerResponse>>(
    `${BASE}/register`,
    payload
  );
  return data.result;
}

/**
 * PUT /api/v1/sellers/register/resubmit  (JWT)
 * Resubmit registration after rejection.
 */
export async function resubmitSellerRegistration(
  payload: RegisterSellerRequest
): Promise<SellerResponse> {
  const { data } = await axiosInstance.put<ApiResponse<SellerResponse>>(
    `${BASE}/register/resubmit`,
    payload
  );
  return data.result;
}
