import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";
import type { OrderResponse, CreateOrderRequest, CancelOrderRequest } from "../types/order";
import type { PageResponse } from "../types/user";

// ─────────────────────────────────────────────────────────────────────────────
// Buyer Order API
// ─────────────────────────────────────────────────────────────────────────────

/** POST /api/v1/orders - Create new order(s) */
export const createOrder = (data: CreateOrderRequest) =>
  axiosInstance.post<ApiResponse<OrderResponse[]>>("/api/v1/orders", data);

/** POST /api/v1/orders/batch - Create multiple orders at once (multi-seller) */
export const createOrders = (data: CreateOrderRequest[]) =>
  axiosInstance.post<ApiResponse<OrderResponse[]>>("/api/v1/orders/batch", data);

/** GET /api/v1/orders - Get buyer's orders */
export const getMyOrders = (params?: { status?: string; page?: number; size?: number }) =>
  axiosInstance.get<ApiResponse<PageResponse<OrderResponse>>>("/api/v1/orders", { params });

/** GET /api/v1/orders/:orderId - Get single order detail */
export const getOrderById = (orderId: string) =>
  axiosInstance.get<ApiResponse<OrderResponse>>(`/api/v1/orders/${orderId}`);

/** POST /api/v1/orders/:orderId/cancel - Cancel an order (buyer) */
export const cancelOrder = (orderId: string, data: CancelOrderRequest) =>
  axiosInstance.post<ApiResponse<OrderResponse>>(`/api/v1/orders/${orderId}/cancel`, data);

/** POST /api/v1/orders/:orderId/complete - Confirm delivery / complete order (buyer) */
export const confirmDelivery = (orderId: string) =>
  axiosInstance.post<ApiResponse<OrderResponse>>(`/api/v1/orders/${orderId}/complete`);

/** POST /api/v1/orders/:orderId/retry-payment - Retry payment for unpaid order */
export const retryPayment = (orderId: string) =>
  axiosInstance.post<ApiResponse<OrderResponse>>(`/api/v1/orders/${orderId}/retry-payment`);
