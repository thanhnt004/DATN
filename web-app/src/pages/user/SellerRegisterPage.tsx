import { useState, useEffect } from "react";
import {
  Store,
  Clock,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Loader2,
  ChevronRight,
  RefreshCw,
  User,
  Building2,
  Mail,
  MapPin,
  FileText,
  Info,
} from "lucide-react";
import {
  getMySellerStatus,
  registerSeller,
  resubmitSellerRegistration,
} from "../../api/sellerApi";
import type { SellerResponse, RegisterSellerRequest, SellerType } from "../../types/seller";

// ─── helpers ──────────────────────────────────────────────────────────────────

function formatDate(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

// ─── sub-components ──────────────────────────────────────────────────────────

function SectionHeader({ icon: Icon, title }: { icon: React.FC<React.SVGProps<SVGSVGElement>>; title: string }) {
  return (
    <div className="flex items-center gap-3 px-6 py-5 border-b border-gray-50">
      <div className="w-9 h-9 bg-red-100 rounded-xl flex items-center justify-center">
        <Icon className="text-red-600" style={{ width: 18, height: 18 }} />
      </div>
      <h1 className="font-black text-gray-900 text-lg">{title}</h1>
    </div>
  );
}

interface InputFieldProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  required?: boolean;
}

function InputField({ label, error, required, ...props }: InputFieldProps) {
  return (
    <div className="space-y-1.5">
      <label className="flex gap-1 text-xs font-bold text-gray-500 uppercase tracking-widest">
        {label}
        {required && <span className="text-red-500">*</span>}
      </label>
      <input
        {...props}
        className={`w-full px-4 py-2.5 rounded-xl border-2 text-sm font-semibold transition-all outline-none
          ${error
            ? "border-red-400 bg-red-50 text-red-700"
            : "border-gray-100 bg-gray-50 focus:border-red-400 text-gray-800"
          }
          disabled:opacity-50 disabled:cursor-not-allowed placeholder:font-normal placeholder:text-gray-400`}
      />
      {error && <p className="text-xs font-semibold text-red-500">{error}</p>}
    </div>
  );
}

interface TextareaFieldProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label: string;
  error?: string;
}

function TextareaField({ label, error, ...props }: TextareaFieldProps) {
  return (
    <div className="space-y-1.5">
      <label className="text-xs font-bold text-gray-500 uppercase tracking-widest">{label}</label>
      <textarea
        {...props}
        rows={3}
        className={`w-full px-4 py-2.5 rounded-xl border-2 text-sm font-semibold transition-all outline-none resize-none
          ${error
            ? "border-red-400 bg-red-50 text-red-700"
            : "border-gray-100 bg-gray-50 focus:border-red-400 text-gray-800"
          }
          placeholder:font-normal placeholder:text-gray-400`}
      />
      {error && <p className="text-xs font-semibold text-red-500">{error}</p>}
    </div>
  );
}

// ─── Status display ────────────────────────────────────────────────────────────

interface StatusBadgeProps {
  status: SellerResponse["status"];
}

