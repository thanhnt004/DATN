import axiosInstance from "./axiosInstance";
import axios from "axios";
import type {
  LoginRequest,
  RegisterRequest,
  TokenResponse,
  BuyerRegisterResponse,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  ChangePasswordRequest,
  ValidateResetTokenResponse,
  GoogleTokenRequest,
  FacebookTokenRequest,
  ResendVerificationRequest,
  ApiResponse,
} from "../types/auth";

// Identity-service base paths
// NOTE: auth/* sub-resources live under /api/v1/identity/auth
//       password-reset & google/* live directly under /api/v1/identity
const AUTH = "/api/v1/identity/auth";
const IDENTITY = "/api/v1/identity";

// ─── Login ────────────────────────────────────────────────────────────────────

/** POST /api/v1/identity/auth/login */
export async function login(payload: LoginRequest): Promise<TokenResponse> {
  const { data } = await axiosInstance.post<ApiResponse<TokenResponse>>(
    `${AUTH}/login`,
    payload
  );
  return data.result;
}

// ─── Register ─────────────────────────────────────────────────────────────────

/** POST /api/v1/identity/auth/register/buyer */
export async function registerBuyer(
  payload: RegisterRequest
): Promise<BuyerRegisterResponse> {
  const { data } = await axiosInstance.post<ApiResponse<BuyerRegisterResponse>>(
    `${AUTH}/register/buyer`,
    payload
  );
  return data.result;
}

// ─── Token management ─────────────────────────────────────────────────────────

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

/**
 * POST /api/v1/identity/auth/refresh-token  (httpOnly cookie sent automatically)
 * Uses raw axios (NOT axiosInstance) to avoid sending the expired Bearer token
 * in the Authorization header — the refresh endpoint only needs the cookie.
 */
export async function refreshToken(): Promise<TokenResponse> {
  const { data } = await axios.post<{ data: TokenResponse }>(
    `${BASE_URL}${AUTH}/refresh-token`,
    {},
    { withCredentials: true }
  );
  return data.data;
}

// ─── Logout ───────────────────────────────────────────────────────────────────

/** POST /api/v1/identity/auth/logout */
export async function logout(): Promise<void> {
  await axiosInstance.post(`${AUTH}/logout`);
}

/** POST /api/v1/identity/auth/logout-all */
export async function logoutAllDevices(): Promise<void> {
  await axiosInstance.post(`${AUTH}/logout-all`);
}

// ─── Email verification ──────────────────────────────────────────────────────

/**
 * POST /api/v1/identity/auth/resend-verification
 * Requests a new verification e-mail. No authentication required.
 */
export async function resendVerification(
  payload: ResendVerificationRequest
): Promise<string> {
  const { data } = await axiosInstance.post<ApiResponse<string>>(
    `${AUTH}/resend-verification`,
    payload
  );
  return data.result;
}

// ─── Password reset flow ──────────────────────────────────────────────────────

/** POST /api/v1/identity/auth/forgot-password  →  sends reset e-mail */
export async function forgotPassword(
  payload: ForgotPasswordRequest
): Promise<string> {
  const { data } = await axiosInstance.post<ApiResponse<string>>(
    `${AUTH}/forgot-password`,
    payload
  );
  return data.result;
}

/** GET /api/v1/identity/validate-reset-token?token=xxx */
export async function validateResetToken(
  token: string
): Promise<ValidateResetTokenResponse> {
  const { data } = await axiosInstance.get<
    ApiResponse<ValidateResetTokenResponse>
  >(`${IDENTITY}/validate-reset-token`, { params: { token } });
  return data.result;
}

/** POST /api/v1/identity/reset-password */
export async function resetPassword(
  payload: ResetPasswordRequest
): Promise<string> {
  const { data } = await axiosInstance.post<ApiResponse<string>>(
    `${IDENTITY}/reset-password`,
    payload
  );
  return data.result;
}

// ─── Change password (authenticated) ─────────────────────────────────────────

/** PUT /api/v1/identity/change-password  (requires Bearer token) */
export async function changePassword(
  payload: ChangePasswordRequest
): Promise<string> {
  const { data } = await axiosInstance.put<ApiResponse<string>>(
    `${IDENTITY}/change-password`,
    payload
  );
  return data.result;
}

// ─── Google OAuth2 ────────────────────────────────────────────────────────────

/**
 * GET /api/v1/identity/auth/google?redirect_uri=…
 * Redirect the browser to this URL to start the Google OAuth2 flow.
 * NOTE: Used for building redirect URL; actual navigation is window.location.href.
 */
export function buildGoogleOAuthRedirectUrl(redirectUri?: string): string {
  const base = `${import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"}${AUTH}/google`;
  return redirectUri ? `${base}?redirect_uri=${encodeURIComponent(redirectUri)}` : base;
}

/**
 * POST /api/v1/identity/auth/google/token
 * Exchanges the authorization code for application access/refresh tokens (SPA flow).
 */
export async function exchangeGoogleCode(
  payload: GoogleTokenRequest
): Promise<TokenResponse> {
  const { data } = await axiosInstance.post<ApiResponse<TokenResponse>>(
    `${AUTH}/google/token`,
    payload
  );
  return data.result;
}

// ─── Facebook OAuth2 ──────────────────────────────────────────────────────────

/**
 * GET /api/v1/identity/auth/facebook?redirect_uri=…
 * Redirect the browser to this URL to start the Facebook OAuth2 flow.
 * NOTE: Used for building redirect URL; actual navigation is window.location.href.
 */
export function buildFacebookOAuthRedirectUrl(redirectUri?: string): string {
  const base = `${import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"}${AUTH}/facebook`;
  return redirectUri ? `${base}?redirect_uri=${encodeURIComponent(redirectUri)}` : base;
}

/**
 * POST /api/v1/identity/auth/facebook/token
 * Exchanges the authorization code for application access/refresh tokens (SPA flow).
 */
export async function exchangeFacebookCode(
  payload: FacebookTokenRequest
): Promise<TokenResponse> {
  const { data } = await axiosInstance.post<ApiResponse<TokenResponse>>(
    `${AUTH}/facebook/token`,
    payload
  );
  return data.result;
}
