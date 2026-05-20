import axios from "axios";
import type { AxiosError, InternalAxiosRequestConfig } from "axios";
import { store } from "../store/store";
import { setCredentials, clearCredentials } from "../store/authSlice";

// ─── Base instance ────────────────────────────────────────────────────────────
const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "",
  withCredentials: true, // gửi httpOnly refresh-token cookie tự động
  headers: { "Content-Type": "application/json" },
});

// ─── Helpers ───────────────────────────────────────────────────────────────────
const AUTH_URLS = [
  "/api/v1/identity/auth/refresh-token",
  "/api/v1/identity/auth/login",
  "/api/v1/identity/auth/register",
];

function isAuthUrl(url?: string): boolean {
  return AUTH_URLS.some((u) => url?.includes(u));
}

// ─── Refresh token queue (tránh gọi nhiều lần khi có nhiều request 401) ─────
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null = null) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (token) resolve(token);
    else reject(error);
  });
  failedQueue = [];
}

/** Call the refresh-token endpoint directly (raw axios, no interceptors). */
async function callRefreshToken(): Promise<{ accessToken: string; expiresIn: number }> {
  try {
    const { data } = await axios.post(
      `${axiosInstance.defaults.baseURL}/api/v1/identity/auth/refresh-token`,
      {},
      { withCredentials: true }
    );
    // Backend shape: { success, code, message, data: { accessToken, expiresIn, ... } }
    return data.data;
  } catch (error: any) {
    // Nếu 403 từ refresh-token, token đã invalid, logout ngay
    if (error.response?.status === 403) {
      store.dispatch(clearCredentials());
      window.location.href = "/login";
      throw new Error("Refresh token expired, please login again");
    }
    throw error;
  }
}

/**
 * Perform a token refresh, store the new credentials, and return the new access token.
 * Guards against concurrent calls using isRefreshing + failedQueue.
 */
function doRefresh(): Promise<string> {
  if (isRefreshing) {
    return new Promise<string>((resolve, reject) => {
      failedQueue.push({ resolve, reject });
    });
  }

  isRefreshing = true;

  return callRefreshToken()
    .then(({ accessToken, expiresIn }) => {
      store.dispatch(
        setCredentials({
          accessToken,
          expiresAt: Date.now() + expiresIn * 1000,
        })
      );
      processQueue(null, accessToken);
      return accessToken;
    })
    .catch((err) => {
      processQueue(err, null);
      // Không dispatch clearCredentials ở đây vì đã handle trong callRefreshToken
      throw err;
    })
    .finally(() => {
      isRefreshing = false;
    });
}

// ─── Request interceptor: attach access token + proactive refresh ──────────
axiosInstance.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
  // Không gắn token hoặc refresh cho các endpoint auth
  if (isAuthUrl(config.url)) {
    return config;
  }

  let token = store.getState().auth.accessToken;
  const expiresAt = store.getState().auth.expiresAt;

  // Proactive refresh: nếu token sắp hết hạn (trong 60s), refresh trước
  if (token && expiresAt && expiresAt - Date.now() < 60_000 && !isRefreshing) {
    try {
      token = await doRefresh();
    } catch {
      // Refresh thất bại — để request đi với token cũ, 401 interceptor sẽ xử lý
    }
  }

  if (token && config.headers) {
    config.headers["Authorization"] = `Bearer ${token}`;
  }
  return config;
});

// ─── Response normalizer: map backend shape → frontend shape ───────────────
// Backend returns { success, code, message, data } for success
//                 { success, errorCode, message, ... } for errors
// Frontend expects { code, result, message }
function normalizeResponseBody(body: Record<string, unknown>): void {
  if (body && typeof body === "object") {
    if ("data" in body && !("result" in body)) {
      body.result = body.data;
    }
    if ("errorCode" in body && !("code" in body)) {
      body.code = body.errorCode;
    }
  }
}

axiosInstance.interceptors.response.use(
  (response) => {
    normalizeResponseBody(response.data);
    return response;
  },
  (error: AxiosError) => {
    if (error.response?.data && typeof error.response.data === "object") {
      normalizeResponseBody(error.response.data as Record<string, unknown>);
    }
    return Promise.reject(error);
  },
);

// ─── Response interceptor: auto refresh on 401 ────────────────────────────
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };

    // Bỏ qua nếu 401 đến từ chính endpoint auth
    if (isAuthUrl(originalRequest?.url)) {
      return Promise.reject(error);
    }

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const newToken = await doRefresh();
        originalRequest.headers["Authorization"] = `Bearer ${newToken}`;
        return axiosInstance(originalRequest);
      } catch {
        return Promise.reject(error);
      }
    }

    return Promise.reject(error);
  }
);

export default axiosInstance;
