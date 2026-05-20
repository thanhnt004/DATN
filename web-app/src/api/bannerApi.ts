import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";

// ─────────────────────────────────────────────────────────────────────────────
// Public Banner API
// ─────────────────────────────────────────────────────────────────────────────

export interface PublicBannerResponse {
  id: string;
  title: string;
  imageUrl: string;
  linkUrl: string | null;
  linkType: string;
  linkValue: string | null;
  sortOrder: number;
}

/** GET /api/v1/banners/active/:positionCode - Get active banners for a position */
export const getActiveBanners = (positionCode: string) =>
  axiosInstance.get<ApiResponse<PublicBannerResponse[]>>(`/api/v1/banners/active/${positionCode}`);

/** POST /api/v1/banners/:id/click - Track banner click */
export const trackBannerClick = (id: string) =>
  axiosInstance.post<ApiResponse<void>>(`/api/v1/banners/${id}/click`);

/** POST /api/v1/banners/:id/view - Track banner view */
export const trackBannerView = (id: string) =>
  axiosInstance.post<ApiResponse<void>>(`/api/v1/banners/${id}/view`);
