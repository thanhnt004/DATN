import { useEffect, useState, useCallback } from "react";
import {
  Ticket, Plus, Loader2, ChevronLeft, ChevronRight, X, Pencil,
  Calendar, Percent, DollarSign, Copy, Check,
} from "lucide-react";
import {
  sellerGetCoupons,
  sellerCreateCoupon,
  sellerUpdateCoupon,
} from "../../api/couponApi";
import { getApiErrorMessage } from "../../utils/apiHelpers";
import type { CouponResponse, CreateCouponRequest, UpdateCouponRequest } from "../../types/coupon";

// ─── helpers ─────────────────────────────────────────────────────────────────
function fmtPrice(n: number | null | undefined) {
  if (n == null) return "—";
  return n.toLocaleString("vi-VN") + "₫";
}
function fmtDate(s: string) {
  return new Date(s).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
}

const STATUS_MAP: Record<string, { label: string; cls: string }> = {
  ACTIVE:   { label: "Đang hoạt động", cls: "bg-green-100 text-green-700" },
  INACTIVE: { label: "Tạm dừng",       cls: "bg-gray-100 text-gray-600" },
  EXPIRED:  { label: "Hết hạn",        cls: "bg-red-100 text-red-700" },
  DRAFT:    { label: "Bản nháp",       cls: "bg-yellow-100 text-yellow-700" },
};

// ─── Coupon form modal ─────────────────────────────────────────────────────
interface CouponFormProps {
  coupon?: CouponResponse | null;
  onClose: () => void;
  onSaved: () => void;
}

