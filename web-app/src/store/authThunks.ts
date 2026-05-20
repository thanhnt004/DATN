import { createAsyncThunk } from "@reduxjs/toolkit";
import * as authApi from "../api/authApi";
import { setCredentials, clearCredentials, setLoading, setError, setSessionRestored } from "./authSlice";
import type { LoginRequest, RegisterRequest } from "../types/auth";
import type { AppDispatch } from "./store";

// ─── Helper ───────────────────────────────────────────────────────────────────
function extractApiError(err: unknown): { code: string; message: string } {
  const e = err as { response?: { data?: { code?: string; message?: string } } };
  return {
    code: e?.response?.data?.code ?? "UNKNOWN",
    message: e?.response?.data?.message ?? "Có lỗi xảy ra. Vui lòng thử lại.",
  };
}

export interface AuthError {
  code: string;
  message: string;
}

// ─── Login ────────────────────────────────────────────────────────────────────
export const loginThunk = createAsyncThunk<
  Awaited<ReturnType<typeof authApi.login>>,
  LoginRequest,
  { rejectValue: AuthError }
>(
  "auth/login",
  async (payload, { dispatch, rejectWithValue }) => {
    const d = dispatch as AppDispatch;
    d(setLoading(true));
    d(setError(null));
    try {
      const tokens = await authApi.login(payload);
      console.log('[loginThunk] SUCCESS - accessToken:', tokens.accessToken?.substring(0, 30) + '...');
      console.log('[loginThunk] expiresIn:', tokens.expiresIn);
      d(setCredentials({ accessToken: tokens.accessToken, expiresAt: Date.now() + tokens.expiresIn * 1000 }));
      d(setLoading(false));
      return tokens;
    } catch (err) {
      const apiErr = extractApiError(err);
      d(setLoading(false));
      d(setError(apiErr.message));
      return rejectWithValue(apiErr);
    }
  }
);

// ─── Register ─────────────────────────────────────────────────────────────────
export const registerThunk = createAsyncThunk(
  "auth/register",
  async (payload: RegisterRequest, { dispatch, rejectWithValue }) => {
    const d = dispatch as AppDispatch;
    d(setLoading(true));
    d(setError(null));
    try {
      const result = await authApi.registerBuyer(payload);
      d(setLoading(false));
      return result;
    } catch (err) {
      const { message } = extractApiError(err);
      d(setLoading(false));
      d(setError(message));
      return rejectWithValue(message);
    }
  }
);

// ─── Logout ───────────────────────────────────────────────────────────────────
export const logoutThunk = createAsyncThunk(
  "auth/logout",
  async (_, { dispatch }) => {
    try {
      await authApi.logout();
    } finally {
      (dispatch as AppDispatch)(clearCredentials());
    }
  }
);

// ─── Restore session on app boot (calls refresh-token using httpOnly cookie) ─
export const refreshSessionThunk = createAsyncThunk(
  "auth/refreshSession",
  async (_, { dispatch }) => {
    const d = dispatch as AppDispatch;
    console.log('[refreshSession] starting refresh-token call...');
    try {
      const tokens = await authApi.refreshToken();
      console.log('[refreshSession] SUCCESS - got accessToken:', tokens.accessToken?.substring(0, 30) + '...');
      d(setCredentials({ accessToken: tokens.accessToken, expiresAt: Date.now() + tokens.expiresIn * 1000 }));
      return tokens;
    } catch (err) {
      console.error('[refreshSession] FAILED:', (err as Error)?.message || err);
      d(clearCredentials());
    } finally {
      console.log('[refreshSession] setting sessionRestored = true');
      d(setSessionRestored());
    }
  }
);
