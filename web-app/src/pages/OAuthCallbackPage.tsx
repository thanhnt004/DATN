/**
 * OAuthCallbackPage
 *
 * Handles the browser redirect from the backend after a successful
 * Google / Facebook OAuth2 login.
 *
 * The backend redirects to:
 *   http://localhost:5173/oauth/callback?access_token=<jwt>&expires_in=<seconds>
 *
 * This page:
 *  1. Reads the URL params
 *  2. Stores the access token + expiry in Redux (same shape as loginThunk)
 *  3. Redirects to the home page (or the originally requested page)
 *  4. Shows an error screen if params are missing / invalid
 */

import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useDispatch } from "react-redux";
import type { AppDispatch } from "../store/store";
import { setCredentials } from "../store/authSlice";
import { Store, Loader2 } from "lucide-react";

export default function OAuthCallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const dispatch = useDispatch<AppDispatch>();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const accessToken = searchParams.get("access_token");
    const expiresIn = searchParams.get("expires_in");
    const errorParam = searchParams.get("error");
    const message = searchParams.get("message");

    if (errorParam) {
      setError(message ?? "Social login failed. Please try again.");
      return;
    }

    if (!accessToken) {
      setError("No access token received. Please try logging in again.");
      return;
    }

    const expiresInSeconds = expiresIn ? parseInt(expiresIn, 10) : 300;
    const expiresAt = Date.now() + expiresInSeconds * 1000;

    // Store in Redux — the refresh token was already set as an HttpOnly cookie by the backend
    dispatch(setCredentials({ accessToken, expiresAt }));

    // Navigate home
    navigate("/", { replace: true });
  }, [dispatch, navigate, searchParams]);

  if (error) {
    return (
      <div className="min-h-screen bg-white flex flex-col items-center justify-center gap-6 p-8">
        <div className="w-16 h-16 rounded-full bg-red-100 flex items-center justify-center">
          <span className="text-red-600 text-2xl font-black">!</span>
        </div>
        <div className="text-center space-y-2 max-w-sm">
          <h1 className="text-2xl font-black text-slate-900">Đăng nhập thất bại</h1>
          <p className="text-slate-500 text-sm font-medium">{error}</p>
        </div>
        <button
          onClick={() => navigate("/login", { replace: true })}
          className="px-8 py-3 bg-red-600 text-white font-black rounded-xl hover:bg-red-700 transition-all text-sm uppercase tracking-widest"
        >
          Quay lại đăng nhập
        </button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-white flex flex-col items-center justify-center gap-6">
      <div className="flex items-center gap-3 text-red-600 font-black text-3xl tracking-tighter">
        <Store className="w-8 h-8" />
        <span>SELLICO</span>
      </div>
      <div className="flex items-center gap-3 text-slate-500">
        <Loader2 className="w-5 h-5 animate-spin text-red-600" />
        <span className="font-semibold text-sm">Đang xác thực...</span>
      </div>
    </div>
  );
}
