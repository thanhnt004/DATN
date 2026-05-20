import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { Mail, ArrowRight, Store, Eye, EyeOff, AlertCircle } from "lucide-react";
import { useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import type { AppDispatch, RootState } from "../store/store";
import { clearError } from "../store/authSlice";
import { loginThunk } from "../store/authThunks";
import { buildGoogleOAuthRedirectUrl, buildFacebookOAuthRedirectUrl } from "../api/authApi";

const loginSchema = z.object({
  identifier: z.string().min(1, "Vui lòng nhập email hoặc số điện thoại"),
  password: z.string().min(6, "Mật khẩu phải chứa ít nhất 6 ký tự"),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const [showPassword, setShowPassword] = useState(false);
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();
  const { isLoading, error } = useSelector((state: RootState) => state.auth);

  // Clear any stale API error when leaving this page
  useEffect(() => () => { dispatch(clearError()); }, [dispatch]);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
  });

  const location = useLocation();
  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || "/";

  const onSubmit = async (data: LoginFormValues) => {
    dispatch(clearError());
    const result = await dispatch(loginThunk({ identifier: data.identifier, password: data.password }));
    if (loginThunk.fulfilled.match(result)) {
      navigate(from, { replace: true });
    } else if (loginThunk.rejected.match(result)) {
      const payload = result.payload as { code?: string } | undefined;
      if (payload?.code === "EMAIL_NOT_VERIFIED") {
        navigate(`/verify-email?email=${encodeURIComponent(data.identifier)}`);
      }
    }
  };

  return (
    <div className="md:h-screen bg-white flex flex-col md:flex-row overflow-hidden">
      {/* Left Section: Branding (Occupies left half on desktop) */}
      <div className="hidden md:flex w-1/2 bg-red-600 p-8 lg:p-16 flex-col justify-center items-center text-center text-white relative">
        {/* Decorative elements */}
        <div className="absolute top-0 right-0 w-[400px] h-[400px] bg-white/5 rounded-full -mr-48 -mt-48 blur-3xl"></div>
        <div className="absolute bottom-0 left-0 w-[500px] h-[500px] bg-black/5 rounded-full -ml-64 -mb-64 blur-3xl"></div>
        
        <div className="relative z-10 flex flex-col items-center space-y-6 lg:space-y-8">
          <div className="bg-white/10 p-8 lg:p-10 rounded-[40px] backdrop-blur-2xl border border-white/20 shadow-xl hover:scale-105 transition-transform duration-500">
            <Store className="w-24 h-24 lg:w-32 lg:h-32 text-white" />
          </div>
          <div className="space-y-4">
            <h1 className="text-6xl lg:text-7xl font-black tracking-tighter leading-none select-none">
              SELLICO
            </h1>
            <div className="w-20 h-2 bg-white rounded-full mx-auto opacity-80"></div>
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

      {/* Right Section: Login Form (Occupies screen on mobile, right half on desktop) */}
      <div className="w-full md:w-1/2 p-6 sm:p-10 lg:p-16 flex items-center justify-center bg-white overflow-y-auto h-screen md:h-full">
        <div className="w-full max-w-md space-y-6 lg:space-y-8 py-4">
          <div className="md:hidden flex justify-center mb-6">
            <Link to="/" className="flex items-center gap-2.5 text-red-600 font-black text-3xl tracking-tighter hover:opacity-80 transition-opacity">
              <Store className="w-8 h-8" />
              <span>SELLICO</span>
            </Link>
          </div>

          <div className="space-y-2 text-center md:text-left">
            <h2 className="text-3xl lg:text-4xl font-black text-slate-900 tracking-tight">Đăng nhập</h2>
            <p className="text-slate-600 font-medium text-sm lg:text-base">Chào mừng bạn quay lại với SELLICO!</p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 lg:space-y-5">
            {/* API Error Banner */}
            {error && (
              <div className="flex items-start gap-3 p-3 rounded-xl bg-red-50 border border-red-100">
                <AlertCircle className="w-4 h-4 text-red-600 mt-0.5 shrink-0" />
                <p className="text-red-700 text-xs font-semibold leading-snug">{error}</p>
              </div>
            )}
            <div className="space-y-1.5">
              <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-1">Email hoặc Số điện thoại</label>
              <div className="relative">
                <input
                  {...register("identifier")}
                  type="text"
                  placeholder="name@example.com"
                  className={`w-full pl-5 pr-12 py-3 lg:py-3.5 rounded-xl border-2 ${
                    errors.identifier ? "border-red-500 focus:ring-red-500" : "border-slate-50 focus:border-red-600"
                  } focus:outline-none transition-all placeholder:text-slate-400 font-semibold bg-slate-50`}
                />
                <Mail className="absolute right-5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
              </div>
              {errors.identifier && <p className="text-red-500 text-[10px] font-bold ml-1">{errors.identifier.message}</p>}
            </div>

            <div className="space-y-1.5">
              <div className="flex justify-between items-center px-1">
                <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest">Mật khẩu</label>
                <Link to="/forgot-password" title="Quên mật khẩu?" className="text-[10px] font-black text-red-600 hover:text-red-700 transition-colors uppercase tracking-widest">
                  Quên mật khẩu?
                </Link>
              </div>
              <div className="relative">
                <input
                  {...register("password")}
                  type={showPassword ? "text" : "password"}
                  placeholder="••••••••"
                  className={`w-full pl-5 pr-14 py-3 lg:py-3.5 rounded-xl border-2 ${
                    errors.password ? "border-red-500 focus:ring-red-500" : "border-slate-50 focus:border-red-600"
                  } focus:outline-none transition-all placeholder:text-slate-400 font-semibold bg-slate-50`}
                />
                <button 
                  type="button" 
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-red-600 transition-colors"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {errors.password && <p className="text-red-500 text-[10px] font-bold ml-1">{errors.password.message}</p>}
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full bg-red-600 text-white font-black py-3.5 lg:py-4 rounded-xl hover:bg-red-700 hover:shadow-lg hover:shadow-red-600/30 active:scale-[0.98] transition-all disabled:opacity-70 text-sm lg:text-base uppercase tracking-widest mt-2"
            >
              {isLoading ? "Đang xử lý..." : "Đăng nhập ngay"}
            </button>

            <div className="relative py-4 lg:py-6">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-slate-100"></div>
              </div>
              <div className="relative flex justify-center text-[10px]">
                <span className="bg-white px-4 text-slate-500 font-black uppercase tracking-[0.2em]">Hoặc tiếp tục với</span>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 lg:gap-4">
              <button
                type="button"
                onClick={() => { window.location.href = buildGoogleOAuthRedirectUrl(); }}
                className="flex items-center justify-center gap-2 px-4 py-3 border-2 border-slate-50 rounded-xl hover:bg-slate-50 hover:border-slate-100 transition-all font-black text-slate-700 shadow-sm text-[11px] uppercase tracking-wider"
              >
                <img src="https://www.google.com/favicon.ico" alt="Google" className="w-4 h-4" />
                <span>Google</span>
              </button>
              <button
                type="button"
                onClick={() => { window.location.href = buildFacebookOAuthRedirectUrl(); }}
                className="flex items-center justify-center gap-2 px-4 py-3 border-2 border-slate-50 rounded-xl hover:bg-slate-50 hover:border-slate-100 transition-all font-black text-slate-700 shadow-sm text-[11px] uppercase tracking-wider"
              >
                <svg className="w-5 h-5 text-[#1877F2]" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z" />
                </svg>
                <span>Facebook</span>
              </button>
            </div>

            <div className="text-center mt-8 lg:mt-10 p-4 lg:p-5 rounded-2xl border-2 border-slate-50 bg-slate-50/30">
              <p className="text-slate-500 font-bold text-[11px]">
                Bạn mới biết đến SELLICO?{" "}
                <Link to="/register" className="text-red-600 font-black hover:underline transition-all block sm:inline mt-1 sm:mt-0 uppercase tracking-widest">
                  Đăng ký tài khoản ngay <ArrowRight className="inline w-3 h-3 ml-1" />
                </Link>
              </p>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

