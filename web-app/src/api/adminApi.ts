/**
 * adminApi.ts
 * Covers all system-admin endpoints:
 *   - /api/v1/admin/users     (user-service)
 *   - /api/v1/admin/sellers   (seller-service)
 *   - /api/v1/admin/banners   (banner-service)
 *   - /api/v1/admin/categories / /api/v1/categories (category-service)
 */

import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";
import type {
  AdminUserResponse,
  AdminUpdateUserStatusRequest,
  AdminSellerResponse,
  SellerStatsResponse,
  AdminOrderStatsResponse,
  RejectSellerRequest,
  BannerResponse,
  BannerPositionResponse,
  CreateBannerRequest,
  UpdateBannerRequest,
  CategoryResponse,
  CategoryTreeResponse,
  CreateCategoryRequest,
  UpdateCategoryRequest,
  CategoryAttributeRequest,
  CategoryAttributeResponse,
  OptionTemplateResponse,
  CreateOptionTemplateRequest,
  UpdateOptionTemplateRequest,
  PageResponse,
} from "../types/admin";

// ─── User management ─────────────────────────────────────────────────────────

const ADMIN_USERS = "/api/v1/admin/users";

export async function adminListUsers(params: {
  keyword?: string;
  status?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<AdminUserResponse>> {
  const { data } = await axiosInstance.get<ApiResponse<PageResponse<AdminUserResponse>>>(
    ADMIN_USERS,
    { params }
  );
  return data.result;
}

export async function adminGetUser(userId: string): Promise<AdminUserResponse> {
  const { data } = await axiosInstance.get<ApiResponse<AdminUserResponse>>(
    `${ADMIN_USERS}/${userId}`
  );
  return data.result;
}

export async function adminUpdateUserStatus(
  userId: string,
  payload: AdminUpdateUserStatusRequest
): Promise<AdminUserResponse> {
  const { data } = await axiosInstance.patch<ApiResponse<AdminUserResponse>>(
    `${ADMIN_USERS}/${userId}/status`,
    payload
  );
  return data.result;
}

// ─── Seller management ───────────────────────────────────────────────────────

const ADMIN_SELLERS = "/api/v1/admin/sellers";

export async function adminListSellers(params: {
  status?: string;
  keyword?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<AdminSellerResponse>> {
  const { data } = await axiosInstance.get<ApiResponse<PageResponse<AdminSellerResponse>>>(
    ADMIN_SELLERS,
    { params }
  );
  return data.result;
}

export async function adminGetSellerStats(): Promise<SellerStatsResponse> {
  const { data } = await axiosInstance.get<ApiResponse<SellerStatsResponse>>(
    `${ADMIN_SELLERS}/stats`
  );
  return data.result;
}

export async function adminGetOrderStatsOverview(): Promise<AdminOrderStatsResponse> {
  const { data } = await axiosInstance.get<ApiResponse<AdminOrderStatsResponse>>(
    `/api/v1/admin/orders/stats/overview`
  );
  return data.result;
}

export async function adminApproveSeller(sellerId: string): Promise<AdminSellerResponse> {
  const { data } = await axiosInstance.post<ApiResponse<AdminSellerResponse>>(
    `${ADMIN_SELLERS}/${sellerId}/approve`
  );
  return data.result;
}

export async function adminRejectSeller(
  sellerId: string,
  payload: RejectSellerRequest
): Promise<AdminSellerResponse> {
  const { data } = await axiosInstance.post<ApiResponse<AdminSellerResponse>>(
    `${ADMIN_SELLERS}/${sellerId}/reject`,
    payload
  );
  return data.result;
}

export async function adminSuspendSeller(
  sellerId: string,
  reason?: string
): Promise<AdminSellerResponse> {
  const { data } = await axiosInstance.post<ApiResponse<AdminSellerResponse>>(
    `${ADMIN_SELLERS}/${sellerId}/suspend`,
    undefined,
    { params: { reason } }
  );
  return data.result;
}

export async function adminReactivateSeller(sellerId: string): Promise<AdminSellerResponse> {
  const { data } = await axiosInstance.post<ApiResponse<AdminSellerResponse>>(
    `${ADMIN_SELLERS}/${sellerId}/reactivate`
  );
  return data.result;
}

// ─── Banner management ───────────────────────────────────────────────────────

const ADMIN_BANNERS = "/api/v1/admin/banners";

export async function adminListBanners(params: {
  positionCode?: string;
  status?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}): Promise<PageResponse<BannerResponse>> {
  const { data } = await axiosInstance.get<ApiResponse<PageResponse<BannerResponse>>>(
    ADMIN_BANNERS,
    { params }
  );
  return data.result;
}

export async function adminGetBanner(id: string): Promise<BannerResponse> {
  const { data } = await axiosInstance.get<ApiResponse<BannerResponse>>(
    `${ADMIN_BANNERS}/${id}`
  );
  return data.result;
}

export async function adminCreateBanner(payload: CreateBannerRequest): Promise<BannerResponse> {
  const { data } = await axiosInstance.post<ApiResponse<BannerResponse>>(
    ADMIN_BANNERS,
    payload
  );
  return data.result;
}

export async function adminUpdateBanner(
  id: string,
  payload: UpdateBannerRequest
): Promise<BannerResponse> {
  const { data } = await axiosInstance.put<ApiResponse<BannerResponse>>(
    `${ADMIN_BANNERS}/${id}`,
    payload
  );
  return data.result;
}

export async function adminDeleteBanner(id: string): Promise<void> {
  await axiosInstance.delete(`${ADMIN_BANNERS}/${id}`);
}

export async function adminUpdateBannerStatus(
  id: string,
  status: string
): Promise<BannerResponse> {
  const { data } = await axiosInstance.patch<ApiResponse<BannerResponse>>(
    `${ADMIN_BANNERS}/${id}/status`,
    { status }
  );
  return data.result;
}

export async function adminListBannerPositions(): Promise<BannerPositionResponse[]> {
  const { data } = await axiosInstance.get<ApiResponse<BannerPositionResponse[]>>(
    `${ADMIN_BANNERS}/positions`
  );
  return data.result;
}

// ─── Category management ─────────────────────────────────────────────────────

const ADMIN_CATEGORIES = "/api/v1/admin/categories";
const PUBLIC_CATEGORIES = "/api/v1/categories";

export async function adminGetCategoryTree(): Promise<CategoryTreeResponse[]> {
  const { data } = await axiosInstance.get<ApiResponse<CategoryTreeResponse[]>>(
    `${PUBLIC_CATEGORIES}/tree`,
    { params: { maxLevel: 5 } }
  );
  return data.result;
}

export async function adminCreateCategory(
  payload: CreateCategoryRequest
): Promise<CategoryResponse> {
  const { data } = await axiosInstance.post<ApiResponse<CategoryResponse>>(
    ADMIN_CATEGORIES,
    payload
  );
  return data.result;
}

export async function adminUpdateCategory(
  id: string,
  payload: UpdateCategoryRequest
): Promise<CategoryResponse> {
  const { data } = await axiosInstance.put<ApiResponse<CategoryResponse>>(
    `${ADMIN_CATEGORIES}/${id}`,
    payload
  );
  return data.result;
}

export async function adminDeleteCategory(id: string): Promise<void> {
  await axiosInstance.delete(`${ADMIN_CATEGORIES}/${id}`);
}

export async function adminMoveCategory(
  id: string,
  payload: { parentId?: string | null; sortOrder?: number }
): Promise<CategoryResponse> {
  const { data } = await axiosInstance.patch<ApiResponse<CategoryResponse>>(
    `${ADMIN_CATEGORIES}/${id}/move`,
    payload
  );
  return data.result;
}

export async function adminNormalizeCategorySortOrder(): Promise<{ totalUpdated: number; message: string }> {
  const { data } = await axiosInstance.post<ApiResponse<{ totalUpdated: number; message: string }>>(
    `${ADMIN_CATEGORIES}/normalize-sort-order`
  );
  return data.result;
}

// ─── Category Attribute management ───────────────────────────────────────────

export async function adminGetCategoryAttributes(
  categoryId: string
): Promise<CategoryAttributeResponse[]> {
  const { data } = await axiosInstance.get<ApiResponse<CategoryAttributeResponse[]>>(
    `${PUBLIC_CATEGORIES}/${categoryId}/attributes`
  );
  return data.result;
}

export async function adminCreateCategoryAttribute(
  categoryId: string,
  payload: CategoryAttributeRequest
): Promise<CategoryAttributeResponse> {
  const { data } = await axiosInstance.post<ApiResponse<CategoryAttributeResponse>>(
    `${ADMIN_CATEGORIES}/${categoryId}/attributes`,
    payload
  );
  return data.result;
}

export async function adminUpdateCategoryAttribute(
  attributeId: string,
  payload: CategoryAttributeRequest
): Promise<CategoryAttributeResponse> {
  const { data } = await axiosInstance.put<ApiResponse<CategoryAttributeResponse>>(
    `${ADMIN_CATEGORIES}/attributes/${attributeId}`,
    payload
  );
  return data.result;
}

export async function adminDeleteCategoryAttribute(
  attributeId: string
): Promise<void> {
  await axiosInstance.delete(`${ADMIN_CATEGORIES}/attributes/${attributeId}`);
}

// ─── Product management (admin) ───────────────────────────────────────────────

const ADMIN_PRODUCTS = "/api/v1/admin/products";

export async function adminListProducts(params: {
  status?: string;
  keyword?: string;
  sellerId?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}): Promise<PageResponse<import("../types/product").ProductResponse>> {
  const { data } = await axiosInstance.get<
    ApiResponse<PageResponse<import("../types/product").ProductResponse>>
  >(ADMIN_PRODUCTS, { params });
  return data.result;
}

export async function adminUpdateProductStatus(
  productId: string,
  status: string
): Promise<import("../types/product").ProductResponse> {
  const { data } = await axiosInstance.patch<
    ApiResponse<import("../types/product").ProductResponse>
  >(`${ADMIN_PRODUCTS}/${productId}/status`, { status });
  return data.result;
}

// ─── Option Template management ──────────────────────────────────────────────

const ADMIN_OPTION_TEMPLATES = "/api/v1/admin/option-templates";

export async function adminListOptionTemplates(): Promise<OptionTemplateResponse[]> {
  const { data } = await axiosInstance.get<ApiResponse<OptionTemplateResponse[]>>(
    ADMIN_OPTION_TEMPLATES
  );
  return data.result;
}

export async function adminCreateOptionTemplate(
  payload: CreateOptionTemplateRequest
): Promise<OptionTemplateResponse> {
  const { data } = await axiosInstance.post<ApiResponse<OptionTemplateResponse>>(
    ADMIN_OPTION_TEMPLATES,
    payload
  );
  return data.result;
}

export async function adminUpdateOptionTemplate(
  id: string,
  payload: UpdateOptionTemplateRequest
): Promise<OptionTemplateResponse> {
  const { data } = await axiosInstance.put<ApiResponse<OptionTemplateResponse>>(
    `${ADMIN_OPTION_TEMPLATES}/${id}`,
    payload
  );
  return data.result;
}

export async function adminDeleteOptionTemplate(id: string): Promise<void> {
  await axiosInstance.delete(`${ADMIN_OPTION_TEMPLATES}/${id}`);
}
