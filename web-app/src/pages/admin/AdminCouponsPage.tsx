import { useEffect, useState, useCallback } from "react";
import {
  Ticket, Loader2, ChevronLeft, ChevronRight, X, Pencil, Plus,
  Calendar, Percent, DollarSign, Copy, Check, Search,
} from "lucide-react";
import {
  adminGetCoupons,
  adminCreateCoupon,
  adminUpdateCoupon,
} from "../../api/couponApi";
import { getApiErrorMessage } from "../../utils/apiHelpers";
import type { CouponResponse, CreateCouponRequest, UpdateCouponRequest } from "../../types/coupon";

// helpers
function fmtPrice(n: number | null | undefined) {
  if (n == null) return "—";
  return n.toLocaleString("vi-VN") + "₫";
}
function fmtDate(s: string) {
  return new Date(s).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
}

const STATUS_MAP: Record<string, { label: string; cls: string }> = {
  ACTIVE:   { label: "Hoạt động", cls: "bg-green-100 text-green-700" },
  INACTIVE: { label: "Tạm dừng",  cls: "bg-gray-100 text-gray-600" },
  EXPIRED:  { label: "Hết hạn",   cls: "bg-red-100 text-red-700" },
  DRAFT:    { label: "Nháp",      cls: "bg-yellow-100 text-yellow-700" },
};

// ─── Form modal ──────────────────────────────────────────────────────────────
function CouponFormModal({ coupon, onClose, onSaved }: { coupon?: CouponResponse | null; onClose: () => void; onSaved: () => void }) {
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
        await adminUpdateCoupon(coupon!.id, data);
      } else {
        const data: CreateCouponRequest = {
          code: code.trim(),
          couponType: "PLATFORM",
          discountType,
          discountValue,
          minOrderAmount: minOrderAmount || undefined,
          totalQuantity,
          maxUsagePerUser,
          startDate: new Date(startDate).toISOString(),
          endDate: new Date(endDate).toISOString(),
        };
        await adminCreateCoupon(data);
      }
      onSaved();
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const cls = "w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400";
  const lbl = "block text-xs font-bold text-slate-500 uppercase tracking-widest mb-1";

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm overflow-auto">
      <form onSubmit={handleSubmit} className="bg-white rounded-2xl w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100 sticky top-0 bg-white rounded-t-2xl z-10">
          <h3 className="font-black text-slate-800">{isEdit ? "Sửa coupon" : "Tạo coupon"}</h3>
          <button type="button" onClick={onClose} className="p-1 rounded-lg hover:bg-slate-100"><X className="w-5 h-5 text-slate-400" /></button>
        </div>
        <div className="p-5 space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className={lbl}>Mã *</label>
              <input className={cls} value={code} onChange={e => setCode(e.target.value.toUpperCase())} placeholder="VD: PLATFORM50K" />
            </div>
            <div>
              <label className={lbl}>Loại giảm</label>
              <select className={cls} value={discountType} onChange={e => setDiscountType(e.target.value)}>
                <option value="PERCENTAGE">Phần trăm (%)</option>
                <option value="FIXED_AMOUNT">Cố định (₫)</option>
              </select>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className={lbl}>Giá trị *</label>
              <input type="number" className={cls} value={discountValue} onChange={e => { e.target.value = e.target.value.replace(/^0+(?=\d)/, ''); setDiscountValue(+e.target.value); }} min={0} />
            </div>
            <div><label className={lbl}>Đơn tối thiểu</label><input type="number" className={cls} value={minOrderAmount} onChange={e => { e.target.value = e.target.value.replace(/^0+(?=\d)/, ''); setMinOrderAmount(+e.target.value); }} min={0} /></div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div><label className={lbl}>Số lượng *</label><input type="number" className={cls} value={totalQuantity} onChange={e => { e.target.value = e.target.value.replace(/^0+(?=\d)/, ''); setTotalQuantity(+e.target.value); }} min={1} /></div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div><label className={lbl}>Giới hạn / người</label><input type="number" className={cls} value={maxUsagePerUser} onChange={e => { e.target.value = e.target.value.replace(/^0+(?=\d)/, ''); setMaxUsagePerUser(+e.target.value); }} min={1} /></div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div><label className={lbl}>Bắt đầu *</label><input type="datetime-local" className={cls} value={startDate} onChange={e => setStartDate(e.target.value)} /></div>
            <div><label className={lbl}>Kết thúc *</label><input type="datetime-local" className={cls} value={endDate} onChange={e => setEndDate(e.target.value)} /></div>
          </div>
          {error && <p className="text-xs text-red-500 font-semibold">{error}</p>}
        </div>
        <div className="px-5 pb-5 flex justify-end gap-2 sticky bottom-0 bg-white rounded-b-2xl">
          <button type="button" onClick={onClose} className="px-4 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50">Hủy</button>
          <button type="submit" disabled={saving} className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-60 flex items-center gap-2">
            {saving && <Loader2 className="w-3.5 h-3.5 animate-spin" />}{isEdit ? "Cập nhật" : "Tạo"}
          </button>
        </div>
      </form>
    </div>
  );
}

