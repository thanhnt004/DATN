import { useEffect, useState } from "react";
import {
  Loader2,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Clock,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
  Filter,
  Store,
  X,
  Info,
} from "lucide-react";
import {
  adminListSellers,
  adminApproveSeller,
  adminRejectSeller,
  adminSuspendSeller,
  adminReactivateSeller,
  adminGetSellerStats,
} from "../../api/adminApi";
import type { AdminSellerResponse, SellerStatus, SellerStatsResponse, PageResponse } from "../../types/admin";

// ─── helpers ──────────────────────────────────────────────────────────────────

const STATUS_CONFIG: Record<SellerStatus, { label: string; color: string; icon: React.ReactNode }> = {
  PENDING:   { label: "Chờ duyệt",     color: "bg-yellow-100 text-yellow-700",  icon: <Clock className="w-3 h-3" /> },
  ACTIVE:    { label: "Hoạt động",     color: "bg-green-100 text-green-700",    icon: <CheckCircle2 className="w-3 h-3" /> },
  REJECTED:  { label: "Từ chối",       color: "bg-red-100 text-red-700",        icon: <XCircle className="w-3 h-3" /> },
  SUSPENDED: { label: "Tạm ngưng",     color: "bg-orange-100 text-orange-700",  icon: <AlertTriangle className="w-3 h-3" /> },
  BANNED:    { label: "Bị cấm",        color: "bg-slate-200 text-slate-500",    icon: <XCircle className="w-3 h-3" /> },
  CLOSED:    { label: "Đã đóng",       color: "bg-slate-100 text-slate-400",    icon: <X className="w-3 h-3" /> },
};

