import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";
import type { RelatedCategoryResponse, SearchProductResponse, SearchParams } from "../types/search";
import type { PageResponse } from "../types/user";

// ─────────────────────────────────────────────────────────────────────────────
// Search API
// ─────────────────────────────────────────────────────────────────────────────

/** GET /api/v1/search/products - Search products */
export const searchProducts = (params: SearchParams) =>
  axiosInstance.get<ApiResponse<PageResponse<SearchProductResponse>>>("/api/v1/search/products", { params });

/** GET /api/v1/search/products/related-categories - Get related categories for current keyword/filter */
export const getRelatedCategories = (params: {
  keyword?: string;
  minPrice?: number;
  maxPrice?: number;
  minRating?: number;
  size?: number;
}) =>
  axiosInstance.get<ApiResponse<RelatedCategoryResponse[]>>("/api/v1/search/products/related-categories", { params });

/** GET /api/v1/search/suggest - Get search suggestions */
export const getSearchSuggestions = (keyword: string) =>
  axiosInstance.get<ApiResponse<string[]>>("/api/v1/search/suggest", { params: { keyword } });
