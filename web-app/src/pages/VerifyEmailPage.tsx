import { useState, useEffect, useCallback } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  MailCheck,
  Store,
  RefreshCw,
  CheckCircle,
  AlertCircle,
  ArrowLeft,
  Inbox,
} from "lucide-react";
import { resendVerification } from "../api/authApi";

const COOLDOWN_SECONDS = 60;

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const email = searchParams.get("email") ?? "";
  const justRegistered = searchParams.get("registered") === "true";

  const [countdown, setCountdown] = useState(justRegistered ? COOLDOWN_SECONDS : 0);
  const [isSending, setIsSending] = useState(false);
  const [status, setStatus] = useState<"idle" | "success" | "error">("idle");
  const [statusMsg, setStatusMsg] = useState("");

  // Countdown timer
  useEffect(() => {
    if (countdown <= 0) return;
    const id = setInterval(() => setCountdown((c) => c - 1), 1000);
    return () => clearInterval(id);
  }, [countdown]);

  const handleResend = useCallback(async () => {
    if (!email || countdown > 0 || isSending) return;
    setIsSending(true);
    setStatus("idle");
    try {
      await resendVerification({ email });
      setStatus("success");
      setStatusMsg("Email xác thực đã được gửi lại. Vui lòng kiểm tra hộp thư.");
      setCountdown(COOLDOWN_SECONDS);
    } catch (err) {
      const e = err as { response?: { data?: { message?: string } } };
      const msg = e?.response?.data?.message ?? "Không thể gửi email. Vui lòng thử lại.";
      setStatus("error");
      setStatusMsg(msg);
    } finally {
      setIsSending(false);
    }
  }, [email, countdown, isSending]);

  return (
    <div className="md:h-screen bg-white flex flex-col md:flex-row overflow-hidden">
      {/* ── Left: Branding ─────────────────────────────────────────────── */}
      <div className="hidden md:flex w-1/2 bg-red-600 p-8 lg:p-16 flex-col justify-center items-center text-center text-white relative">
        <div className="absolute top-0 right-0 w-[400px] h-[400px] bg-white/5 rounded-full -mr-48 -mt-48 blur-3xl" />
        <div className="absolute bottom-0 left-0 w-[500px] h-[500px] bg-black/5 rounded-full -ml-64 -mb-64 blur-3xl" />

        <div className="relative z-10 flex flex-col items-center space-y-6 lg:space-y-8">
          <div className="bg-white/10 p-8 lg:p-10 rounded-[40px] backdrop-blur-2xl border border-white/20 shadow-xl hover:scale-105 transition-transform duration-500">
            <Store className="w-24 h-24 lg:w-32 lg:h-32 text-white" />
          </div>
          <div className="space-y-4">
            <h1 className="text-6xl lg:text-7xl font-black tracking-tighter leading-none select-none">
              SELLICO
            </h1>
            <div className="w-20 h-2 bg-white rounded-full mx-auto opacity-80" />
            <p className="text-lg lg:text-xl text-red-500 bg-white/95 px-6 py-2 rounded-full font-bold tracking-wide shadow-lg inline-block">
              Premium Shopping Experience
            </p>
          </div>
        </div>

        <div className="absolute bottom-8 flex flex-col items-center gap-3">
          <div className="text-red-100/60 text-[10px] font-black tracking-[0.3em] uppercase select-none text-center">
            Establishing Quality • Since 2026
          </div>
        </div>
      </div>

      {/* ── Right: Content ──────────────────────────────────────────────── */}
      <div className="w-full md:w-1/2 flex items-center justify-center p-6 sm:p-10 lg:p-16 bg-white h-screen md:h-full">
        <div className="w-full max-w-md space-y-8">
          {/* Mobile logo */}
          <div className="md:hidden flex justify-center">
            <div className="flex items-center gap-2 text-red-600 font-black text-2xl tracking-tighter">
              <Store className="w-6 h-6" />
              <span>SELLICO</span>
            </div>
          </div>

          {/* Mail icon (animated) */}
          <div className="flex justify-center">
            <div className="relative">
              <div className="w-24 h-24 bg-red-50 rounded-full flex items-center justify-center ring-8 ring-red-100">
                <MailCheck className="w-11 h-11 text-red-600" strokeWidth={1.5} />
              </div>
              {/* Pulsing ring */}
              <span className="absolute inset-0 rounded-full animate-ping bg-red-200 opacity-30" />
            </div>
          </div>

          {/* Heading */}
          <div className="text-center space-y-2">
            <h2 className="text-3xl font-black text-gray-900 tracking-tight">
              Kiểm tra hộp thư của bạn
            </h2>
            <p className="text-gray-500 text-sm leading-relaxed">
              Chúng tôi đã gửi đường dẫn xác thực tới
            </p>
            {email && (
              <p className="font-bold text-gray-800 text-base break-all">
                {email}
              </p>
            )}
          </div>

          {/* Steps hint */}
          <div className="bg-gray-50 rounded-2xl p-5 space-y-3">
            <p className="text-xs font-bold text-gray-500 uppercase tracking-widest">
              Các bước tiếp theo
            </p>
            {[
              { icon: Inbox, text: "Mở hộp thư đến (hoặc thư mục Spam)" },
              { icon: MailCheck, text: "Nhấn vào đường dẫn xác thực trong email" },
              { icon: CheckCircle, text: "Quay lại đây và đăng nhập" },
            ].map(({ icon: Icon, text }, i) => (
              <div key={i} className="flex items-center gap-3">
                <div className="w-7 h-7 rounded-full bg-red-100 flex items-center justify-center shrink-0">
                  <Icon className="w-3.5 h-3.5 text-red-600" />
                </div>
                <span className="text-sm text-gray-600">{text}</span>
              </div>
            ))}
          </div>

          {/* Status banner */}
          {status !== "idle" && (
            <div
              className={`flex items-start gap-3 px-4 py-3 rounded-xl text-sm ${
                status === "success"
                  ? "bg-green-50 text-green-700"
                  : "bg-red-50 text-red-700"
              }`}
            >
              {status === "success" ? (
                <CheckCircle className="w-4 h-4 mt-0.5 shrink-0" />
              ) : (
                <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" />
              )}
              <span>{statusMsg}</span>
            </div>
          )}

          {/* Resend button */}
          <button
            onClick={handleResend}
            disabled={!email || countdown > 0 || isSending}
            className="w-full flex items-center justify-center gap-2 px-6 py-3.5 rounded-xl border-2 border-red-600 text-red-600 font-bold text-sm transition-all
              hover:bg-red-600 hover:text-white focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2
              disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent disabled:hover:text-red-600"
          >
            <RefreshCw
              className={`w-4 h-4 ${isSending ? "animate-spin" : ""}`}
            />
            {isSending
              ? "Đang gửi..."
              : countdown > 0
              ? `Gửi lại sau ${countdown}s`
              : "Gửi lại email xác thực"}
          </button>

          {/* Back to login */}
          <div className="text-center">
            <Link
              to="/login"
              className="inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-red-600 font-medium transition-colors"
            >
              <ArrowLeft className="w-4 h-4" />
              Quay lại trang đăng nhập
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