function StatusBadge({ status }: { status: SellerStatus }) {
  const { label, color, icon } = STATUS_CONFIG[status];
  return (
    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-bold ${color}`}>
      {icon}{label}
    </span>
  );
}

function formatDate(iso: string | null) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("vi-VN");
}

// ─── Action modal ──────────────────────────────────────────────────────────────

interface ActionModalProps {
  seller: AdminSellerResponse;
  onClose: () => void;
  onDone: () => void;
}

function ActionModal({ seller, onClose, onDone }: ActionModalProps) {
  const [action, setAction] = useState<"approve" | "reject" | "suspend" | "reactivate">("approve");
  const [reason, setReason] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const AVAILABLE_ACTIONS = [
    ...(seller.status === "PENDING" ? [
      { value: "approve" as const,    label: "Duyệt",          icon: <CheckCircle2 className="w-4 h-4 text-green-600" /> },
      { value: "reject" as const,     label: "Từ chối",        icon: <XCircle className="w-4 h-4 text-red-600" /> },
    ] : []),
    ...(seller.status === "ACTIVE" ? [
      { value: "suspend" as const,    label: "Tạm ngưng",      icon: <AlertTriangle className="w-4 h-4 text-orange-500" /> },
    ] : []),
    ...((seller.status === "SUSPENDED" || seller.status === "BANNED") ? [
      { value: "reactivate" as const, label: "Kích hoạt lại",  icon: <CheckCircle2 className="w-4 h-4 text-green-600" /> },
    ] : []),
  ];

  const handleSave = async () => {
    if ((action === "reject" || action === "suspend") && !reason.trim()) {
      setError("Vui lòng nhập lý do");
      return;
    }
    setSaving(true); setError(null);
    try {
      if (action === "approve")    await adminApproveSeller(seller.id);
      if (action === "reject")     await adminRejectSeller(seller.id, { reason });
      if (action === "suspend")    await adminSuspendSeller(seller.id, reason);
      if (action === "reactivate") await adminReactivateSeller(seller.id);
      onDone();
    } catch {
      setError("Thao tác thất bại. Vui lòng thử lại.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-full max-w-sm shadow-2xl">
        <div className="px-5 py-4 border-b border-slate-100">
          <h3 className="font-black text-slate-800">Thao tác cửa hàng</h3>
          <p className="text-xs text-slate-400 mt-0.5 font-semibold">{seller.shopName}</p>
        </div>

        {/* Rejection reason if exists */}
        {seller.rejectionReason && (
          <div className="mx-5 mt-4 p-3 bg-red-50 border border-red-100 rounded-xl flex gap-2">
            <Info className="w-3.5 h-3.5 text-red-500 shrink-0 mt-0.5" />
            <p className="text-xs text-red-600 font-semibold">{seller.rejectionReason}</p>
          </div>
        )}

        <div className="p-5 space-y-4">
          {AVAILABLE_ACTIONS.length === 0 ? (
            <p className="text-sm text-slate-400 text-center py-4">Không có thao tác khả dụng cho trạng thái này.</p>
          ) : (
            <>
              <div className="grid grid-cols-2 gap-2">
                {AVAILABLE_ACTIONS.map(({ value, label, icon }) => (
                  <button
                    key={value}
                    onClick={() => setAction(value)}
                    className={`flex items-center gap-2 px-3 py-2.5 rounded-xl border-2 text-sm font-semibold transition-all ${
                      action === value ? "border-red-500 bg-red-50" : "border-slate-100 bg-slate-50 hover:border-slate-200"
                    }`}
                  >
                    {icon}{label}
                  </button>
                ))}
              </div>
              {(action === "reject" || action === "suspend") && (
                <div className="space-y-1">
                  <label className="text-xs font-bold text-slate-400 uppercase tracking-widest">
                    Lý do <span className="text-red-500">*</span>
                  </label>
                  <textarea
                    rows={2}
                    className="w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400 resize-none"
                    placeholder="Nhập lý do..."
                    value={reason}
                    onChange={e => setReason(e.target.value)}
                  />
                </div>
              )}
              {error && <p className="text-xs text-red-500 font-semibold">{error}</p>}
            </>
          )}
        </div>

        <div className="px-5 pb-5 flex gap-2 justify-end">
          <button onClick={onClose} className="px-4 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50">
            Đóng
          </button>
          {AVAILABLE_ACTIONS.length > 0 && (
            <button
              onClick={handleSave}
              disabled={saving}
              className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-60 flex items-center gap-2"
            >
              {saving && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
              Xác nhận
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

// ─── Stat strip ───────────────────────────────────────────────────────────────

function StatStrip({ stats }: { stats: SellerStatsResponse }) {
  const items = [
    { label: "Chờ duyệt",   value: stats.pendingSellers,   color: "text-yellow-600 bg-yellow-50 border-yellow-200" },
    { label: "Hoạt động",   value: stats.activeSellers,    color: "text-green-600 bg-green-50 border-green-200" },
    { label: "Từ chối",     value: stats.rejectedSellers,  color: "text-red-600 bg-red-50 border-red-200" },
    { label: "Tạm ngưng",   value: stats.suspendedSellers, color: "text-orange-600 bg-orange-50 border-orange-200" },
    { label: "Bị cấm",      value: stats.bannedSellers,    color: "text-slate-600 bg-slate-50 border-slate-200" },
  ];
  return (
    <div className="flex flex-wrap gap-2">
      {items.map(({ label, value, color }) => (
        <div key={label} className={`flex items-center gap-2 px-3 py-1.5 rounded-xl border text-xs font-bold ${color}`}>
          <span className="text-base font-black">{value}</span>
          {label}
        </div>
      ))}
    </div>
  );
}

// ─── Main page ─────────────────────────────────────────────────────────────────

type StatusFilter = "" | SellerStatus;

export default function AdminSellersPage() {
  const [page, setPage]                 = useState<PageResponse<AdminSellerResponse> | null>(null);
  const [currentPage, setCurrentPage]   = useState(0);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("PENDING");
  const [stats, setStats]               = useState<SellerStatsResponse | null>(null);
  const [loading, setLoading]           = useState(false);
  const [actionSeller, setActionSeller] = useState<AdminSellerResponse | null>(null);

  const load = async (p = 0, st: StatusFilter = statusFilter) => {
    setLoading(true);
    try {
      const [result, statsResult] = await Promise.all([
        adminListSellers({ status: st || undefined, page: p, size: 15 }),
        adminGetSellerStats(),
      ]);
      setPage(result);
      setStats(statsResult);
      setCurrentPage(p);
    } finally {
      setLoading(false);
    }
  };

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { load(0); }, []);

  const handleFilterChange = (v: StatusFilter) => {
    setStatusFilter(v);
    load(0, v);
  };

  const handleActionDone = () => {
    setActionSeller(null);
    load(currentPage);
  };

  const FILTERS: { value: StatusFilter; label: string }[] = [
    { value: "",          label: "Tất cả" },
    { value: "PENDING",   label: "Chờ duyệt" },
    { value: "ACTIVE",    label: "Hoạt động" },
    { value: "REJECTED",  label: "Từ chối" },
    { value: "SUSPENDED", label: "Tạm ngưng" },
    { value: "BANNED",    label: "Bị cấm" },
  ];

  return (
    <div className="space-y-5 max-w-6xl">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-xl font-black text-slate-900">Quản lý bán hàng</h1>
          <p className="text-sm text-slate-400 mt-0.5">
            {page ? `${page.totalElements.toLocaleString()} cửa hàng` : "—"}
          </p>
        </div>
        <button onClick={() => load(currentPage)} className="flex items-center gap-1.5 px-3 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50">
          <RefreshCw className="w-3.5 h-3.5" /> Làm mới
        </button>
      </div>

      {stats && <StatStrip stats={stats} />}

      {/* Status filter tabs */}
      <div className="flex flex-wrap gap-1.5">
        <Filter className="w-4 h-4 text-slate-400 self-center" />
        {FILTERS.map(({ value, label }) => (
          <button
            key={value}
            onClick={() => handleFilterChange(value)}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all border ${
              statusFilter === value
                ? "bg-red-600 text-white border-red-600"
                : "bg-white text-slate-600 border-slate-200 hover:border-red-300"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {/* Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-16 gap-2 text-slate-400">
            <Loader2 className="w-5 h-5 animate-spin" />
            <span className="text-sm font-semibold">Đang tải...</span>
          </div>
        ) : !page || page.content.length === 0 ? (
          <div className="text-center py-16 text-slate-400 text-sm font-semibold">
            Không có cửa hàng nào
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50">
                  {["Cửa hàng", "Loại", "Trạng thái", "Email", "Ngày đăng ký", "Lý do từ chối", ""].map(h => (
                    <th key={h} className="px-4 py-3 text-left text-[11px] font-black text-slate-400 uppercase tracking-widest whitespace-nowrap">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {page.content.map(seller => (
                  <tr key={seller.id} className="hover:bg-slate-50 transition-colors">
                    <td className="px-4 py-3 whitespace-nowrap">
                      <div className="flex items-center gap-2.5">
                        <div className="w-8 h-8 rounded-xl bg-red-100 flex items-center justify-center">
                          <Store className="w-4 h-4 text-red-600" />
                        </div>
                        <div>
                          <p className="font-bold text-slate-800">{seller.shopName}</p>
                          <p className="text-[11px] text-slate-400">{seller.city || "—"}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-0.5 rounded-full text-[11px] font-bold ${seller.sellerType === "BUSINESS" ? "bg-purple-100 text-purple-700" : "bg-blue-100 text-blue-700"}`}>
                        {seller.sellerType === "BUSINESS" ? "Doanh nghiệp" : "Cá nhân"}
                      </span>
                    </td>
                    <td className="px-4 py-3"><StatusBadge status={seller.status} /></td>
                    <td className="px-4 py-3 text-slate-500 truncate max-w-[160px]">{seller.email}</td>
                    <td className="px-4 py-3 text-slate-400 whitespace-nowrap">{formatDate(seller.createdAt)}</td>
                    <td className="px-4 py-3 text-slate-400 max-w-[200px] truncate text-xs">
                      {seller.rejectionReason || "—"}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => setActionSeller(seller)}
                        className="px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-red-100 text-slate-600 hover:text-red-700 text-xs font-bold transition-colors"
                      >
                        Thao tác
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {page && page.totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-slate-100">
            <span className="text-xs text-slate-400 font-semibold">
              Trang {currentPage + 1} / {page.totalPages}
            </span>
            <div className="flex gap-1">
              <button disabled={page.first} onClick={() => load(currentPage - 1)}
                className="p-1.5 rounded-lg border border-slate-200 disabled:opacity-40 hover:bg-slate-50">
                <ChevronLeft className="w-4 h-4 text-slate-600" />
              </button>
              <button disabled={page.last} onClick={() => load(currentPage + 1)}
                className="p-1.5 rounded-lg border border-slate-200 disabled:opacity-40 hover:bg-slate-50">
                <ChevronRight className="w-4 h-4 text-slate-600" />
              </button>
            </div>
          </div>
        )}
      </div>

      {actionSeller && (
        <ActionModal
          seller={actionSeller}
          onClose={() => setActionSeller(null)}
          onDone={handleActionDone}
        />
      )}
    </div>
  );
}
