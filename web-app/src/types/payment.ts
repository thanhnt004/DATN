// ─────────────────────────────────────────────────────────────────────────────
// Payment – Enums & Response DTOs
// ─────────────────────────────────────────────────────────────────────────────

export type PaymentServiceStatus =
  | "PENDING"
  | "COMPLETED"
  | "FAILED"
  | "REFUNDED"
  | "EXPIRED"
  | "COD_PENDING"
  | "COD_CONFIRMED";

export type RefundStatus = "PENDING" | "COMPLETED" | "FAILED";

export interface PaymentResponse {
  id: string;
  orderId: string;
  userId: string;
  amount: number;
  currency: string;
  paymentMethod: string;
  status: string;
  vnpayTxnRef: string | null;
  vnpayTransactionNo: string | null;
  bankCode: string | null;
  cardType: string | null;
  payDate: string | null;
  failureReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface VnPayCreateResponse {
  paymentUrl: string;
}

export interface RefundResponse {
  id: string;
  paymentId: string;
  amount: number;
  reason: string;
  status: string;
  createdAt: string;
}

// ─────────────────────────────────────────────────────────────────────────────
// Payment – Request DTOs
// ─────────────────────────────────────────────────────────────────────────────
