// ─────────────────────────────────────────────────────────────────────────────
// Enums
// ─────────────────────────────────────────────────────────────────────────────

export type Gender = "MALE" | "FEMALE" | "OTHER";

export type UserType = "BUYER" | "SELLER";

/** Matches UserStatus enum in user-service */
export type UserStatus = "ACTIVE" | "INACTIVE" | "BANNED" | "DEACTIVATED";

// ─────────────────────────────────────────────────────────────────────────────
// Response DTOs
// ─────────────────────────────────────────────────────────────────────────────

/** Full profile — returned for authenticated user (/me endpoints) */
export interface UserProfileResponse {
  id: string;
  username: string;
  email: string;
  phone: string;
  userType: UserType;
  status: UserStatus;
  fullName: string | null;
  gender: Gender | null;
  /** ISO-8601 date string, e.g. "1999-07-15" */
  dateOfBirth: string | null;
  avatarUrl: string | null;
  /** ISO-8601 instant string */
  updatedAt: string;
  createdAt: string;
}

/** Limited public profile — returned for GET /api/v1/users/{userId} (no auth) */
export interface UserPublicResponse {
  id: string;
  username: string;
  userType: UserType;
  fullName: string | null;
  avatarUrl: string | null;
  createdAt: string;
}

/** Address entry */
export interface AddressResponse {
  id: string;
  receiverName: string;
  receiverPhone: string;
  province: string;
  district: string;
  ward: string;
  addressLine: string;
  /** Backend-computed: full single-line string */
  fullAddress: string;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

/** Spring Page<T> wrapper */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // current page (0-indexed)
  first: boolean;
  last: boolean;
  empty: boolean;
}

// ─────────────────────────────────────────────────────────────────────────────
// Request DTOs
// ─────────────────────────────────────────────────────────────────────────────

/** PUT /api/v1/users/me */
export interface UpdateProfileRequest {
  fullName?: string;
  gender?: Gender;
  /** ISO-8601 date string "YYYY-MM-DD" */
  dateOfBirth?: string;
  avatarUrl?: string;
  phone?: string;
}

/** PATCH /api/v1/users/me/email */
export interface ChangeEmailRequest {
  newEmail: string;
}

/** POST /api/v1/users/me/deactivate */
export interface DeactivateAccountRequest {
  reason: string;
}

/** POST /api/v1/users/me/addresses */
export interface CreateAddressRequest {
  receiverName: string;
  receiverPhone: string;
  province: string;
  district: string;
  ward: string;
  addressLine: string;
  isDefault?: boolean;
}

/** PUT /api/v1/users/me/addresses/{addressId} */
export interface UpdateAddressRequest {
  receiverName?: string;
  receiverPhone?: string;
  province?: string;
  district?: string;
  ward?: string;
  addressLine?: string;
}

/** PATCH /api/v1/admin/users/{userId}/status */
export interface AdminUpdateUserStatusRequest {
  status: UserStatus;
  reason?: string;
}

/** Query params for GET /api/v1/admin/users */
export interface AdminUserSearchParams {
  keyword?: string;
  status?: UserStatus;
  page?: number;
  size?: number;
}
