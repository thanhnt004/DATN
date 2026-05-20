// ─────────────────────────────────────────────────────────────────────────────
// Inventory – Enums & Response DTOs
// ─────────────────────────────────────────────────────────────────────────────

export type ReservationStatus = "PENDING" | "CONFIRMED" | "CANCELLED" | "EXPIRED";
export type InventoryLogType = "SALE" | "RESTOCK" | "RETURN" | "ADJUSTMENT" | "EXPIRED_RESERVATION";

export interface InventoryResponse {
  skuId: string;
  totalStock: number;
  reservedStock: number;
  availableStock: number;
  lowStockThreshold: number;
  isLowStock: boolean;
  locationCode: string | null;
  updatedAt: string;
}

export interface StockAvailabilityResponse {
  skuId: string;
  availableStock: number;
  isAvailable: boolean;
  isLowStock: boolean;
}

export interface InventoryLogResponse {
  id: string;
  skuId: string;
  changeAmount: number;
  type: InventoryLogType;
  referenceId: string | null;
  note: string | null;
  createdAt: string;
}

// ─────────────────────────────────────────────────────────────────────────────
// Inventory – Request DTOs
// ─────────────────────────────────────────────────────────────────────────────

export interface RestockRequest {
  skuId: string;
  quantity: number;
  referenceId?: string;
  note?: string;
}

export interface AdjustStockRequest {
  skuId: string;
  changeAmount: number;
  note?: string;
}

export interface UpdateInventoryRequest {
  lowStockThreshold?: number;
  locationCode?: string;
}

export interface CreateInventoryRequest {
  skuId: string;
  totalStock: number;
  lowStockThreshold?: number;
  locationCode?: string;
}
