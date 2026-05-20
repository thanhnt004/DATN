import type { PageResponse } from "./user";

// ─────────────────────────────────────────────────────────────────────────────
// Order – Enums & Response DTOs
// ─────────────────────────────────────────────────────────────────────────────

export type OrderStatus =
  | "PENDING"
  | "CONFIRMED"
  | "SHIPPED"
  | "DELIVERED"
  | "COMPLETED"
  | "CANCELLED";

export type PaymentMethod = "COD" | "BANK_TRANSFER" | "CREDIT_CARD" | "MOMO" | "VNPAY";
export type PaymentStatus = "PENDING" | "PAID" | "FAILED" | "REFUNDED";

export interface OrderItemResponse {
  id: string;
  skuId: string;
  productId: string;
  productName: string;
  skuCode: string;
  imageUrl: string | null;
  unitPrice: number;
  quantity: number;
  subtotal: number;
  discountAmount: number;
  variantInfo: Record<string, string>;
}

export interface OrderResponse {
  id: string;
  orderNumber: string;
  userId: string;
  sellerId: string;
  status: OrderStatus;
  subtotal: number;
  shippingFee: number;
  discountAmount: number;
  totalAmount: number;
  recipientName: string;
  recipientPhone: string;
  shippingAddress: string;
  shippingWard: string | null;
  shippingDistrict: string | null;
  shippingCity: string | null;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  paidAt: string | null;
  shippedAt: string | null;
  deliveredAt: string | null;
  shippingProvider: string | null;
  trackingNumber: string | null;
  buyerNote: string | null;
  sellerNote: string | null;
  cancelReason: string | null;
  voucherCode: string | null;
  platformCouponId: string | null;
  platformVoucherShare: number;
  paymentUrl?: string;
  createdAt: string;
  confirmedAt: string | null;
  completedAt: string | null;
  cancelledAt: string | null;
  items: OrderItemResponse[];
}

// ─────────────────────────────────────────────────────────────────────────────
// Order – Request DTOs
// ─────────────────────────────────────────────────────────────────────────────

export interface ShipOrderRequest {
  shippingProvider: string;
  trackingNumber: string;
  note?: string;
}

export interface CancelOrderRequest {
  reason: string;
}

// Buyer create order
export interface OrderItemDto {
  skuId: string;
  productId: string;
  productName: string;
  skuCode?: string;
  imageUrl?: string;
  unitPrice: number;
  quantity: number;
  variantInfo?: Record<string, string>;
}

export interface CreateOrderRequest {
  checkoutSessionId?: string;
  sellerId?: string;
  items?: OrderItemDto[];
  recipientName?: string;
  recipientPhone?: string;
  shippingAddress?: string;
  shippingWard?: string;
  shippingDistrict?: string;
  shippingCity?: string;
  paymentMethod: PaymentMethod;
  shippingFee?: number;
  voucherCode?: string;
  discountAmount?: number;
  buyerNote?: string;
}

// ─────────────────────────────────────────────────────────────────────────────
// Order Stats – Response DTOs
// ─────────────────────────────────────────────────────────────────────────────

export interface WaterfallChartData {
  baseProductRevenue: number;
  shopVouchers: number;
  shippingSubsidies: number;
  platformFees: number;
  paymentFees: number;
  commissionFees: number;
  serviceFees: number;
  payout: number;
}

export interface RevenueDataPoint {
  period: string;
  revenue: number;
  orderCount: number;
}

export interface CategoryRevenueData {
  categoryName: string;
  revenue: number;
  orderCount: number;
  percentage?: number;
}

export interface ProductSalesData {
  productId: string;
  productName: string;
  imageUrl?: string;
  totalQuantitySold: number;
  totalRevenue: number;
}

export interface DetailedReportSummaryResponse {
  totalGrossSales: number;
  netRevenue: number;
  totalOrderCount: number;
  cancellationRate: number;
  returnRate: number;
  averageOrderValue: number;
  waterfallChart: WaterfallChartData;
}

export interface DetailedReportTrendResponse {
  revenueByTime: RevenueDataPoint[];
  previousRevenueByTime?: RevenueDataPoint[];
}

export interface DetailedReportCategoryResponse {
  revenueByCategory: CategoryRevenueData[];
}

export interface DetailedReportProductSalesResponse {
  productsPage?: PageResponse<ProductSalesData>;
}

export interface DetailedReportResponse extends DetailedReportSummaryResponse, DetailedReportTrendResponse, DetailedReportCategoryResponse, DetailedReportProductSalesResponse {}