function StatusBadge({ status }: StatusBadgeProps) {
  const configs: Record<SellerResponse["status"], { label: string; color: string }> = {
    PENDING:   { label: "Đang chờ duyệt",     color: "bg-yellow-100 text-yellow-700" },
    ACTIVE:    { label: "Đang hoạt động",      color: "bg-green-100 text-green-700" },
    REJECTED:  { label: "Đã bị từ chối",       color: "bg-red-100 text-red-700" },
    SUSPENDED: { label: "Tạm ngưng hoạt động", color: "bg-orange-100 text-orange-700" },
    BANNED:    { label: "Bị cấm",              color: "bg-gray-100 text-gray-600" },
    CLOSED:    { label: "Đã đóng cửa hàng",    color: "bg-gray-100 text-gray-600" },
  };
  const { label, color } = configs[status];
  return (
    <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-bold ${color}`}>
      {label}
    </span>
  );
}

interface StatusPageProps {
  seller: SellerResponse;
  onResubmit: () => void;
}

function StatusPage({ seller, onResubmit }: StatusPageProps) {
  const statusIcons: Record<SellerResponse["status"], React.ReactNode> = {
    PENDING:   <Clock className="w-10 h-10 text-yellow-500" />,
    ACTIVE:    <CheckCircle2 className="w-10 h-10 text-green-500" />,
    REJECTED:  <XCircle className="w-10 h-10 text-red-500" />,
    SUSPENDED: <AlertTriangle className="w-10 h-10 text-orange-500" />,
    BANNED:    <XCircle className="w-10 h-10 text-gray-400" />,
    CLOSED:    <XCircle className="w-10 h-10 text-gray-400" />,
  };

  const statusMessages: Record<SellerResponse["status"], { title: string; desc: string }> = {
    PENDING: {
      title: "Đơn đăng ký đang được xem xét",
      desc: "Chúng tôi sẽ xem xét thông tin và phản hồi trong vòng 1-3 ngày làm việc. Vui lòng kiểm tra email để nhận thông báo.",
    },
    ACTIVE: {
      title: "Cửa hàng của bạn đang hoạt động!",
      desc: "Tài khoản bán hàng đã được kích hoạt. Bạn có thể đăng sản phẩm và bắt đầu bán hàng ngay.",
    },
    REJECTED: {
      title: "Đơn đăng ký bị từ chối",
      desc: "Thông tin đăng ký chưa đáp ứng yêu cầu. Vui lòng xem lý do từ chối và nộp lại đơn với thông tin đầy đủ hơn.",
    },
    SUSPENDED: {
      title: "Cửa hàng đang bị tạm ngưng",
      desc: "Tài khoản bán hàng của bạn đã bị tạm ngưng. Vui lòng liên hệ hỗ trợ để biết thêm chi tiết.",
    },
    BANNED: {
      title: "Tài khoản bán hàng bị cấm",
      desc: "Tài khoản bán hàng của bạn đã bị cấm vĩnh viễn. Vui lòng liên hệ hỗ trợ nếu bạn cho rằng đây là sự nhầm lẫn.",
    },
    CLOSED: {
      title: "Cửa hàng đã đóng",
      desc: "Tài khoản bán hàng đã được đóng.",
    },
  };

  const { title, desc } = statusMessages[seller.status];

  return (
    <div className="space-y-4">
      {/* Status card */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
        <SectionHeader icon={Store} title="Đăng ký cửa hàng" />
        <div className="px-6 py-10 flex flex-col items-center text-center gap-4">
          <div className="w-20 h-20 rounded-2xl bg-gray-50 flex items-center justify-center">
            {statusIcons[seller.status]}
          </div>
          <div className="space-y-2">
            <h2 className="text-xl font-black text-gray-900">{title}</h2>
            <StatusBadge status={seller.status} />
          </div>
          <p className="text-sm text-gray-500 max-w-md leading-relaxed">{desc}</p>

          {seller.status === "REJECTED" && (
            <button
              onClick={onResubmit}
              className="flex items-center gap-2 px-5 py-2.5 bg-red-600 hover:bg-red-700 text-white rounded-xl text-sm font-bold transition-colors mt-2"
            >
              <RefreshCw className="w-4 h-4" />
              Đăng ký lại
            </button>
          )}
          {seller.status === "ACTIVE" && (
            <a
              href="/seller"
              className="flex items-center gap-2 px-5 py-2.5 bg-red-600 hover:bg-red-700 text-white rounded-xl text-sm font-bold transition-colors mt-2"
            >
              Quản lý cửa hàng
              <ChevronRight className="w-4 h-4" />
            </a>
          )}
        </div>
      </div>

      {/* Detail info card */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-50">
          <h2 className="font-black text-gray-800 text-sm">Thông tin đăng ký</h2>
        </div>
        <div className="px-6 py-4 grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-0 divide-y divide-gray-50 sm:divide-y-0">

          {[
            { label: "Tên cửa hàng",   value: seller.shopName },
            { label: "Email liên hệ",   value: seller.email },
            { label: "Số điện thoại",   value: seller.phone },
            { label: "Địa chỉ",         value: [seller.address, seller.ward, seller.district, seller.city].filter(Boolean).join(", ") || "—" },
            { label: "Loại tài khoản",  value: seller.sellerType === "BUSINESS" ? "Doanh nghiệp" : "Cá nhân" },
            { label: "Ngày nộp đơn",    value: formatDate(seller.createdAt) },
            ...(seller.approvedAt ? [{ label: "Ngày duyệt", value: formatDate(seller.approvedAt) }] : []),
            ...(seller.rejectedAt ? [{ label: "Ngày từ chối", value: formatDate(seller.rejectedAt) }] : []),
          ].map(({ label, value }) => (
            <div key={label} className="flex items-start gap-2 py-3 border-b border-gray-50 last:border-0 sm:border-b">
              <span className="w-36 shrink-0 text-sm text-gray-400">{label}</span>
              <span className="text-sm font-semibold text-gray-800">{value}</span>
            </div>
          ))}
        </div>

        {/* Rejection reason */}
        {seller.status === "REJECTED" && seller.rejectionReason && (
          <div className="mx-6 mb-5 p-4 bg-red-50 border border-red-100 rounded-xl flex gap-3">
            <Info className="w-4 h-4 text-red-500 shrink-0 mt-0.5" />
            <div>
              <p className="text-xs font-bold text-red-600 mb-1">Lý do từ chối</p>
              <p className="text-sm text-red-700">{seller.rejectionReason}</p>
            </div>
          </div>
        )}
        {seller.status === "SUSPENDED" && seller.statusNote && (
          <div className="mx-6 mb-5 p-4 bg-orange-50 border border-orange-100 rounded-xl flex gap-3">
            <Info className="w-4 h-4 text-orange-500 shrink-0 mt-0.5" />
            <div>
              <p className="text-xs font-bold text-orange-600 mb-1">Lý do tạm ngưng</p>
              <p className="text-sm text-orange-700">{seller.statusNote}</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Registration form ─────────────────────────────────────────────────────────

interface FormErrors {
  shopName?: string;
  email?: string;
  phone?: string;
  address?: string;
  businessName?: string;
  taxCode?: string;
}

interface RegistrationFormProps {
  isResubmit?: boolean;
  prefill?: SellerResponse | null;
  onSuccess: (seller: SellerResponse) => void;
}

function RegistrationForm({ isResubmit = false, prefill, onSuccess }: RegistrationFormProps) {
  const [sellerType, setSellerType] = useState<SellerType>(prefill?.sellerType ?? "INDIVIDUAL");
  const [form, setForm] = useState<RegisterSellerRequest>({
    shopName:   prefill?.shopName   ?? "",
    description: prefill?.description ?? "",
    email:      prefill?.email      ?? "",
    phone:      prefill?.phone      ?? "",
    address:    prefill?.address    ?? "",
    ward:       prefill?.ward       ?? "",
    district:   prefill?.district   ?? "",
    city:       prefill?.city       ?? "",
    sellerType: prefill?.sellerType ?? "INDIVIDUAL",
    businessName: prefill?.businessName ?? "",
    taxCode:    prefill?.taxCode    ?? "",
    businessLicenseNumber: "",
  });
  const [errors, setErrors] = useState<FormErrors>({});
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const set = (key: keyof RegisterSellerRequest) => (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    setForm((f) => ({ ...f, [key]: e.target.value }));
    setErrors((err) => ({ ...err, [key]: undefined }));
  };

  const validate = (): boolean => {
    const e: FormErrors = {};
    if (!form.shopName.trim()) e.shopName = "Vui lòng nhập tên cửa hàng";
    else if (form.shopName.trim().length < 3) e.shopName = "Tên cửa hàng tối thiểu 3 ký tự";
    if (!form.email.trim()) e.email = "Vui lòng nhập email";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) e.email = "Email không hợp lệ";
    if (!form.phone.trim()) e.phone = "Vui lòng nhập số điện thoại";
    if (!form.address.trim()) e.address = "Vui lòng nhập địa chỉ";
    if (sellerType === "BUSINESS") {
      if (!form.businessName?.trim()) e.businessName = "Vui lòng nhập tên doanh nghiệp";
      if (!form.taxCode?.trim()) e.taxCode = "Vui lòng nhập mã số thuế";
    }
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;
    setSubmitting(true);
    setServerError(null);
    try {
      const payload: RegisterSellerRequest = { ...form, sellerType };
      let result: SellerResponse;
      if (isResubmit) {
        result = await resubmitSellerRegistration(payload);
      } else {
        result = await registerSeller(payload);
      }
      onSuccess(result);
    } catch (err: unknown) {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const msg = (err as any)?.response?.data?.message ?? "Đã xảy ra lỗi. Vui lòng thử lại.";
      setServerError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
      <SectionHeader icon={Store} title={isResubmit ? "Đăng ký lại cửa hàng" : "Đăng ký cửa hàng"} />

      {/* Info banner */}
      <div className="mx-6 mt-5 p-4 bg-blue-50 border border-blue-100 rounded-xl flex gap-3">
        <Info className="w-4 h-4 text-blue-500 shrink-0 mt-0.5" />
        <p className="text-sm text-blue-700 leading-relaxed">
          Sau khi nộp đơn, đội ngũ Sellico sẽ xem xét và phản hồi qua email trong vòng <strong>1-3 ngày làm việc</strong>.
          {isResubmit && " Đơn đăng ký trước của bạn đã bị từ chối — vui lòng cập nhật đầy đủ thông tin."}
        </p>
      </div>

      <form onSubmit={handleSubmit} className="px-6 py-6 space-y-6">
        {/* Seller type selector */}
        <div className="space-y-2">
          <label className="text-xs font-bold text-gray-500 uppercase tracking-widest">
            Loại tài khoản bán hàng <span className="text-red-500">*</span>
          </label>
          <div className="grid grid-cols-2 gap-3">
            {(["INDIVIDUAL", "BUSINESS"] as SellerType[]).map((type) => (
              <button
                key={type}
                type="button"
                onClick={() => setSellerType(type)}
                className={`flex items-center gap-3 px-4 py-3.5 rounded-xl border-2 text-left transition-all ${
                  sellerType === type
                    ? "border-red-400 bg-red-50"
                    : "border-gray-100 bg-gray-50 hover:border-gray-200"
                }`}
              >
                {type === "INDIVIDUAL" ? (
                  <User className={`w-5 h-5 ${sellerType === type ? "text-red-600" : "text-gray-400"}`} />
                ) : (
                  <Building2 className={`w-5 h-5 ${sellerType === type ? "text-red-600" : "text-gray-400"}`} />
                )}
                <div>
                  <p className={`text-sm font-bold ${sellerType === type ? "text-red-700" : "text-gray-700"}`}>
                    {type === "INDIVIDUAL" ? "Cá nhân" : "Doanh nghiệp"}
                  </p>
                  <p className="text-[11px] text-gray-400 leading-tight">
                    {type === "INDIVIDUAL" ? "Hộ kinh doanh cá thể" : "Công ty / Doanh nghiệp"}
                  </p>
                </div>
              </button>
            ))}
          </div>
        </div>

        {/* Shop info */}
        <div className="space-y-4">
          <h3 className="flex items-center gap-2 text-sm font-black text-gray-700">
            <Store className="w-4 h-4 text-red-500" />
            Thông tin cửa hàng
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="sm:col-span-2">
              <InputField
                label="Tên cửa hàng"
                required
                placeholder="VD: Thời trang ABC"
                value={form.shopName}
                onChange={set("shopName")}
                error={errors.shopName}
              />
            </div>
            <div className="sm:col-span-2">
              <TextareaField
                label="Mô tả cửa hàng"
                placeholder="Giới thiệu ngắn về cửa hàng của bạn..."
                value={form.description ?? ""}
                onChange={set("description")}
              />
            </div>
          </div>
        </div>

        {/* Contact info */}
        <div className="space-y-4">
          <h3 className="flex items-center gap-2 text-sm font-black text-gray-700">
            <Mail className="w-4 h-4 text-red-500" />
            Thông tin liên hệ
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <InputField
              label="Email liên hệ"
              required
              type="email"
              placeholder="owner@example.com"
              value={form.email}
              onChange={set("email")}
              error={errors.email}
            />
            <InputField
              label="Số điện thoại"
              required
              type="tel"
              placeholder="0xxxxxxxxx"
              value={form.phone}
              onChange={set("phone")}
              error={errors.phone}
            />
          </div>
        </div>

        {/* Address */}
        <div className="space-y-4">
          <h3 className="flex items-center gap-2 text-sm font-black text-gray-700">
            <MapPin className="w-4 h-4 text-red-500" />
            Địa chỉ kho / cửa hàng
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="sm:col-span-2">
              <InputField
                label="Địa chỉ cụ thể"
                required
                placeholder="Số nhà, tên đường..."
                value={form.address}
                onChange={set("address")}
                error={errors.address}
              />
            </div>
            <InputField
              label="Phường / Xã"
              placeholder="Phường Bến Nghé"
              value={form.ward ?? ""}
              onChange={set("ward")}
            />
            <InputField
              label="Quận / Huyện"
              placeholder="Quận 1"
              value={form.district ?? ""}
              onChange={set("district")}
            />
            <div className="sm:col-span-2">
              <InputField
                label="Tỉnh / Thành phố"
                placeholder="TP. Hồ Chí Minh"
                value={form.city ?? ""}
                onChange={set("city")}
              />
            </div>
          </div>
        </div>

        {/* Business info (only for BUSINESS type) */}
        {sellerType === "BUSINESS" && (
          <div className="space-y-4">
            <h3 className="flex items-center gap-2 text-sm font-black text-gray-700">
              <FileText className="w-4 h-4 text-red-500" />
              Thông tin doanh nghiệp
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="sm:col-span-2">
                <InputField
                  label="Tên doanh nghiệp"
                  required
                  placeholder="Công ty TNHH ABC"
                  value={form.businessName ?? ""}
                  onChange={set("businessName")}
                  error={errors.businessName}
                />
              </div>
              <InputField
                label="Mã số thuế"
                required
                placeholder="0123456789"
                value={form.taxCode ?? ""}
                onChange={set("taxCode")}
                error={errors.taxCode}
              />
              <InputField
                label="Số đăng ký kinh doanh"
                placeholder="(nếu có)"
                value={form.businessLicenseNumber ?? ""}
                onChange={set("businessLicenseNumber")}
              />
            </div>
          </div>
        )}

        {/* Server error */}
        {serverError && (
          <div className="p-4 bg-red-50 border border-red-200 rounded-xl flex items-start gap-3">
            <XCircle className="w-4 h-4 text-red-500 shrink-0 mt-0.5" />
            <p className="text-sm text-red-700 font-semibold">{serverError}</p>
          </div>
        )}

        {/* Submit */}
        <div className="flex justify-end pt-2">
          <button
            type="submit"
            disabled={submitting}
            className="flex items-center gap-2 px-6 py-2.5 bg-red-600 hover:bg-red-700 disabled:bg-red-400 text-white rounded-xl text-sm font-bold transition-colors"
          >
            {submitting ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Đang gửi...
              </>
            ) : (
              <>
                {isResubmit ? <RefreshCw className="w-4 h-4" /> : <Store className="w-4 h-4" />}
                {isResubmit ? "Nộp lại đơn" : "Gửi đơn đăng ký"}
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
}

// ─── Main page ─────────────────────────────────────────────────────────────────

type PageView = "loading" | "form" | "status" | "resubmit";

export default function SellerRegisterPage() {
  const [view, setView] = useState<PageView>("loading");
  const [seller, setSeller] = useState<SellerResponse | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const result = await getMySellerStatus();
        if (cancelled) return;
        if (result === null) {
          // No application yet → show form
          setView("form");
        } else {
          setSeller(result);
          setView("status");
        }
      } catch {
        if (!cancelled) setLoadError("Không thể tải dữ liệu. Vui lòng thử lại.");
      }
    })();
    return () => { cancelled = true; };
  }, []);

  const handleFormSuccess = (result: SellerResponse) => {
    setSeller(result);
    setView("status");
  };

  if (view === "loading") {
    return (
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm">
        <SectionHeader icon={Store} title="Đăng ký cửa hàng" />
        <div className="flex items-center justify-center py-24 gap-3 text-gray-400">
          {loadError ? (
            <div className="text-center space-y-3">
              <p className="text-sm font-semibold text-red-500">{loadError}</p>
              <button
                onClick={() => { setLoadError(null); setView("loading"); }}
                className="text-xs font-bold text-red-600 hover:underline"
              >
                Thử lại
              </button>
            </div>
          ) : (
            <>
              <Loader2 className="w-5 h-5 animate-spin text-red-500" />
              <span className="text-sm font-semibold">Đang tải...</span>
            </>
          )}
        </div>
      </div>
    );
  }

  if (view === "form") {
    return <RegistrationForm onSuccess={handleFormSuccess} />;
  }

  if (view === "resubmit" && seller) {
    return (
      <RegistrationForm
        isResubmit
        prefill={seller}
        onSuccess={handleFormSuccess}
      />
    );
  }

  if (view === "status" && seller) {
    return (
      <StatusPage
        seller={seller}
        onResubmit={() => setView("resubmit")}
      />
    );
  }

  return null;
}
