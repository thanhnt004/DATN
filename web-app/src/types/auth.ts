// ─────────────────────────────────────────────────────────────────────────────
// Generic wrapper – mirrors backend ApiResponse<T>
// Backend sends { success, code, message, data }, normalizer maps data→result
// ─────────────────────────────────────────────────────────────────────────────
export interface ApiResponse<T> {
  success?: boolean;
  code: string;
  result: T;
  message?: string;
}

// ─────────────────────────────────────────────────────────────────────────────
// Auth – Request DTOs  (mirrors Java DTOs exactly)
// ─────────────────────────────────────────────────────────────────────────────

/** POST /api/v1/identity/auth/login */
export interface LoginRequest {
  identifier: string; // username | email | phone
  password: string;
}

/** POST /api/v1/identity/auth/register/buyer */
export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
  phone: string;
}

/** POST /api/v1/identity/auth/forgot-password */
export interface ForgotPasswordRequest {
  email: string;
}

/** POST /api/v1/identity/reset-password */
export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
  confirmPassword: string;
}

/** PUT /api/v1/identity/change-password  (authenticated) */
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

/** POST /api/v1/identity/auth/resend-verification */
export interface ResendVerificationRequest {
  email: string;
}

/** POST /api/v1/identity/auth/google/token  (SPA OAuth2 code exchange) */
export interface GoogleTokenRequest {
  code: string;
  redirect_uri?: string;
}

/** POST /api/v1/identity/auth/facebook/token  (SPA OAuth2 code exchange) */
export interface FacebookTokenRequest {
  code: string;
  redirect_uri?: string;
}

// ─────────────────────────────────────────────────────────────────────────────
// Auth – Response DTOs
// ─────────────────────────────────────────────────────────────────────────────

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
}

export interface BuyerRegisterResponse {
  userId: string;
  email: string;
  username: string;
}

export interface ValidateResetTokenResponse {
  valid: boolean;
}

// ─────────────────────────────────────────────────────────────────────────────
// Auth – Redux State
// ─────────────────────────────────────────────────────────────────────────────
export interface AuthState {
  accessToken: string | null;
  expiresAt: number | null; // unix ms
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  /** true after the initial refresh-token attempt completes (success or fail) */
  sessionRestored: boolean;
}