function CouponFormModal({ coupon, onClose, onSaved }: CouponFormProps) {
  const isEdit = !!coupon;
  const [code, setCode] = useState(coupon?.code ?? "");
  const [discountType, setDiscountType] = useState(coupon?.discountType ?? "PERCENTAGE");
  const [discountValue, setDiscountValue] = useState(coupon?.discountValue ?? 0);
  const [minOrderAmount, setMinOrderAmount] = useState(coupon?.minOrderAmount ?? 0);
  const [totalQuantity, setTotalQuantity] = useState(coupon?.totalQuantity ?? 100);
  const [maxUsagePerUser, setMaxUsagePerUser] = useState(coupon?.maxUsagePerUser ?? 1);
  const [startDate, setStartDate] = useState(coupon ? coupon.startDate.slice(0, 16) : "");
  const [endDate, setEndDate] = useState(coupon ? coupon.endDate.slice(0, 16) : "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!code.trim()) {
      setError("Vui lòng nhập mã coupon");
      return;
    }
    if (discountValue <= 0 || Number.isNaN(discountValue)) {
      setError("Giá trị giảm giá phải lớn hơn 0");
      return;
    }
    if (totalQuantity < 1 || Number.isNaN(totalQuantity)) {
      setError("Tổng số lượng phải lớn hơn hoặc bằng 1");
      return;
    }
    if (!startDate || !endDate) {
      setError("Vui lòng chọn thời gian bắt đầu và kết thúc");
      return;
    }
    if (new Date(startDate) >= new Date(endDate)) {
      setError("Ngày kết thúc phải lớn hơn ngày bắt đầu");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      if (isEdit) {
        const data: UpdateCouponRequest = {
          code: code.trim(),
          discountType,
          discountValue,
          minOrderAmount: minOrderAmount || undefined,
          totalQuantity,
          maxUsagePerUser,
          startDate: new Date(startDate).toISOString(),
          endDate: new Date(endDate).toISOString(),
        };
        await sellerUpdateCoupon(coupon!.id, data);
      } else {
        const data: CreateCouponRequest = {
          code: code.trim(),
          couponType: "SHOP",
          discountType,
          discountValue,
          minOrderAmount: minOrderAmount || undefined,
          totalQuantity,
          maxUsagePerUser,
          startDate: new Date(startDate).toISOString(),
          endDate: new Date(endDate).toISOString(),
        };
        await sellerCreateCoupon(data);
      }
      onSaved();
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const inputCls = "w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400";
  const labelCls = "block text-xs font-bold text-slate-500 uppercase tracking-widest mb-1";

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm overflow-auto">
      <form onSubmit={handleSubmit} className="bg-white rounded-2xl w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100 sticky top-0 bg-white rounded-t-2xl z-10">
          <h3 className="font-black text-slate-800">{isEdit ? "Sửa mã giảm giá" : "Tạo mã giảm giá"}</h3>
          <button type="button" onClick={onClose} className="p-1 rounded-lg hover:bg-slate-100"><X className="w-5 h-5 text-slate-400" /></button>
        </div>
        <div className="p-5 space-y-4">
          <div>
            <label className={labelCls}>Mã coupon *</label>
            <input className={inputCls} value={code} onChange={e => setCode(e.target.value.toUpperCase())} placeholder="VD: SUMMER2024" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className={labelCls}>Loại giảm giá</label>
              <select className={inputCls} value={discountType} onChange={e => setDiscountType(e.target.value)}>
                <option value="PERCENTAGE">Phần trăm (%)</option>
                <option value="FIXED_AMOUNT">Số tiền cố định (₫)</option>
              </select>
            </div>
            <div>
              <label className={labelCls}>Giá trị giảm *</label>
              <input type="number" className={inputCls} value={discountValue} onChange={e => { e.target.value = e.target.value.replace(/^0+(?=\d)/, ''); setDiscountValue(+e.target.value); }} min={0} />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className={labelCls}>Đơn tối thiểu</label>
              <input type="number" className={inputCls} value={minOrderAmount} onChange={e => { e.target.value = e.target.value.replace(/^0+(?=\d)/, ''); setMinOrderAmount(+e.target.value); }} min={0} />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className={labelCls}>Tổng số lượng *</label>
              <input type="number" className={inputCls} value={totalQuantity} onChange={e => { e.target.value = e.target.value.replace(/^0+(?=\d)/, ''); setTotalQuantity(+e.target.value); }} min={1} />
            </div>
            <div>
              <label className={labelCls}>Giới hạn / người</label>
              <input type="number" className={inputCls} value={maxUsagePerUser} onChange={e => { e.target.value = e.target.value.replace(/^0+(?=\d)/, ''); setMaxUsagePerUser(+e.target.value); }} min={1} />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className={labelCls}>Bắt đầu *</label>
              <input type="datetime-local" className={inputCls} value={startDate} onChange={e => setStartDate(e.target.value)} />
            </div>
            <div>
              <label className={labelCls}>Kết thúc *</label>
              <input type="datetime-local" className={inputCls} value={endDate} onChange={e => setEndDate(e.target.value)} />
            </div>
          </div>
          {error && <p className="text-xs text-red-500 font-semibold">{error}</p>}
        </div>
        <div className="px-5 pb-5 flex justify-end gap-2 sticky bottom-0 bg-white rounded-b-2xl">
          <button type="button" onClick={onClose} className="px-4 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50">Hủy</button>
          <button type="submit" disabled={saving}
            className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-60 flex items-center gap-2">
            {saving && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
            {isEdit ? "Cập nhật" : "Tạo mới"}
          </button>
        </div>
      </form>
    </div>
  );
}

// ─── Main page ──────────────────────────────────────────────────────────────
export default function SellerCouponsPage() {
  const [coupons, setCoupons] = useState<CouponResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>("");
  const [showForm, setShowForm] = useState(false);
  const [editCoupon, setEditCoupon] = useState<CouponResponse | null>(null);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await sellerGetCoupons({ status: statusFilter || undefined, page, size: 10 });
      const data = res.data.result as { content: CouponResponse[]; totalPages: number };
      setCoupons(data.content ?? []);
      setTotalPages(data.totalPages ?? 0);
    } catch { /* ignore */ }
    setLoading(false);
  }, [page, statusFilter]);

  useEffect(() => { load(); }, [load]);

  const handleCopy = (code: string, id: string) => {
    navigator.clipboard.writeText(code);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 1500);
  };

  const handleSaved = () => {
    setShowForm(false);
    setEditCoupon(null);
    load();
  };

  return (
    <div className="p-4 md:p-6 space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-red-100 rounded-xl flex items-center justify-center">
            <Ticket className="w-5 h-5 text-red-600" />
          </div>
          <div>
            <h1 className="text-xl font-black text-slate-800">Mã giảm giá</h1>
            <p className="text-xs text-slate-400">Quản lý coupon của shop</p>
          </div>
        </div>
        <button onClick={() => { setEditCoupon(null); setShowForm(true); }}
          className="flex items-center gap-2 px-4 py-2 rounded-xl bg-red-600 text-white text-sm font-bold hover:bg-red-700">
          <Plus className="w-4 h-4" />Tạo coupon
        </button>
      </div>

      {/* Filter tabs */}
      <div className="flex gap-2 overflow-x-auto">
        {[
          { value: "", label: "Tất cả" },
          { value: "ACTIVE", label: "Hoạt động" },
          { value: "INACTIVE", label: "Tạm dừng" },
          { value: "EXPIRED", label: "Hết hạn" },
          { value: "DRAFT", label: "Nháp" },
        ].map(f => (
          <button key={f.value} onClick={() => { setStatusFilter(f.value); setPage(0); }}
            className={`px-4 py-2 rounded-xl text-sm font-bold whitespace-nowrap transition-colors ${statusFilter === f.value ? "bg-red-600 text-white" : "bg-white text-slate-600 border border-slate-200 hover:bg-slate-50"}`}
          >{f.label}</button>
        ))}
      </div>

      {/* Coupons list */}
      {loading ? (
        <div className="flex justify-center py-16"><Loader2 className="w-8 h-8 animate-spin text-slate-300" /></div>
      ) : coupons.length === 0 ? (
        <div className="text-center py-16">
          <Ticket className="w-12 h-12 text-slate-200 mx-auto mb-3" />
          <p className="font-bold text-slate-500">Chưa có mã giảm giá nào</p>
          <p className="text-sm text-slate-400 mt-1">Tạo coupon để thu hút khách hàng</p>
        </div>
      ) : (
        <div className="space-y-3">
          {coupons.map(c => {
            const sCfg = STATUS_MAP[c.status] ?? { label: c.status, cls: "bg-gray-100 text-gray-600" };
            return (
              <div key={c.id} className="bg-white rounded-xl border border-slate-100 p-4 hover:shadow-md transition-shadow">
                <div className="flex items-start justify-between gap-3 flex-wrap">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 bg-red-50 rounded-xl flex items-center justify-center shrink-0">
                      {c.discountType === "PERCENTAGE" ? <Percent className="w-5 h-5 text-red-600" /> : <DollarSign className="w-5 h-5 text-red-600" />}
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-black text-slate-800 text-sm tracking-wider">{c.code}</span>
                        <button onClick={() => handleCopy(c.code, c.id)} className="p-1 rounded-md hover:bg-slate-100">
                          {copiedId === c.id ? <Check className="w-3.5 h-3.5 text-green-500" /> : <Copy className="w-3.5 h-3.5 text-slate-400" />}
                        </button>
                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${sCfg.cls}`}>{sCfg.label}</span>
                      </div>
                      <p className="text-xs text-slate-500 mt-0.5">
                        Giảm {c.discountType === "PERCENTAGE" ? `${c.discountValue}%` : fmtPrice(c.discountValue)}
                        {c.maxDiscountAmount ? ` (tối đa ${fmtPrice(c.maxDiscountAmount)})` : ""}
                        {c.minOrderAmount ? ` · Đơn tối thiểu ${fmtPrice(c.minOrderAmount)}` : ""}
                      </p>
                    </div>
                  </div>
                  <button onClick={() => { setEditCoupon(c); setShowForm(true); }}
                    className="p-2 rounded-xl hover:bg-slate-100 text-slate-400 hover:text-red-600">
                    <Pencil className="w-4 h-4" />
                  </button>
                </div>
                <div className="flex items-center gap-4 mt-3 text-xs text-slate-400">
                  <span className="flex items-center gap-1"><Calendar className="w-3.5 h-3.5" />{fmtDate(c.startDate)} — {fmtDate(c.endDate)}</span>
                  <span>Đã dùng: {c.usedQuantity}/{c.totalQuantity}</span>
                  <span>Còn lại: {c.remainingQuantity}</span>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex justify-center gap-2">
          <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
            className="p-2 rounded-xl border border-slate-200 hover:bg-slate-50 disabled:opacity-40"><ChevronLeft className="w-4 h-4" /></button>
          <span className="px-4 py-2 text-sm font-bold text-slate-600">{page + 1} / {totalPages}</span>
          <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
            className="p-2 rounded-xl border border-slate-200 hover:bg-slate-50 disabled:opacity-40"><ChevronRight className="w-4 h-4" /></button>
        </div>
      )}

      {/* Form Modal */}
      {showForm && <CouponFormModal coupon={editCoupon} onClose={() => { setShowForm(false); setEditCoupon(null); }} onSaved={handleSaved} />}
    </div>
  );
}
