import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Loader2, Plus, Pencil, Trash2, ToggleLeft, ToggleRight,
  RefreshCw, ChevronLeft, ChevronRight, ImageOff,
  Search,
} from "lucide-react";
import {
  getMySellerProfile,
  getSellerProducts,
  updateProductStatus,
  deleteProduct,
} from "../../api/sellerDashboardApi";
import type { ProductResponse, ProductStatus } from "../../types/product";
import type { ProductPage } from "../../api/sellerDashboardApi";

// ─── helpers ───────────────────────────────────────────────────────────────────

const STATUS_CONFIG: Record<ProductStatus, { label: string; color: string }> = {
  DRAFT:   { label: "Nháp",        color: "bg-slate-100 text-slate-500"    },
  PENDING: { label: "Chờ duyệt",   color: "bg-yellow-100 text-yellow-700"  },
  ACTIVE:  { label: "Đang bán",    color: "bg-green-100 text-green-700"    },
  BANNED:  { label: "Bị cấm",      color: "bg-red-100 text-red-600"        },
  DELETED: { label: "Đã xóa",      color: "bg-slate-200 text-slate-400"    },
};

function fmtPrice(n: number) {
  return n.toLocaleString("vi-VN") + "₫";
}

// ─── Delete confirm ────────────────────────────────────────────────────────────

