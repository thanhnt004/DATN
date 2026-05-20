import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";
import type {
  InventoryResponse,
  StockAvailabilityResponse,
  InventoryLogResponse,
  RestockRequest,
  AdjustStockRequest,
  UpdateInventoryRequest,
} from "../types/inventory";

// ─────────────────────────────────────────────────────────────────────────────
// Inventory API (Seller-facing)
// ─────────────────────────────────────────────────────────────────────────────

/** GET /api/v1/inventories/:skuId - Get inventory for SKU */
export const getInventory = (skuId: string) =>
  axiosInstance.get<ApiResponse<InventoryResponse>>(`/api/v1/inventories/${skuId}`);

/** GET /api/v1/inventories/check-availability - Check stock availability for multiple SKUs */
export const checkAvailability = (skuIds: string[]) =>
  axiosInstance.get<ApiResponse<StockAvailabilityResponse[]>>("/api/v1/inventories/check-availability", {
    params: { skuIds: skuIds.join(",") },
  });

/** POST /api/v1/inventories/restock - Restock inventory */
export const restock = (data: RestockRequest) =>
  axiosInstance.post<ApiResponse<InventoryResponse>>("/api/v1/inventories/restock", data);

/** POST /api/v1/inventories/adjust - Adjust stock */
export const adjustStock = (data: AdjustStockRequest) =>
  axiosInstance.post<ApiResponse<InventoryResponse>>("/api/v1/inventories/adjust", data);

/** PUT /api/v1/inventories/:skuId - Update inventory settings */
export const updateInventory = (skuId: string, data: UpdateInventoryRequest) =>
  axiosInstance.put<ApiResponse<InventoryResponse>>(`/api/v1/inventories/${skuId}`, data);

/** GET /api/v1/inventories/:skuId/logs - Get inventory logs */
export const getInventoryLogs = (skuId: string, params?: { page?: number; size?: number }) =>
  axiosInstance.get<ApiResponse<InventoryLogResponse[]>>(`/api/v1/inventories/${skuId}/logs`, { params });
