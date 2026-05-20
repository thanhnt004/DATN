import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";
import type { ProductResponse, ProductSummaryResponse, ProductPageParams } from "../types/product";
import type { PageResponse } from "../types/user";

// ─────────────────────────────────────────────────────────────────────────────
// Public Product API
// ─────────────────────────────────────────────────────────────────────────────

/** GET /api/v1/products/:id - Get product detail (public) */
export const getProductById = (id: string) =>
  axiosInstance.get<ApiResponse<ProductResponse>>(`/api/v1/products/${id}`);

/** GET /api/v1/products - List products with filters (public) */
export const getProducts = (params: ProductPageParams) =>
  axiosInstance.get<ApiResponse<PageResponse<ProductSummaryResponse>>>("/api/v1/products", { params });

/** GET /api/v1/products/category/:categoryId - Products by category (public) */
export const getProductsByCategory = (categoryId: string, params?: { page?: number; size?: number; sortBy?: string; sortDirection?: string }) =>
  axiosInstance.get<ApiResponse<PageResponse<ProductSummaryResponse>>>(`/api/v1/products/category/${categoryId}`, { params });

/** GET /api/v1/products/seller/:sellerId - Products by seller (public) */
export const getProductsBySeller = (sellerId: string, params?: { page?: number; size?: number }) =>
  axiosInstance.get<ApiResponse<PageResponse<ProductSummaryResponse>>>(`/api/v1/products/seller/${sellerId}`, { params });
