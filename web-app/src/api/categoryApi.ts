import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";
import type { CategoryTreeResponse } from "../types/admin";

// ─────────────────────────────────────────────────────────────────────────────
// Public Category API
// ─────────────────────────────────────────────────────────────────────────────

export interface PublicCategoryResponse {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  iconUrl: string | null;
  parentId: string | null;
  sortOrder: number;
  status: string;
}

/** GET /api/v1/categories/tree - Get full category tree (public) */
export const getCategoryTree = () =>
  axiosInstance.get<ApiResponse<CategoryTreeResponse[]>>("/api/v1/categories/tree");

/** GET /api/v1/categories/:id - Get category by ID */
export const getCategoryById = (id: string) =>
  axiosInstance.get<ApiResponse<PublicCategoryResponse>>(`/api/v1/categories/${id}`);
