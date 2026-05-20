import { useEffect, useRef, useState } from "react";
import {
  Loader2,
  CheckCircle2,
  XCircle,
  Clock,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
  Filter,
  Search,
  ImageOff,
  ShieldAlert,
  Eye,
} from "lucide-react";
import {
  adminListProducts,
  adminUpdateProductStatus,
} from "../../api/adminApi";
import type { ProductResponse, ProductStatus } from "../../types/product";
import type { PageResponse } from "../../types/admin";

// ─── helpers ──────────────────────────────────────────────────────────────────

const STATUS_CONFIG: Record<ProductStatus, { label: string; color: string; icon: React.ReactNode }> = {
  DRAFT:   { label: "Nháp",        color: "bg-slate-100 text-slate-500",   icon: <Clock className="w-3 h-3" /> },
  PENDING: { label: "Chờ duyệt",   color: "bg-yellow-100 text-yellow-700", icon: <Clock className="w-3 h-3" /> },
  ACTIVE:  { label: "Đang bán",    color: "bg-green-100 text-green-700",   icon: <CheckCircle2 className="w-3 h-3" /> },
  BANNED:  { label: "Bị cấm",      color: "bg-red-100 text-red-600",       icon: <XCircle className="w-3 h-3" /> },
  DELETED: { label: "Đã xóa",      color: "bg-slate-200 text-slate-400",   icon: <XCircle className="w-3 h-3" /> },
};

function StatusBadge({ status }: { status: ProductStatus }) {
  const cfg = STATUS_CONFIG[status];
  return (
    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-bold ${cfg.color}`}>
      {cfg.icon}{cfg.label}
    </span>
  );
}

function fmtPrice(n: number) {
  return n.toLocaleString("vi-VN") + "₫";
}

function formatDate(iso: string | null) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("vi-VN");
}

// ─── Action modal ──────────────────────────────────────────────────────────────

interface ActionModalProps {
  product: ProductResponse;
  onClose: () => void;
  onDone: () => void;
}

function ActionModal({ product, onClose, onDone }: ActionModalProps) {
  const [action, setAction] = useState<"ACTIVE" | "BANNED" | "DRAFT">("ACTIVE");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  type ActionItem = { value: "ACTIVE" | "BANNED" | "DRAFT"; label: string; icon: React.ReactNode; desc: string };

  const AVAILABLE_ACTIONS: ActionItem[] = [];

  if (product.status === "PENDING") {
    AVAILABLE_ACTIONS.push(
      { value: "ACTIVE", label: "Duyệt", icon: <CheckCircle2 className="w-4 h-4 text-green-600" />, desc: "Phê duyệt sản phẩm để bán" },
      { value: "BANNED", label: "Từ chối", icon: <XCircle className="w-4 h-4 text-red-600" />, desc: "Từ chối sản phẩm này" },
      { value: "DRAFT", label: "Trả về nháp", icon: <Clock className="w-4 h-4 text-slate-500" />, desc: "Trả về cho seller chỉnh sửa" },
    );
  }
  if (product.status === "ACTIVE") {
    AVAILABLE_ACTIONS.push(
      { value: "BANNED", label: "Cấm bán", icon: <ShieldAlert className="w-4 h-4 text-red-600" />, desc: "Gỡ sản phẩm khỏi sàn" },
      { value: "DRAFT", label: "Trả về nháp", icon: <Clock className="w-4 h-4 text-slate-500" />, desc: "Chuyển về trạng thái nháp" },
    );
  }
  if (product.status === "BANNED") {
    AVAILABLE_ACTIONS.push(
      { value: "ACTIVE", label: "Bỏ cấm", icon: <CheckCircle2 className="w-4 h-4 text-green-600" />, desc: "Cho phép bán lại" },
    );
  }

  // Set default action to the first available one
  // eslint-disable-next-line react-hooks/rules-of-hooks
  useEffect(() => {
    if (AVAILABLE_ACTIONS.length > 0) setAction(AVAILABLE_ACTIONS[0].value);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSave = async () => {
    setSaving(true); setError(null);
    try {
      await adminUpdateProductStatus(product.id, action);
      onDone();
    } catch {
      setError("Thao tác thất bại. Vui lòng thử lại.");
    } finally {
      setSaving(false);
    }
  };

  const primaryImg = product.images?.find(i => i.isPrimary)?.url ?? product.images?.[0]?.url;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-full max-w-md shadow-2xl">
        <div className="px-5 py-4 border-b border-slate-100">
          <h3 className="font-black text-slate-800">Duyệt sản phẩm</h3>
          <p className="text-xs text-slate-400 mt-0.5 font-semibold truncate">{product.name}</p>
        </div>

        {/* Product preview */}
        <div className="px-5 pt-4 flex gap-3">
          <div className="w-16 h-16 rounded-xl bg-slate-100 overflow-hidden shrink-0 flex items-center justify-center">
            {primaryImg
              ? <img src={primaryImg} alt="" className="w-full h-full object-cover" />
              : <ImageOff className="w-6 h-6 text-slate-300" />}
          </div>
          <div className="flex-1 min-w-0">
            <p className="font-bold text-sm text-slate-800 line-clamp-2">{product.name}</p>
            <p className="text-sm font-black text-red-600 mt-0.5">
              {product.minPrice === product.maxPrice
                ? fmtPrice(product.minPrice)
                : `${fmtPrice(product.minPrice)} – ${fmtPrice(product.maxPrice)}`}
            </p>
            <div className="flex items-center gap-2 mt-1">
              <StatusBadge status={product.status} />
              <span className="text-[10px] text-slate-400">{product.skus?.length ?? 0} SKU</span>
            </div>
          </div>
        </div>

        <div className="p-5 space-y-4">
          {AVAILABLE_ACTIONS.length === 0 ? (
            <p className="text-sm text-slate-400 text-center py-4">
              Không có thao tác khả dụng cho trạng thái này.
            </p>
          ) : (
            <>
              <div className="space-y-2">
                {AVAILABLE_ACTIONS.map(({ value, label, icon, desc }) => (
                  <button
                    key={value}
                    onClick={() => setAction(value)}
                    className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl border-2 text-left transition-all ${
                      action === value
                        ? "border-red-500 bg-red-50"
                        : "border-slate-100 bg-slate-50 hover:border-slate-200"
                    }`}
                  >
                    {icon}
                    <div>
                      <p className="text-sm font-bold text-slate-800">{label}</p>
                      <p className="text-xs text-slate-400">{desc}</p>
                    </div>
                  </button>
                ))}
              </div>
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

