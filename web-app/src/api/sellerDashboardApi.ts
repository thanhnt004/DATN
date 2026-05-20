/**
 * sellerDashboardApi.ts
 * Covers seller self-management endpoints:
 *   - /api/v1/seller/**         (seller-service: profile, documents, bank)
 *   - /api/v1/seller/orders/**  (order-service)
 *   - /api/v1/seller/products/* (product-service)
 *   - /api/v1/products/**       (product-service: list, create, delete, status)
 */

import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";
import type { SellerResponse } from "../types/seller";
import type {
  SellerDocumentResponse,
  UploadDocumentRequest,
  BankAccountResponse,
  UpdateBankAccountRequest,
} from "../types/seller";
import type {
  ProductResponse,
  CreateProductRequest,
  UpdateProductBasicInfoRequest,
  UpdateProductStatusRequest,
  UpdateSingleSkuRequest,
  ProductPageParams,
} from "../types/product";
import type {
  OptionTemplateResponse,
  CreateOptionTemplateRequest,
  UpdateOptionTemplateRequest,
} from "../types/admin";
import type {
  OrderResponse,
  ShipOrderRequest,
  CancelOrderRequest,
  OrderStatus,
  DetailedReportResponse,
  DetailedReportSummaryResponse,
  DetailedReportTrendResponse,
  DetailedReportCategoryResponse,
  DetailedReportProductSalesResponse,
} from "../types/order";
import type { PageResponse } from "../types/user";

// ─── Seller profile ───────────────────────────────────────────────────────────

export interface UpdateSellerProfileRequest {
  shopName: string;
  description?: string;
  logoUrl?: string;
  bannerUrl?: string;
  phone?: string;
  address?: string;
  ward?: string;
  district?: string;
  city?: string;
}

export async function getMySellerProfile(): Promise<SellerResponse> {
  const { data } = await axiosInstance.get<ApiResponse<SellerResponse>>(
    "/api/v1/seller/profile"
  );
  return data.result;
}

export async function updateMySellerProfile(
  payload: UpdateSellerProfileRequest
): Promise<SellerResponse> {
  const { data } = await axiosInstance.put<ApiResponse<SellerResponse>>(
    "/api/v1/seller/profile",
    payload
  );
  return data.result;
}

// ─── Products ─────────────────────────────────────────────────────────────────

export interface ProductPage {
  content: ProductResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  first: boolean;
  last: boolean;
}

export async function getSellerProducts(
  params: ProductPageParams
): Promise<ProductPage> {
  const { data } = await axiosInstance.get<ApiResponse<ProductPage>>(
    "/api/v1/products",
    { params }
  );
  return data.result;
}

export async function createProduct(
  payload: CreateProductRequest
): Promise<ProductResponse> {
  const { data } = await axiosInstance.post<ApiResponse<ProductResponse>>(
    "/api/v1/products",
    payload
  );
  return data.result;
}

export async function updateProductBasicInfo(
  productId: string,
  payload: UpdateProductBasicInfoRequest
): Promise<ProductResponse> {
  const { data } = await axiosInstance.patch<ApiResponse<ProductResponse>>(
    `/api/v1/seller/products/${productId}`,
    payload
  );
  return data.result;
}

export async function updateProductStatus(
  productId: string,
  payload: UpdateProductStatusRequest
): Promise<ProductResponse> {
  const { data } = await axiosInstance.patch<ApiResponse<ProductResponse>>(
    `/api/v1/products/${productId}/status`,
    payload
  );
  return data.result;
}

export async function deleteProduct(productId: string): Promise<void> {
  await axiosInstance.delete(`/api/v1/products/${productId}`);
}

// ─── Seller orders ────────────────────────────────────────────────────────────

