import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";
import type { SellerResponse } from "../types/seller";

// ─────────────────────────────────────────────────────────────────────────────
// Public Seller API – shop pages
// ─────────────────────────────────────────────────────────────────────────────

/** GET /api/v1/sellers/:sellerId - Get public seller/shop info */
export const getSellerPublic = (sellerId: string) =>
  axiosInstance.get<ApiResponse<SellerResponse>>(`/api/v1/sellers/${sellerId}`);

/** POST /api/v1/sellers/:sellerId/follow - Follow a seller */
export const followSeller = (sellerId: string) =>
  axiosInstance.post<ApiResponse<void>>(`/api/v1/sellers/${sellerId}/follow`);

/** DELETE /api/v1/sellers/:sellerId/follow - Unfollow a seller */
export const unfollowSeller = (sellerId: string) =>
  axiosInstance.delete<ApiResponse<void>>(`/api/v1/sellers/${sellerId}/follow`);

/** GET /api/v1/sellers/:sellerId/following - Check if following */
export const isFollowing = (sellerId: string) =>
  axiosInstance.get<ApiResponse<boolean>>(`/api/v1/sellers/${sellerId}/following`);

/** GET /api/v1/sellers/following - Get list of followed seller IDs */
export const getFollowedSellers = () =>
  axiosInstance.get<ApiResponse<string[]>>("/api/v1/sellers/following");
