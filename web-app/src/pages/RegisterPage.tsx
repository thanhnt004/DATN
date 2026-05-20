import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { Link, useNavigate } from "react-router-dom";
import { Mail, ArrowRight, Store, Eye, EyeOff, User, Phone, AlertCircle } from "lucide-react";
import { useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import type { AppDispatch, RootState } from "../store/store";
import { clearError } from "../store/authSlice";
import { registerThunk } from "../store/authThunks";
import { buildGoogleOAuthRedirectUrl, buildFacebookOAuthRedirectUrl } from "../api/authApi";

const registerSchema = z.object({
  username: z.string().min(3, "Tên đăng nhập phải có ít nhất 3 ký tự").max(20, "Tên đăng nhập quá dài"),
  password: z.string().min(8, "Mật khẩu phải có ít nhất 8 ký tự")
    .regex(/[A-Z]/, "Mật khẩu phải chứa ít nhất một chữ hoa")
    .regex(/[0-9]/, "Mật khẩu phải chứa ít nhất một chữ số"),
  confirmPassword: z.string(),
  email: z.string().email("Vui lòng nhập địa chỉ email hợp lệ"),
  phone: z.string().regex(/^(0|84)(3|5|7|8|9)([0-9]{8})$/, "Số điện thoại không hợp lệ"),
}).refine((data) => data.password === data.confirmPassword, {
  message: "Mật khẩu xác nhận không khớp",
  path: ["confirmPassword"],
});

type RegisterFormValues = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();
  const { isLoading, error } = useSelector((state: RootState) => state.auth);

  // Clear any stale API error when leaving this page
  useEffect(() => () => { dispatch(clearError()); }, [dispatch]);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
  });

  const onSubmit = async (data: RegisterFormValues) => {
    dispatch(clearError());
    const result = await dispatch(
      registerThunk({
        username: data.username,
        password: data.password,
        email: data.email,
        phone: data.phone,
      })
    );
    if (registerThunk.fulfilled.match(result)) {
      navigate(`/verify-email?email=${encodeURIComponent(data.email)}&registered=true`);
    }
  };

  return (
    <div className="md:h-screen bg-white flex flex-col md:flex-row overflow-hidden">
      {/* Left Section: Branding (Same as Login for Consistency) */}
      <div className="hidden md:flex w-1/2 bg-red-600 p-8 lg:p-16 flex-col justify-center items-center text-center text-white relative">
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

        <div className="absolute bottom-8 flex flex-col items-center gap-3 text-center">
            <div className="text-red-100/60 text-[10px] font-black tracking-[0.3em] uppercase select-none">
                Quality Beyond Expectations
            </div>
        </div>
      </div>

      {/* Right Section: Registration Form */}
      <div className="w-full md:w-1/2 p-6 sm:p-10 lg:p-16 flex items-center justify-center bg-white overflow-y-auto h-screen md:h-full scrollbar-hidden">
        <div className="w-full max-w-md space-y-6 py-8">
          <div className="md:hidden flex justify-center mb-6">
            <Link to="/" className="flex items-center gap-2.5 text-red-600 font-black text-3xl tracking-tighter hover:opacity-80 transition-opacity">
              <Store className="w-8 h-8" />
              <span>SELLICO</span>
            </Link>
          </div>

          <div className="space-y-2 text-center md:text-left">
            <h2 className="text-3xl lg:text-4xl font-black text-slate-900 tracking-tight">Đăng ký</h2>
            <p className="text-slate-600 font-medium text-sm">Bắt đầu hành trình mua sắm tuyệt vời cùng chúng tôi!</p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            {/* API Error Banner */}
            {error && (
              <div className="flex items-start gap-3 p-3 rounded-xl bg-red-50 border border-red-100">
                <AlertCircle className="w-4 h-4 text-red-600 mt-0.5 shrink-0" />
                <p className="text-red-700 text-xs font-semibold leading-snug">{error}</p>
              </div>
            )}
            {/* Username Selection */}
            <div className="space-y-1.5">
              <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-1">Tên đăng nhập</label>
              <div className="relative">
                <input
                  {...register("username")}
                  type="text"
                  placeholder="john_doe"
                  className={`w-full pl-5 pr-12 py-3 rounded-xl border-2 ${
                    errors.username ? "border-red-500 focus:ring-red-500" : "border-slate-50 focus:border-red-600"
                  } focus:outline-none transition-all placeholder:text-slate-400 font-semibold bg-slate-50`}
                />
                <User className="absolute right-5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
              </div>
              {errors.username && <p className="text-red-500 text-[10px] font-bold ml-1">{errors.username.message}</p>}
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
               {/* Email Field */}
               <div className="space-y-1.5">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-1">Email</label>
                  <div className="relative">
                    <input
                      {...register("email")}
                      type="email"
                      placeholder="mail@example.com"
                      className={`w-full pl-5 pr-12 py-3 rounded-xl border-2 ${
                        errors.email ? "border-red-500 focus:ring-red-500" : "border-slate-50 focus:border-red-600"
                      } focus:outline-none transition-all placeholder:text-slate-400 font-semibold bg-slate-50`}
                    />
                    <Mail className="absolute right-5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                  </div>
                  {errors.email && <p className="text-red-500 text-[10px] font-bold ml-1">{errors.email.message}</p>}
                </div>

                {/* Phone Field */}
                <div className="space-y-1.5">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-1">Số điện thoại</label>
                  <div className="relative">
                    <input
                      {...register("phone")}
                      type="tel"
                      placeholder="0987654321"
                      className={`w-full pl-5 pr-12 py-3 rounded-xl border-2 ${
                        errors.phone ? "border-red-500 focus:ring-red-500" : "border-slate-50 focus:border-red-600"
                      } focus:outline-none transition-all placeholder:text-slate-400 font-semibold bg-slate-50`}
                    />
                    <Phone className="absolute right-5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                  </div>
                  {errors.phone && <p className="text-red-500 text-[10px] font-bold ml-1">{errors.phone.message}</p>}
                </div>
            </div>

            {/* Password Fields */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-1">Mật khẩu</label>
                  <div className="relative">
                    <input
                      {...register("password")}
                      type={showPassword ? "text" : "password"}
                      placeholder="••••••••"
                      className={`w-full pl-5 pr-12 py-3 rounded-xl border-2 ${
                        errors.password ? "border-red-500 focus:ring-red-500" : "border-slate-50 focus:border-red-600"
                      } focus:outline-none transition-all placeholder:text-slate-400 font-semibold bg-slate-50`}
                    />
                    <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-red-600">
                      {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                  {errors.password && <p className="text-red-500 text-[10px] font-bold ml-1 leading-tight">{errors.password.message}</p>}
                </div>

                <div className="space-y-1.5">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-1">Xác nhận</label>
                  <div className="relative">
                    <input
                      {...register("confirmPassword")}
                      type={showConfirmPassword ? "text" : "password"}
                      placeholder="••••••••"
                      className={`w-full pl-5 pr-12 py-3 rounded-xl border-2 ${
                        errors.confirmPassword ? "border-red-500 focus:ring-red-500" : "border-slate-50 focus:border-red-600"
                      } focus:outline-none transition-all placeholder:text-slate-400 font-semibold bg-slate-50`}
                    />
                    <button type="button" onClick={() => setShowConfirmPassword(!showConfirmPassword)} className="absolute right-5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-red-600">
                      {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                  {errors.confirmPassword && <p className="text-red-500 text-[10px] font-bold ml-1">{errors.confirmPassword.message}</p>}
                </div>
            </div>

            <div className="pt-2 text-[10px] text-slate-500 font-medium px-2 leading-relaxed">
              Bằng việc nhấn tiếp theo, bạn đồng ý với <span className="text-red-600 font-bold cursor-pointer hover:underline">Điều khoản dịch vụ</span> và <span className="text-red-600 font-bold cursor-pointer hover:underline">Chính sách bảo mật</span> của chúng tôi.
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full bg-red-600 text-white font-black py-4 rounded-xl hover:bg-red-700 hover:shadow-lg hover:shadow-red-600/30 active:scale-[0.98] transition-all disabled:opacity-70 text-sm uppercase tracking-widest mt-2 flex items-center justify-center gap-2"
            >
              {isLoading ? "Đang xử lý..." : "Tiếp theo"}
              {!isLoading && <ArrowRight className="w-4 h-4" />}
            </button>

            <div className="relative py-4">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-slate-100"></div>
              </div>
              <div className="relative flex justify-center text-[10px]">
                <span className="bg-white px-4 text-slate-500 font-black uppercase tracking-[0.2em]">Hoặc với</span>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => { window.location.href = buildGoogleOAuthRedirectUrl(); }}
                className="flex items-center justify-center gap-2 px-4 py-3 border-2 border-slate-50 rounded-xl hover:bg-slate-50 transition-all font-black text-slate-700 text-[11px] uppercase"
              >
                <img src="https://www.google.com/favicon.ico" alt="Google" className="w-4 h-4" />
                <span>Google</span>
              </button>
              <button
                type="button"
                onClick={() => { window.location.href = buildFacebookOAuthRedirectUrl(); }}
                className="flex items-center justify-center gap-2 px-4 py-3 border-2 border-slate-50 rounded-xl hover:bg-slate-50 transition-all font-black text-slate-700 text-[11px] uppercase"
              >
                <svg className="w-4 h-4 text-[#1877F2]" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z" />
                </svg>
                <span>Facebook</span>
              </button>
            </div>

            <div className="text-center mt-6 p-4 rounded-xl border-2 border-slate-50 bg-slate-50/50">
              <p className="text-slate-600 font-bold text-[11px]">
                Đã có tài khoản tại SELLICO?{" "}
                <Link to="/login" className="text-red-600 font-black hover:underline transition-all uppercase tracking-widest">
                  Đăng nhập tại đây
                </Link>
              </p>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
