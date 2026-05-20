// ─────────────────────────────────────────────────────────────────────────────
// Enums
// ─────────────────────────────────────────────────────────────────────────────

export type SellerStatus =
  | "PENDING"
  | "ACTIVE"
  | "REJECTED"
  | "SUSPENDED"
  | "BANNED"
  | "CLOSED";

export type SellerType = "INDIVIDUAL" | "BUSINESS";

// ─────────────────────────────────────────────────────────────────────────────
// Response DTOs
// ─────────────────────────────────────────────────────────────────────────────

export interface SellerResponse {
  id: string;
  userId: string;
  shopName: string;
  shopSlug: string;
  description: string | null;
  logoUrl: string | null;
  bannerUrl: string | null;
  sellerType: SellerType;
  status: SellerStatus;
  email: string;
  phone: string;
  address: string;
  ward: string | null;
  district: string | null;
  city: string | null;
  country: string | null;
  businessName: string | null;
  taxCode: string | null;
  ratingAvg: number | null;
  ratingCount: number | null;
  totalProducts: number | null;
  totalOrders: number | null;
  followerCount: number | null;
  createdAt: string;
  approvedAt: string | null;
  rejectedAt: string | null;
  rejectionReason: string | null;
  statusNote: string | null;
}

// ─────────────────────────────────────────────────────────────────────────────
// Request DTOs
// ─────────────────────────────────────────────────────────────────────────────

export interface RegisterSellerRequest {
  shopName: string;
  description?: string;
  email: string;
  phone: string;
  address: string;
  ward?: string;
  district?: string;
  city?: string;
  sellerType: SellerType;
  // Business fields
  businessName?: string;
  taxCode?: string;
  businessLicenseNumber?: string;
}

// ─────────────────────────────────────────────────────────────────────────────
// Document types
// ─────────────────────────────────────────────────────────────────────────────

export type DocumentType =
  | "ID_CARD"
  | "PASSPORT"
  | "BUSINESS_LICENSE"
  | "TAX_CERTIFICATE"
  | "BANK_STATEMENT";

export type DocumentStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface SellerDocumentResponse {
  id: string;
  documentType: DocumentType;
  documentUrl: string;
  documentNumber: string | null;
  status: DocumentStatus;
  rejectionReason: string | null;
  verifiedAt: string | null;
  createdAt: string;
}

export interface UploadDocumentRequest {
  documentType: DocumentType;
  documentUrl: string;
  documentNumber?: string;
}

// ─────────────────────────────────────────────────────────────────────────────
// Bank Account types
// ─────────────────────────────────────────────────────────────────────────────

export interface BankAccountResponse {
  id: string;
  bankName: string;
  bankCode: string | null;
  branchName: string | null;
  accountNumber: string;
  accountHolderName: string;
  isVerified: boolean;
  createdAt: string;
}

export interface UpdateBankAccountRequest {
  bankName: string;
  bankCode?: string;
  branchName?: string;
  accountNumber: string;
  accountHolderName: string;
}
