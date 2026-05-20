import { createSlice } from "@reduxjs/toolkit";
import type { PayloadAction } from "@reduxjs/toolkit";
import type { AuthState } from "../types/auth";

// ─── Initial state ────────────────────────────────────────────────────────────
const initialState: AuthState = {
  accessToken: null,
  expiresAt: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,
  sessionRestored: false,
};

// ─── Slice (sync reducers only — thunks live in authThunks.ts) ────────────────
const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    /** Cập nhật token sau khi refresh thành công */
    setCredentials(
      state,
      action: PayloadAction<{ accessToken: string; expiresAt: number }>
    ) {
      state.accessToken = action.payload.accessToken;
      state.expiresAt = action.payload.expiresAt;
      state.isAuthenticated = true;
      state.error = null;
    },
    /** Xoá toàn bộ auth state (logout / 401 thất bại) */
    clearCredentials(state) {
      state.accessToken = null;
      state.expiresAt = null;
      state.isAuthenticated = false;
      state.error = null;
    },
    setLoading(state, action: PayloadAction<boolean>) {
      state.isLoading = action.payload;
    },
    setError(state, action: PayloadAction<string | null>) {
      state.error = action.payload;
      state.isLoading = false;
    },
    clearError(state) {
      state.error = null;
    },
    setSessionRestored(state) {
      state.sessionRestored = true;
    },
  },
});

export const { setCredentials, clearCredentials, setLoading, setError, clearError, setSessionRestored } = authSlice.actions;
export default authSlice.reducer;