export async function getSellerOrders(params: {
  status?: OrderStatus | "";
  orderNumber?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<OrderResponse>> {
  const { data } = await axiosInstance.get<ApiResponse<PageResponse<OrderResponse>>>(
    "/api/v1/seller/orders",
    {
      params: {
        status: params.status || undefined,
        orderNumber: params.orderNumber || undefined,
        startDate: params.startDate || undefined,
        endDate: params.endDate || undefined,
        page: params.page ?? 0,
        size: params.size ?? 15,
      },
    }
  );
  return data.result;
}

export async function getSellerDetailedSummary(params?: {
  startDate?: string;
  endDate?: string;
  period?: "DAILY" | "WEEKLY" | "MONTHLY";
}): Promise<DetailedReportSummaryResponse> {
  const { data } = await axiosInstance.get<ApiResponse<DetailedReportSummaryResponse>>(
    "/api/v1/seller/orders/stats/detailed/summary",
    { params }
  );
  return data.result;
}

export async function getSellerDetailedTrend(params?: {
  startDate?: string;
  endDate?: string;
  period?: "DAILY" | "WEEKLY" | "MONTHLY";
}): Promise<DetailedReportTrendResponse> {
  const { data } = await axiosInstance.get<ApiResponse<DetailedReportTrendResponse>>(
    "/api/v1/seller/orders/stats/detailed/trend",
    { params }
  );
  return data.result;
}

export async function getSellerDetailedCategory(params?: {
  startDate?: string;
  endDate?: string;
  period?: "DAILY" | "WEEKLY" | "MONTHLY";
}): Promise<DetailedReportCategoryResponse> {
  const { data } = await axiosInstance.get<ApiResponse<DetailedReportCategoryResponse>>(
    "/api/v1/seller/orders/stats/detailed/category",
    { params }
  );
  return data.result;
}

export async function getSellerDetailedProducts(params?: {
  startDate?: string;
  endDate?: string;
  period?: "DAILY" | "WEEKLY" | "MONTHLY";
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}): Promise<DetailedReportProductSalesResponse> {
  const { data } = await axiosInstance.get<ApiResponse<DetailedReportProductSalesResponse>>(
    "/api/v1/seller/orders/stats/detailed/products",
    { params }
  );
  return data.result;
}

export async function getSellerDetailedStats(params?: {
  startDate?: string;
  endDate?: string;
  period?: "DAILY" | "WEEKLY" | "MONTHLY";
}): Promise<DetailedReportResponse> {
  const [summary, trend, category, products] = await Promise.all([
    getSellerDetailedSummary(params),
    getSellerDetailedTrend(params),
    getSellerDetailedCategory(params),
    getSellerDetailedProducts(params),
  ]);
  return {
    ...summary,
    ...trend,
    ...category,
    ...products,
  };
}

export async function getSellerOrderById(orderId: string): Promise<OrderResponse> {
  const { data } = await axiosInstance.get<ApiResponse<OrderResponse>>(
    `/api/v1/seller/orders/${orderId}`
  );
  return data.result;
}

export async function confirmOrder(orderId: string, note?: string): Promise<OrderResponse> {
  const { data } = await axiosInstance.post<ApiResponse<OrderResponse>>(
    `/api/v1/seller/orders/${orderId}/confirm`,
    null,
    { params: note ? { note } : undefined }
  );
  return data.result;
}

export async function shipOrder(
  orderId: string,
  payload: ShipOrderRequest
): Promise<OrderResponse> {
  const { data } = await axiosInstance.post<ApiResponse<OrderResponse>>(
    `/api/v1/seller/orders/${orderId}/ship`,
    payload
  );
  return data.result;
}

export async function deliverOrder(orderId: string): Promise<OrderResponse> {
  const { data } = await axiosInstance.post<ApiResponse<OrderResponse>>(
    `/api/v1/seller/orders/${orderId}/deliver`
  );
  return data.result;
}

export async function completeOrder(orderId: string): Promise<OrderResponse> {
  const { data } = await axiosInstance.post<ApiResponse<OrderResponse>>(
    `/api/v1/seller/orders/${orderId}/complete`
  );
  return data.result;
}

export async function cancelOrder(
  orderId: string,
  payload: CancelOrderRequest
): Promise<OrderResponse> {
  const { data } = await axiosInstance.post<ApiResponse<OrderResponse>>(
    `/api/v1/seller/orders/${orderId}/cancel`,
    payload
  );
  return data.result;
}

// ─── Seller Documents ─────────────────────────────────────────────────────────

export async function getMyDocuments(): Promise<SellerDocumentResponse[]> {
  const { data } = await axiosInstance.get<ApiResponse<SellerDocumentResponse[]>>(
    "/api/v1/seller/documents"
  );
  return data.result;
}

export async function uploadDocument(
  payload: UploadDocumentRequest
): Promise<SellerDocumentResponse> {
  const { data } = await axiosInstance.post<ApiResponse<SellerDocumentResponse>>(
    "/api/v1/seller/documents",
    payload
  );
  return data.result;
}

// ─── Seller Bank Account ──────────────────────────────────────────────────────

export async function getMyBankAccount(): Promise<BankAccountResponse | null> {
  try {
    const { data } = await axiosInstance.get<ApiResponse<BankAccountResponse>>(
      "/api/v1/seller/bank-account"
    );
    return data.result;
  } catch (err: unknown) {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    if ((err as any)?.response?.status === 404) return null;
    throw err;
  }
}

export async function updateMyBankAccount(
  payload: UpdateBankAccountRequest
): Promise<BankAccountResponse> {
  const { data } = await axiosInstance.put<ApiResponse<BankAccountResponse>>(
    "/api/v1/seller/bank-account",
    payload
  );
  return data.result;
}

// ─── Product Images / Specs / SKUs ────────────────────────────────────────────

export async function updateProductImages(
  productId: string,
  images: { url: string; isPrimary?: boolean; sortOrder?: number }[]
): Promise<void> {
  await axiosInstance.put(`/api/v1/seller/products/${productId}/images`, { images });
}

export async function updateProductSpecifications(
  productId: string,
  specifications: { name: string; value: string }[]
): Promise<void> {
  await axiosInstance.put(`/api/v1/seller/products/${productId}/specifications`, { specifications });
}

export async function updateProductSkus(
  productId: string,
  skus: {
    skuCode: string; price: number; originalPrice?: number; costPrice?: number;
    weightGram?: number;
    dimensions?: { lengthCm?: number; widthCm?: number; heightCm?: number };
    selectionAttributes: Record<string, string>;
  }[]
): Promise<void> {
  await axiosInstance.patch(`/api/v1/seller/products/${productId}/skus`, { skus });
}

export async function updateSingleSku(
  skuId: string,
  payload: UpdateSingleSkuRequest
): Promise<void> {
  await axiosInstance.patch(`/api/v1/seller/skus/${skuId}`, payload);
}

// ─── Low stock ────────────────────────────────────────────────────────────────

export async function getLowStockInventories(): Promise<
  { skuId: string; totalStock: number; reservedStock: number; availableStock: number; lowStockThreshold: number; isLowStock: boolean }[]
> {
  const { data } = await axiosInstance.get<ApiResponse<
    { skuId: string; totalStock: number; reservedStock: number; availableStock: number; lowStockThreshold: number; isLowStock: boolean }[]
  >>("/api/v1/inventories/low-stock");
  return data.result;
}

// ─── Inventory management ─────────────────────────────────────────────────────

import type { InventoryResponse, CreateInventoryRequest } from "../types/inventory";

export async function batchUpsertInventory(
  items: CreateInventoryRequest[]
): Promise<InventoryResponse[]> {
  const { data } = await axiosInstance.post<ApiResponse<InventoryResponse[]>>(
    "/api/v1/inventories/batch",
    items
  );
  return data.result;
}

export async function getBatchInventories(
  skuIds: string[]
): Promise<InventoryResponse[]> {
  const { data } = await axiosInstance.get<ApiResponse<InventoryResponse[]>>(
    "/api/v1/inventories/batch",
    { params: { skuIds: skuIds.join(",") } }
  );
  return data.result;
}

// ─── Option templates (seller) ────────────────────────────────────────────────

const SELLER_OPTION_TEMPLATES = "/api/v1/seller/option-templates";

export async function sellerListOptionTemplates(): Promise<OptionTemplateResponse[]> {
  const { data } = await axiosInstance.get<ApiResponse<OptionTemplateResponse[]>>(
    SELLER_OPTION_TEMPLATES
  );
  return data.result;
}

export async function sellerCreateOptionTemplate(
  payload: CreateOptionTemplateRequest
): Promise<OptionTemplateResponse> {
  const { data } = await axiosInstance.post<ApiResponse<OptionTemplateResponse>>(
    SELLER_OPTION_TEMPLATES,
    payload
  );
  return data.result;
}

export async function sellerUpdateOptionTemplate(
  id: string,
  payload: UpdateOptionTemplateRequest
): Promise<OptionTemplateResponse> {
  const { data } = await axiosInstance.put<ApiResponse<OptionTemplateResponse>>(
    `${SELLER_OPTION_TEMPLATES}/${id}`,
    payload
  );
  return data.result;
}

export async function sellerDeleteOptionTemplate(id: string): Promise<void> {
  await axiosInstance.delete(`${SELLER_OPTION_TEMPLATES}/${id}`);
}

/** Get all available option templates (admin global + seller's own) */
export async function getAvailableOptionTemplates(): Promise<OptionTemplateResponse[]> {
  const { data } = await axiosInstance.get<ApiResponse<OptionTemplateResponse[]>>(
    "/api/v1/option-templates/available"
  );
  return data.result;
}

// --- Shipping actions -------------------------------------------------------------

export interface BatchResult<T> {
  successItems: T[];
  failedItems: { id: T; reason: string; item?: T }[];
}

export async function batchShipOrders(orderIds: string[]): Promise<BatchResult<string>> {
  const res = await axiosInstance.post<ApiResponse<BatchResult<string>>>(
    "/api/v1/shipping/orders",
    orderIds
  );
  return res.data.result;
}

export async function printShippingLabel(orderCodes: string[]): Promise<string> {
  const res = await axiosInstance.post<string>(
    "/api/v1/shipping/print",
    { order_codes: orderCodes }
  );
  return res.data;
}
