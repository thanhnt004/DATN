// ─────────────────────────────────────────────────────────────────────────────
// Coupon / Discount – Enums & Response DTOs
// ─────────────────────────────────────────────────────────────────────────────

export type CouponType = "PLATFORM" | "SHOP";
export type DiscountType = "PERCENTAGE" | "FIXED_AMOUNT";
export type CouponStatus = "ACTIVE" | "INACTIVE" | "EXPIRED" | "DRAFT";
export type UserCouponStatus = "AVAILABLE" | "USED" | "EXPIRED";
export type RuleType = "APPLICABLE_CATEGORY" | "APPLICABLE_PRODUCT" | "EXCLUDED_PRODUCT";
export type CampaignType = "PLATFORM" | "SHOP";
export type CampaignStatus = "DRAFT" | "ACTIVE" | "ENDED" | "CANCELLED";

export interface CouponResponse {
  id: string;
  campaignId: string | null;
  sellerId: string | null;
  code: string;
  couponType: string;
  discountType: string;
  discountValue: number;
  minOrderAmount: number | null;
  maxDiscountAmount: number | null;
  totalQuantity: number;
  claimedQuantity: number;
  usedQuantity: number;
  maxUsagePerUser: number;
  isStackable: boolean;
  status: string;
  startDate: string;
  endDate: string;
  createdAt: string;
  remainingQuantity: number;
}

export interface UserCouponResponse {
  id: string;
  couponId: string;
  status: string;
  claimedAt: string;
  usedAt: string | null;
  couponCode: string;
  discountType: string;
  discountValue: number;
  minOrderAmount: number | null;
  maxDiscountAmount: number | null;
  couponEndDate: string;
}

export interface CampaignResponse {
  id: string;
  sellerId: string | null;
  name: string;
  description: string | null;
  campaignType: string;
  status: string;
  startDate: string;
  endDate: string;
  createdAt: string;
  updatedAt: string;
}

export interface ApplyCouponResult {
  couponId: string;
  discountAmount: number;
  finalAmount: number;
}

// ─────────────────────────────────────────────────────────────────────────────
// Coupon / Discount – Request DTOs
// ─────────────────────────────────────────────────────────────────────────────

export interface RuleItem {
  ruleType: RuleType;
  targetId: string;
}

export interface CreateCouponRequest {
  campaignId?: string;
  sellerId?: string;
  code: string;
  couponType: string;
  discountType: string;
  discountValue: number;
  minOrderAmount?: number;
  maxDiscountAmount?: number;
  totalQuantity: number;
  maxUsagePerUser?: number;
  isStackable?: boolean;
  startDate: string;
  endDate: string;
  rules?: RuleItem[];
}

export interface UpdateCouponRequest {
  code?: string;
  discountType?: string;
  discountValue?: number;
  minOrderAmount?: number;
  maxDiscountAmount?: number;
  totalQuantity?: number;
  maxUsagePerUser?: number;
  isStackable?: boolean;
  status?: string;
  startDate?: string;
  endDate?: string;
}

export interface ApplyCouponRequest {
  couponId: string;
  orderAmount: number;
}

export interface CreateCampaignRequest {
  sellerId?: string;
  name: string;
  description?: string;
  campaignType: string;
  startDate: string;
  endDate: string;
}

export interface UpdateCampaignRequest {
  name?: string;
  description?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
}
