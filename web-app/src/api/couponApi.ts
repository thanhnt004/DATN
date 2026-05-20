import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";
import type {
  CouponResponse,
  UserCouponResponse,
  ApplyCouponRequest,
  ApplyCouponResult,
  CreateCouponRequest,
  UpdateCouponRequest,
  CampaignResponse,
  CreateCampaignRequest,
  UpdateCampaignRequest,
} from "../types/coupon";
import type { PageResponse } from "../types/user";

// ─────────────────────────────────────────────────────────────────────────────
// Buyer Coupon API (public + authenticated)
// ─────────────────────────────────────────────────────────────────────────────

/** GET /api/v1/coupons/available - List available coupons (public) */
export const getAvailableCoupons = (params?: { sellerId?: string; page?: number; size?: number }) =>
  axiosInstance.get<ApiResponse<PageResponse<CouponResponse>>>("/api/v1/coupons/available", { params });

/** GET /api/v1/coupons/:id - Get coupon detail (public) */
export const getCouponById = (id: string) =>
  axiosInstance.get<ApiResponse<CouponResponse>>(`/api/v1/coupons/${id}`);

/** POST /api/v1/coupons/:id/claim - Claim a coupon */
export const claimCoupon = (id: string) =>
  axiosInstance.post<ApiResponse<UserCouponResponse>>(`/api/v1/coupons/${id}/claim`);

/** GET /api/v1/coupons/my - Get claimed coupons */
export const getMyCoupons = (params?: { status?: string; page?: number; size?: number }) =>
  axiosInstance.get<ApiResponse<UserCouponResponse[]>>("/api/v1/coupons/my", { params });

/** POST /api/v1/coupons/apply - Apply coupon to order */
export const applyCoupon = (data: ApplyCouponRequest) =>
  axiosInstance.post<ApiResponse<ApplyCouponResult>>("/api/v1/coupons/apply", data);

// ─────────────────────────────────────────────────────────────────────────────
// Seller Coupon API
// ─────────────────────────────────────────────────────────────────────────────

/** POST /api/v1/seller/coupons - Create coupon (seller) */
export const sellerCreateCoupon = (data: CreateCouponRequest) =>
  axiosInstance.post<ApiResponse<CouponResponse>>("/api/v1/seller/coupons", data);

/** GET /api/v1/seller/coupons - List seller's coupons */
export const sellerGetCoupons = (params?: { status?: string; page?: number; size?: number }) =>
  axiosInstance.get<ApiResponse<PageResponse<CouponResponse>>>("/api/v1/seller/coupons", { params });

/** GET /api/v1/seller/coupons/:id - Get seller's coupon detail */
export const sellerGetCouponById = (id: string) =>
  axiosInstance.get<ApiResponse<CouponResponse>>(`/api/v1/seller/coupons/${id}`);

/** PUT /api/v1/seller/coupons/:id - Update coupon (seller) */
export const sellerUpdateCoupon = (id: string, data: UpdateCouponRequest) =>
  axiosInstance.put<ApiResponse<CouponResponse>>(`/api/v1/seller/coupons/${id}`, data);

// ─────────────────────────────────────────────────────────────────────────────
// Admin Coupon API
// ─────────────────────────────────────────────────────────────────────────────

/** POST /api/v1/admin/coupons - Create coupon (admin) */
export const adminCreateCoupon = (data: CreateCouponRequest) =>
  axiosInstance.post<ApiResponse<CouponResponse>>("/api/v1/admin/coupons", data);

/** GET /api/v1/admin/coupons - List platform coupons (admin) */
export const adminGetCoupons = (params?: { status?: string; page?: number; size?: number }) =>
  axiosInstance.get<ApiResponse<PageResponse<CouponResponse>>>("/api/v1/admin/coupons", { params });

/** GET /api/v1/admin/coupons/:id - Get coupon (admin) */
export const adminGetCouponById = (id: string) =>
  axiosInstance.get<ApiResponse<CouponResponse>>(`/api/v1/admin/coupons/${id}`);

/** PUT /api/v1/admin/coupons/:id - Update coupon (admin) */
export const adminUpdateCoupon = (id: string, data: UpdateCouponRequest) =>
  axiosInstance.put<ApiResponse<CouponResponse>>(`/api/v1/admin/coupons/${id}`, data);

// ─────────────────────────────────────────────────────────────────────────────
// Admin Campaign API
// ─────────────────────────────────────────────────────────────────────────────

/** POST /api/v1/admin/campaigns - Create campaign */
export const adminCreateCampaign = (data: CreateCampaignRequest) =>
  axiosInstance.post<ApiResponse<CampaignResponse>>("/api/v1/admin/campaigns", data);

/** GET /api/v1/admin/campaigns - List campaigns */
export const adminGetCampaigns = (params?: { sellerId?: string; status?: string; page?: number; size?: number }) =>
  axiosInstance.get<ApiResponse<PageResponse<CampaignResponse>>>("/api/v1/admin/campaigns", { params });

/** GET /api/v1/admin/campaigns/:id - Get campaign detail */
export const adminGetCampaignById = (id: string) =>
  axiosInstance.get<ApiResponse<CampaignResponse>>(`/api/v1/admin/campaigns/${id}`);

/** PUT /api/v1/admin/campaigns/:id - Update campaign */
export const adminUpdateCampaign = (id: string, data: UpdateCampaignRequest) =>
  axiosInstance.put<ApiResponse<CampaignResponse>>(`/api/v1/admin/campaigns/${id}`, data);

/** DELETE /api/v1/admin/campaigns/:id - Delete campaign */
export const adminDeleteCampaign = (id: string) =>
  axiosInstance.delete<ApiResponse<void>>(`/api/v1/admin/campaigns/${id}`);
