import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";
import type {
  PaymentResponse,
  RefundResponse,
} from "../types/payment";

// ─────────────────────────────────────────────────────────────────────────────
// Payment API
// ─────────────────────────────────────────────────────────────────────────────

/** GET /api/v1/payments/vnpay/callback - VNPay callback (browser redirect) */
export const vnPayCallback = (params: Record<string, string>) =>
  axiosInstance.get<ApiResponse<PaymentResponse>>("/api/v1/payments/vnpay/callback", { params });

/** GET /api/v1/payments/order/:orderId - Get payment by order */
export const getPaymentByOrder = (orderId: string) =>
  axiosInstance.get<ApiResponse<PaymentResponse>>(`/api/v1/payments/order/${orderId}`);

/** GET /api/v1/payments/:paymentId - Get payment detail */
export const getPaymentById = (paymentId: string) =>
  axiosInstance.get<ApiResponse<PaymentResponse>>(`/api/v1/payments/${paymentId}`);

/** GET /api/v1/payments/order/:orderId/refunds - Get refunds for order */
export const getRefundsByOrder = (orderId: string) =>
  axiosInstance.get<ApiResponse<RefundResponse[]>>(`/api/v1/payments/order/${orderId}/refunds`);
