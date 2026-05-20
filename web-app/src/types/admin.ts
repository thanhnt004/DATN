import type { PageResponse } from "./user";

// ─────────────────────────────────────────────────────────────────────────────
// Re-export common
// ─────────────────────────────────────────────────────────────────────────────
export type { PageResponse };

// ─────────────────────────────────────────────────────────────────────────────
// User management  (mirrors user-service)
// ─────────────────────────────────────────────────────────────────────────────
export type UserStatus =
  | "ACTIVE"
  | "LOCKED"
  | "BANNED"
  | "DEACTIVATED"
  | "SUSPENDED"
  | "DELETED";

export interface AdminUserResponse {
  id: string;
  authId: string;
  username: string;
  email: string;
  phone: string;
  fullName: string | null;
  avatarUrl: string | null;
  userType: "BUYER" | "SELLER";
  status: UserStatus;
  isVerified: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AdminUpdateUserStatusRequest {
  status: UserStatus;
  reason?: string;
}

// ─────────────────────────────────────────────────────────────────────────────
// Seller management (mirrors seller-service)
// ─────────────────────────────────────────────────────────────────────────────
export type SellerStatus =
  | "PENDING"
  | "ACTIVE"
  | "REJECTED"
  | "SUSPENDED"
  | "BANNED"
  | "CLOSED";

export interface AdminSellerResponse {
  id: string;
  userId: string;
  shopName: string;
  shopSlug: string;
  sellerType: "INDIVIDUAL" | "BUSINESS";
  status: SellerStatus;
  email: string;
  phone: string;
  city: string | null;
  ratingAvg: number | null;
  totalProducts: number | null;
  totalOrders: number | null;
  createdAt: string;
  approvedAt: string | null;
  rejectedAt: string | null;
  rejectionReason: string | null;
  statusNote: string | null;
}

export interface SellerStatsResponse {
  totalSellers: number;
  pendingSellers: number;
  activeSellers: number;
  rejectedSellers: number;
  suspendedSellers: number;
  bannedSellers: number;
  closedSellers: number;
}

export interface AdminOrderStatsResponse {
  totalOrders: number;
  totalRevenue: number;
  pendingOrders: number;
  confirmedOrders: number;
  shippedOrders: number;
  deliveredOrders: number;
  completedOrders: number;
  cancelledOrders: number;
  ordersByStatus: Record<string, number>;
  revenueByStatus: Record<string, number>;
}

export interface RejectSellerRequest {
  reason: string;
}

// ─────────────────────────────────────────────────────────────────────────────
// Banner management (mirrors banner-service)
// ─────────────────────────────────────────────────────────────────────────────
export type BannerStatus = "DRAFT" | "ACTIVE" | "INACTIVE" | "SCHEDULED" | "EXPIRED";
export type LinkType = "NONE" | "URL" | "PRODUCT" | "CATEGORY" | "CAMPAIGN" | "SELLER";
export type TargetAudience = "ALL" | "NEW_USER" | "RETURNING_USER" | "VIP";

export interface BannerResponse {
  id: string;
  title: string;
  imageUrl: string;
  linkUrl: string | null;
  linkType: LinkType | null;
  linkValue: string | null;
  positionCode: string;
  sortOrder: number;
  status: BannerStatus;
  startDate: string | null;
  endDate: string | null;
  targetAudience: TargetAudience;
  clickCount: number;
  viewCount: number;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface BannerPositionResponse {
  code: string;
  name: string;
  description: string | null;
  maxBanners: number;
  isActive: boolean;
  createdAt: string;
}

export interface CreateBannerRequest {
  title: string;
  imageUrl: string;
  linkUrl?: string;
  linkType?: LinkType;
  linkValue?: string;
  positionCode: string;
  sortOrder?: number;
  status?: BannerStatus;
  startDate?: string;
  endDate?: string;
  targetAudience?: TargetAudience;
}

export interface UpdateBannerRequest {
  title?: string;
  imageUrl?: string;
  linkUrl?: string;
  linkType?: LinkType;
  linkValue?: string;
  positionCode?: string;
  sortOrder?: number;
  startDate?: string;
  endDate?: string;
  targetAudience?: TargetAudience;
}

// ─────────────────────────────────────────────────────────────────────────────
// Category management (mirrors category-service)
// ─────────────────────────────────────────────────────────────────────────────
export type CategoryStatus = "ACTIVE" | "INACTIVE" | "DELETED";

export interface CategoryResponse {
  id: string;
  parentId: string | null;
  name: string;
  slug: string;
  path: string;
  level: number;
  status: CategoryStatus;
  createdAt: string;
  breadcrumbs: { id: string; name: string; slug: string }[];
}

export interface CategoryTreeResponse {
  id: string;
  parentId: string | null;
  name: string;
  slug: string;
  iconUrl: string | null;
  imageUrl: string | null;
  level: number;
  path: string;
  sortOrder: number;
  description?: string;
  status: CategoryStatus;
  children: CategoryTreeResponse[];
}

// ── Category Attribute DTOs ──

export interface CategoryAttributeValueRequest {
  value: string;
}

export interface CategoryAttributeRequest {
  name: string;
  isRequired?: boolean;
  isFilterable?: boolean;
  dataType?: "string" | "number" | "boolean" | "enum";
  predefinedValues?: CategoryAttributeValueRequest[];
}

export interface CategoryAttributeValueResponse {
  id: string;
  value: string;
  sortOrder: number;
}

export interface CategoryAttributeResponse {
  id: string;
  value: string;          // attribute name (backend field is named "value")
  required: boolean;
  filterable: boolean;
  dataType: string;
  predefinedValues: CategoryAttributeValueResponse[];
}

// ── Category CRUD DTOs ──

export interface CreateCategoryRequest {
  name: string;
  parentId?: string;
  description?: string;
  iconUrl?: string;
  imageUrl?: string;
  status?: CategoryStatus;
  sortOrder?: number;
  attributes?: CategoryAttributeRequest[];
}

export interface UpdateCategoryRequest {
  name?: string;
  description?: string;
  iconUrl?: string;
  imageUrl?: string;
  status?: CategoryStatus;
  sortOrder?: number;
}

// ── Option Template DTOs ──

export interface OptionTemplateValueResponse {
  id: string;
  value: string;
  sortOrder: number;
}

export interface OptionTemplateResponse {
  id: string;
  name: string;
  source: "ADMIN" | "SELLER";
  sellerId: string | null;
  createdAt: string;
  values: OptionTemplateValueResponse[];
}

export interface CreateOptionTemplateRequest {
  name: string;
  values?: string[];
}

export interface UpdateOptionTemplateRequest {
  name: string;
  values?: string[];
}