// ─── Main page ──────────────────────────────────────────────────────────────
export default function AdminCouponsPage() {
  const [coupons, setCoupons] = useState<CouponResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState("");
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [editCoupon, setEditCoupon] = useState<CouponResponse | null>(null);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await adminGetCoupons({ status: statusFilter || undefined, page, size: 12 });
      const data = res.data.result as { content: CouponResponse[]; totalPages: number };
      setCoupons(data.content ?? []);
      setTotalPages(data.totalPages ?? 0);
    } catch { /* */ }
    setLoading(false);
  }, [page, statusFilter]);

  useEffect(() => { load(); }, [load]);

  const filtered = search ? coupons.filter(c => c.code.toLowerCase().includes(search.toLowerCase())) : coupons;

  const handleCopy = (code: string, id: string) => {
    navigator.clipboard.writeText(code);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 1500);
  };

  return (
    <div className="p-4 md:p-6 space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-red-100 rounded-xl flex items-center justify-center">
            <Ticket className="w-5 h-5 text-red-600" />
          </div>
          <div>
            <h1 className="text-xl font-black text-slate-800">Quản lý Coupon</h1>
            <p className="text-xs text-slate-400">Quản lý mã giảm giá toàn sàn</p>
          </div>
        </div>
        <button onClick={() => { setEditCoupon(null); setShowForm(true); }}
          className="flex items-center gap-2 px-4 py-2 rounded-xl bg-red-600 text-white text-sm font-bold hover:bg-red-700">
          <Plus className="w-4 h-4" />Tạo coupon
        </button>
      </div>

      <div className="flex flex-wrap gap-3 items-center">
        <div className="relative flex-1 max-w-xs">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input className="w-full pl-10 pr-3 py-2 rounded-xl border border-slate-200 bg-white text-sm outline-none focus:border-red-400"
            placeholder="Tìm theo mã..." value={search} onChange={e => setSearch(e.target.value)} />
        </div>
        {["", "ACTIVE", "INACTIVE", "EXPIRED", "DRAFT"].map(v => (
          <button key={v} onClick={() => { setStatusFilter(v); setPage(0); }}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold ${statusFilter === v ? "bg-red-600 text-white" : "bg-white border border-slate-200 text-slate-600 hover:bg-slate-50"}`}
          >{v || "Tất cả"}</button>
        ))}
      </div>

      {loading ? (
        <div className="flex justify-center py-16"><Loader2 className="w-8 h-8 animate-spin text-slate-300" /></div>
      ) : filtered.length === 0 ? (
        <div className="text-center py-16">
          <Ticket className="w-12 h-12 text-slate-200 mx-auto mb-3" />
          <p className="font-bold text-slate-500">Không có coupon nào</p>
        </div>
      ) : (
        <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
          {filtered.map(c => {
            const s = STATUS_MAP[c.status] ?? { label: c.status, cls: "bg-gray-100 text-gray-600" };
            return (
              <div key={c.id} className="bg-white rounded-xl border border-slate-100 p-4 hover:shadow-md transition-shadow">
                <div className="flex items-start justify-between gap-2 mb-2">
                  <div className="flex items-center gap-2">
                    {c.discountType === "PERCENTAGE" ? <Percent className="w-4 h-4 text-red-500" /> : <DollarSign className="w-4 h-4 text-red-500" />}
                    <span className="font-black text-sm text-slate-800 tracking-wider">{c.code}</span>
                    <button onClick={() => handleCopy(c.code, c.id)} className="p-0.5 rounded hover:bg-slate-100">
                      {copiedId === c.id ? <Check className="w-3 h-3 text-green-500" /> : <Copy className="w-3 h-3 text-slate-400" />}
                    </button>
                  </div>
                  <div className="flex items-center gap-1">
                    <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${s.cls}`}>{s.label}</span>
                    <button onClick={() => { setEditCoupon(c); setShowForm(true); }} className="p-1 rounded-lg hover:bg-slate-100">
                      <Pencil className="w-3.5 h-3.5 text-slate-400" />
                    </button>
                  </div>
                </div>
                <p className="text-xs text-slate-500">
                  Giảm {c.discountType === "PERCENTAGE" ? `${c.discountValue}%` : fmtPrice(c.discountValue)}
                  {c.maxDiscountAmount ? ` · Max ${fmtPrice(c.maxDiscountAmount)}` : ""}
                  {c.minOrderAmount ? ` · Min ${fmtPrice(c.minOrderAmount)}` : ""}
                </p>
                <div className="flex items-center gap-3 mt-2 text-[10px] text-slate-400">
                  <span className="flex items-center gap-1"><Calendar className="w-3 h-3" />{fmtDate(c.startDate)} — {fmtDate(c.endDate)}</span>
                  <span>Dùng: {c.usedQuantity}/{c.totalQuantity}</span>
                </div>
                <span className="inline-block mt-2 px-2 py-0.5 rounded text-[10px] font-bold bg-red-50 text-red-400">Coupon sàn</span>
              </div>
            );
          })}
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex justify-center gap-2">
          <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
            className="p-2 rounded-xl border border-slate-200 hover:bg-slate-50 disabled:opacity-40"><ChevronLeft className="w-4 h-4" /></button>
          <span className="px-4 py-2 text-sm font-bold text-slate-600">{page + 1} / {totalPages}</span>
          <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
            className="p-2 rounded-xl border border-slate-200 hover:bg-slate-50 disabled:opacity-40"><ChevronRight className="w-4 h-4" /></button>
        </div>
      )}

      {showForm && <CouponFormModal coupon={editCoupon} onClose={() => { setShowForm(false); setEditCoupon(null); }} onSaved={() => { setShowForm(false); setEditCoupon(null); load(); }} />}
    </div>
  );
}