// ─── Stat strip ────────────────────────────────────────────────────────────────

function StatStrip({ stats }: { stats: Record<string, number> }) {
  const items = [
    { label: "Chờ duyệt", value: stats.PENDING ?? 0, color: "text-yellow-600 bg-yellow-50 border-yellow-200" },
    { label: "Đang bán",   value: stats.ACTIVE ?? 0,  color: "text-green-600 bg-green-50 border-green-200"   },
    { label: "Nháp",       value: stats.DRAFT ?? 0,   color: "text-slate-600 bg-slate-50 border-slate-200"   },
    { label: "Bị cấm",     value: stats.BANNED ?? 0,  color: "text-red-600 bg-red-50 border-red-200"         },
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

// ─── Main page ──────────────────────────────────────────────────────────────────

type StatusFilter = "" | ProductStatus;

export default function AdminProductsPage() {
  const [page, setPage]                 = useState<PageResponse<ProductResponse> | null>(null);
  const [currentPage, setCurrentPage]   = useState(0);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("PENDING");
  const [keyword, setKeyword]           = useState("");
  const [loading, setLoading]           = useState(false);
  const [actionProduct, setActionProduct] = useState<ProductResponse | null>(null);
  const [stats, setStats]               = useState<Record<string, number>>({});
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const load = async (p = 0, sf: StatusFilter = statusFilter, kw = keyword) => {
    setLoading(true);
    try {
      const result = await adminListProducts({
        status: sf || undefined,
        keyword: kw.trim() || undefined,
        page: p,
        size: 15,
        sortBy: "createdAt",
        sortDirection: "DESC",
      });
      setPage(result);
      setCurrentPage(p);
    } finally {
      setLoading(false);
    }
  };

  const loadStats = async () => {
    try {
      const results = await Promise.all([
        adminListProducts({ status: "PENDING", page: 0, size: 1 }),
        adminListProducts({ status: "ACTIVE", page: 0, size: 1 }),
        adminListProducts({ status: "DRAFT", page: 0, size: 1 }),
        adminListProducts({ status: "BANNED", page: 0, size: 1 }),
      ]);
      setStats({
        PENDING: results[0].totalElements,
        ACTIVE:  results[1].totalElements,
        DRAFT:   results[2].totalElements,
        BANNED:  results[3].totalElements,
      });
    } catch { /* ignore */ }
  };

  useEffect(() => {
    load(0);
    loadStats();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleKeyword = (v: string) => {
    setKeyword(v);
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => load(0, statusFilter, v), 400);
  };

  const handleFilterChange = (v: StatusFilter) => {
    setStatusFilter(v);
    load(0, v, keyword);
  };

  const handleActionDone = () => {
    setActionProduct(null);
    load(currentPage);
    loadStats();
  };

  const FILTERS: { value: StatusFilter; label: string }[] = [
    { value: "",        label: "Tất cả"    },
    { value: "PENDING", label: "Chờ duyệt" },
    { value: "ACTIVE",  label: "Đang bán"  },
    { value: "DRAFT",   label: "Nháp"      },
    { value: "BANNED",  label: "Bị cấm"    },
  ];

  return (
    <div className="space-y-5 max-w-6xl">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-xl font-black text-slate-900">Duyệt sản phẩm</h1>
          <p className="text-sm text-slate-400 mt-0.5">
            {page ? `${page.totalElements.toLocaleString()} sản phẩm` : "—"}
          </p>
        </div>
        <button
          onClick={() => { load(currentPage); loadStats(); }}
          className="flex items-center gap-1.5 px-3 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50"
        >
          <RefreshCw className="w-3.5 h-3.5" /> Làm mới
        </button>
      </div>

      {/* Stats */}
      <StatStrip stats={stats} />

      {/* Search + filter */}
      <div className="flex flex-wrap gap-2 items-center">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400" />
          <input
            className="pl-8 pr-3 py-1.5 rounded-xl border-2 border-slate-100 bg-white text-sm outline-none focus:border-red-400 w-56"
            placeholder="Tìm sản phẩm..."
            value={keyword}
            onChange={e => handleKeyword(e.target.value)}
          />
        </div>
        <Filter className="w-4 h-4 text-slate-400" />
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
            Không có sản phẩm nào
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50">
                  {["Sản phẩm", "Giá", "Trạng thái", "SKU", "Đã bán", "Ngày tạo", ""].map(h => (
                    <th key={h} className="px-4 py-3 text-left text-[11px] font-black text-slate-400 uppercase tracking-widest whitespace-nowrap">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {page.content.map(product => {
                  const primaryImg = product.images?.find(i => i.isPrimary)?.url ?? product.images?.[0]?.url;
                  return (
                    <tr key={product.id} className="hover:bg-slate-50 transition-colors">
                      {/* Product info */}
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-3 max-w-xs">
                          <div className="w-10 h-10 rounded-lg bg-slate-100 overflow-hidden shrink-0 flex items-center justify-center">
                            {primaryImg
                              ? <img src={primaryImg} alt="" className="w-full h-full object-cover" />
                              : <ImageOff className="w-4 h-4 text-slate-300" />}
                          </div>
                          <div className="min-w-0">
                            <p className="font-bold text-slate-800 truncate">{product.name}</p>
                            <p className="text-[10px] text-slate-400 truncate">ID: {product.id.slice(0, 8)}…</p>
                          </div>
                        </div>
                      </td>
                      {/* Price */}
                      <td className="px-4 py-3 whitespace-nowrap">
                        <p className="font-bold text-red-600 text-xs">
                          {product.minPrice === product.maxPrice
                            ? fmtPrice(product.minPrice)
                            : `${fmtPrice(product.minPrice)} – ${fmtPrice(product.maxPrice)}`}
                        </p>
                      </td>
                      {/* Status */}
                      <td className="px-4 py-3">
                        <StatusBadge status={product.status} />
                      </td>
                      {/* SKU count */}
                      <td className="px-4 py-3 text-slate-500 text-xs font-semibold">
                        {product.skus?.length ?? 0}
                      </td>
                      {/* Sold */}
                      <td className="px-4 py-3 text-slate-500 text-xs font-semibold">
                        {(product.soldCount ?? 0).toLocaleString()}
                      </td>
                      {/* Date */}
                      <td className="px-4 py-3 text-slate-400 whitespace-nowrap text-xs">
                        {formatDate(product.createdAt)}
                      </td>
                      {/* Actions */}
                      <td className="px-4 py-3 text-right">
                        <div className="flex items-center gap-1 justify-end">
                          <a
                            href={`/product/${product.id}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="p-1.5 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-600"
                            title="Xem sản phẩm"
                          >
                            <Eye className="w-3.5 h-3.5" />
                          </a>
                          {product.status !== "DRAFT" && product.status !== "DELETED" && (
                            <button
                              onClick={() => setActionProduct(product)}
                              className="px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-red-100 text-slate-600 hover:text-red-700 text-xs font-bold transition-colors"
                            >
                              Thao tác
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
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

      {/* Action modal */}
      {actionProduct && (
        <ActionModal
          product={actionProduct}
          onClose={() => setActionProduct(null)}
          onDone={handleActionDone}
        />
      )}
    </div>
  );
}