function DeleteModal({ product, onClose, onDone }: { product: ProductResponse; onClose: () => void; onDone: () => void }) {
  const [loading, setLoading] = useState(false);
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-full max-w-sm shadow-2xl p-6">
        <h3 className="font-black text-slate-800 mb-2">Xóa sản phẩm</h3>
        <p className="text-sm text-slate-500 mb-4">Bạn có chắc muốn xóa <strong>"{product.name}"</strong>? Thao tác này không thể hoàn tác.</p>
        <div className="flex gap-2 justify-end">
          <button onClick={onClose} className="px-4 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50">Hủy</button>
          <button disabled={loading} onClick={async () => { setLoading(true); try { await deleteProduct(product.id); onDone(); } finally { setLoading(false); } }}
            className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-60 flex items-center gap-2">
            {loading && <Loader2 className="w-3.5 h-3.5 animate-spin" />}Xóa
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Main page ──────────────────────────────────────────────────────────────────

type Modal = { type: "delete"; product: ProductResponse } | null;

export default function SellerProductsPage() {
  const navigate = useNavigate();
  const [sellerId,    setSellerId]    = useState<string>("");
  const [page,        setPage]        = useState<ProductPage | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<ProductStatus | "">("");
  const [keyword,     setKeyword]     = useState("");
  const [loading,     setLoading]     = useState(false);
  const [toggling,    setToggling]    = useState<string | null>(null);
  const [modal,       setModal]       = useState<Modal>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const load = async (p = 0, sf = statusFilter, kw = keyword, sid = sellerId) => {
    setLoading(true);
    try {
      const result = await getSellerProducts({
        sellerId: sid || undefined,
        status: sf || undefined,
        keyword: kw.trim() || undefined,
        page: p, size: 12,
        sortBy: "createdAt", sortDirection: "DESC",
      });
      setPage(result); setCurrentPage(p);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // Get seller profile first to obtain the seller UUID, then load products
    getMySellerProfile().then(profile => {
      setSellerId(profile.id);
      load(0, statusFilter, keyword, profile.id);
    }).catch(() => load(0));
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleKeyword = (v: string) => {
    setKeyword(v);
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => load(0, statusFilter, v), 400);
  };

  const handleToggle = async (product: ProductResponse) => {
    setToggling(product.id);
    try {
      const next: ProductStatus = product.status === "ACTIVE" ? "DRAFT" : "PENDING";
      await updateProductStatus(product.id, { status: next });
      load(currentPage);
    } finally {
      setToggling(null);
    }
  };

  const STATUS_FILTERS: { value: ProductStatus | ""; label: string }[] = [
    { value: "", label: "Tất cả" },
    { value: "ACTIVE",  label: "Đang bán"  },
    { value: "DRAFT",   label: "Nháp"      },
    { value: "PENDING", label: "Chờ duyệt" },
    { value: "BANNED",  label: "Bị cấm"   },
  ];

  return (
    <div className="space-y-5 max-w-5xl">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-xl font-black text-slate-900">Sản phẩm</h1>
          <p className="text-sm text-slate-400 mt-0.5">{page ? `${page.totalElements.toLocaleString()} sản phẩm` : "—"}</p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={() => load(currentPage)} className="p-2 rounded-xl border border-slate-200 hover:bg-slate-50">
            <RefreshCw className="w-4 h-4 text-slate-500" />
          </button>
          <button onClick={() => navigate("/seller/products/new")}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold">
            <Plus className="w-3.5 h-3.5" />Thêm sản phẩm
          </button>
        </div>
      </div>

      {/* Search + filter */}
      <div className="flex flex-wrap gap-2">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400" />
          <input
            className="pl-8 pr-3 py-1.5 rounded-xl border-2 border-slate-100 bg-white text-sm outline-none focus:border-red-400 w-52"
            placeholder="Tìm sản phẩm..."
            value={keyword}
            onChange={e => handleKeyword(e.target.value)}
          />
        </div>
        {STATUS_FILTERS.map(({ value, label }) => (
          <button key={value} onClick={() => { setStatusFilter(value); load(0, value, keyword); }}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold border transition-all ${
              statusFilter === value ? "bg-red-600 text-white border-red-600" : "bg-white text-slate-600 border-slate-200 hover:border-red-300"
            }`}>
            {label}
          </button>
        ))}
      </div>

      {/* Product grid */}
      {loading ? (
        <div className="flex items-center justify-center py-16 gap-2 text-slate-400">
          <Loader2 className="w-5 h-5 animate-spin" /><span className="text-sm font-semibold">Đang tải...</span>
        </div>
      ) : !page || page.content.length === 0 ? (
        <div className="text-center py-16 text-slate-400 text-sm font-semibold">Chưa có sản phẩm nào</div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {page.content.map(product => {
            const primaryImg = product.images.find(i => i.isPrimary)?.url ?? product.images[0]?.url;
            const cfg = STATUS_CONFIG[product.status];
            return (
              <div key={product.id} className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
                {/* Image */}
                <div className="h-40 bg-slate-100 flex items-center justify-center overflow-hidden">
                  {primaryImg
                    ? <img src={primaryImg} alt={product.name} className="w-full h-full object-cover" onError={e => { e.currentTarget.style.display = "none"; }} />
                    : <ImageOff className="w-8 h-8 text-slate-300" />}
                </div>
                {/* Body */}
                <div className="p-3 flex-1 flex flex-col gap-2">
                  <div className="flex items-start justify-between gap-2">
                    <p className="font-bold text-sm text-slate-800 line-clamp-2 flex-1">{product.name}</p>
                    <span className={`px-1.5 py-0.5 rounded-full text-[10px] font-bold shrink-0 ${cfg.color}`}>{cfg.label}</span>
                  </div>
                  <p className="text-sm font-black text-red-600">
                    {product.minPrice === product.maxPrice
                      ? fmtPrice(product.minPrice)
                      : `${fmtPrice(product.minPrice)} – ${fmtPrice(product.maxPrice)}`}
                  </p>
                  <p className="text-xs text-slate-400">{(product.soldCount ?? 0).toLocaleString()} đã bán</p>
                </div>
                {/* Actions */}
                <div className="flex items-center gap-1 px-3 pb-3">
                  <button onClick={() => navigate(`/seller/products/${product.id}/edit`)}
                    className="flex-1 flex items-center justify-center gap-1 py-1.5 rounded-lg text-xs font-bold border border-slate-200 hover:bg-slate-50 text-slate-600">
                    <Pencil className="w-3 h-3" />Sửa
                  </button>
                  <button
                    onClick={() => handleToggle(product)}
                    disabled={toggling === product.id || product.status === "BANNED" || product.status === "PENDING"}
                    className="p-1.5 rounded-lg border border-slate-200 hover:bg-slate-50 text-slate-500 disabled:opacity-40"
                    title={product.status === "ACTIVE" ? "Gỡ sản phẩm" : "Gửi duyệt"}
                  >
                    {toggling === product.id
                      ? <Loader2 className="w-4 h-4 animate-spin" />
                      : product.status === "ACTIVE"
                      ? <ToggleRight className="w-4 h-4 text-green-500" />
                      : <ToggleLeft className="w-4 h-4 text-slate-400" />}
                  </button>
                  <button onClick={() => setModal({ type: "delete", product })}
                    className="p-1.5 rounded-lg border border-slate-200 hover:bg-red-50 text-slate-400 hover:text-red-500">
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Pagination */}
      {page && page.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <span className="text-xs text-slate-400 font-semibold">Trang {currentPage + 1} / {page.totalPages}</span>
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

      {/* Modals */}
      {modal?.type === "delete" && (
        <DeleteModal
          product={modal.product}
          onClose={() => setModal(null)}
          onDone={() => { setModal(null); load(currentPage); }}
        />
      )}
    </div>
  );
}
