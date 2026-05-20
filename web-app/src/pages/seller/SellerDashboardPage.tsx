import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  Store, Package, ShoppingBag, Star,
  TrendingUp, Clock, CheckCircle2, Truck,
  Loader2, ChevronRight, AlertCircle,
  AlertTriangle, DollarSign, FileText, Landmark,
} from "lucide-react";
import { getMySellerProfile, getSellerOrders, getSellerProducts, getLowStockInventories } from "../../api/sellerDashboardApi";
import type { SellerResponse } from "../../types/seller";
import type { OrderResponse } from "../../types/order";

// ─── helpers ───────────────────────────────────────────────────────────────────

function fmt(n: number | null | undefined) {
  return (n ?? 0).toLocaleString("vi-VN");
}

function StatCard({
  icon: Icon, label, value, sub, color,
}: {
  icon: React.ElementType; label: string; value: string | number; sub?: string; color: string;
}) {
  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-4 flex items-start gap-3 shadow-sm">
      <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${color}`}>
        <Icon className="w-5 h-5" />
      </div>
      <div>
        <p className="text-xs text-slate-400 font-semibold uppercase tracking-widest">{label}</p>
        <p className="text-xl font-black text-slate-800">{value}</p>
        {sub && <p className="text-[11px] text-slate-400 mt-0.5">{sub}</p>}
      </div>
    </div>
  );
}

const ORDER_STATUS_CONFIG: Record<string, { label: string; color: string }> = {
  PENDING:   { label: "Chờ xử lý",  color: "bg-yellow-100 text-yellow-700"  },
  CONFIRMED: { label: "Đã xác nhận", color: "bg-blue-100 text-blue-700"     },
  SHIPPED:   { label: "Đang giao",  color: "bg-blue-100 text-blue-700"  },
  DELIVERED: { label: "Đã giao",    color: "bg-green-100 text-green-700"    },
  CANCELLED: { label: "Đã hủy",     color: "bg-red-100 text-red-700"        },
  COMPLETED: { label: "Hoàn thành", color: "bg-emerald-100 text-emerald-700" },
};

// ─── Main ──────────────────────────────────────────────────────────────────────

export default function SellerDashboardPage() {
  const [profile, setProfile] = useState<SellerResponse | null>(null);
  const [orders,  setOrders]  = useState<OrderResponse[]>([]);
  const [productCount, setProductCount] = useState<number>(0);
  const [lowStockCount, setLowStockCount] = useState<number>(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const p = await getMySellerProfile();
        setProfile(p);
        const [o, prod, lowStock] = await Promise.all([
          getSellerOrders({ page: 0, size: 5 }),
          getSellerProducts({ sellerId: p.id, page: 0, size: 1 }),
          getLowStockInventories().catch(() => []),
        ]);
        setOrders(o.content ?? []);
        setProductCount(prod.totalElements);
        setLowStockCount(lowStock.length);
      } catch {
        // ignore
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24 gap-2 text-slate-400">
        <Loader2 className="w-5 h-5 animate-spin" /><span className="text-sm font-semibold">Đang tải...</span>
      </div>
    );
  }

  const pendingCount   = orders.filter(o => o.status === "PENDING").length;
  const confirmedCount = orders.filter(o => o.status === "CONFIRMED").length;
  const shippedCount   = orders.filter(o => o.status === "SHIPPED").length;

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      {/* Shop header */}
      {profile && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          {/* Banner */}
          <div className="h-24 bg-gradient-to-r from-red-600 to-red-600 relative">
            {profile.bannerUrl && (
              <img src={profile.bannerUrl} alt="banner" className="w-full h-full object-cover" />
            )}
          </div>
          <div className="px-5 pb-5 flex items-end gap-4 -mt-8 relative z-10">
            <div className="w-16 h-16 rounded-2xl border-4 border-white bg-red-100 flex items-center justify-center overflow-hidden shadow">
              {profile.logoUrl
                ? <img src={profile.logoUrl} alt="logo" className="w-full h-full object-cover" />
                : <Store className="w-8 h-8 text-red-600" />
              }
            </div>
            <div className="flex-1 min-w-0 pb-1">
              <h2 className="text-lg font-black text-slate-800 truncate">{profile.shopName}</h2>
              <p className="text-xs text-slate-400">{profile.city ? `${profile.city} · ` : ""}{profile.sellerType === "BUSINESS" ? "Doanh nghiệp" : "Cá nhân"}</p>
            </div>
            <Link to="/seller/shop"
              className="shrink-0 flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-slate-200 text-xs font-bold text-slate-600 hover:border-red-300 hover:text-red-600 transition-colors mb-1">
              Chỉnh sửa <ChevronRight className="w-3 h-3" />
            </Link>
          </div>
        </div>
      )}

      {/* Alert if not active */}
      {profile && profile.status !== "ACTIVE" && (
        <div className="flex items-start gap-3 p-4 bg-yellow-50 border border-yellow-200 rounded-2xl">
          <AlertCircle className="w-4 h-4 text-yellow-600 shrink-0 mt-0.5" />
          <p className="text-sm text-yellow-700 font-semibold">
            Shop hiện có trạng thái <strong>{profile.status}</strong>.{" "}
            {profile.rejectionReason && `Lý do: ${profile.rejectionReason}`}
          </p>
        </div>
      )}



      {/* Low stock warning */}
      {lowStockCount > 0 && (
        <div className="flex items-start gap-3 p-4 bg-orange-50 border border-orange-200 rounded-2xl">
          <AlertTriangle className="w-4 h-4 text-orange-600 shrink-0 mt-0.5" />
          <div className="flex-1">
            <p className="text-sm text-orange-700 font-semibold">
              Có <strong>{lowStockCount}</strong> SKU sắp hết hàng
            </p>
            <p className="text-xs text-orange-500 mt-0.5">Kiểm tra tồn kho và bổ sung hàng kịp thời.</p>
          </div>
          <Link to="/seller/products" className="shrink-0 px-3 py-1.5 rounded-xl border border-orange-300 text-xs font-bold text-orange-700 hover:bg-orange-100 transition-colors">
            Xem sản phẩm
          </Link>
        </div>
      )}

      {/* Quick links */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        {[
          { to: "/seller/shop",      icon: Store,       label: "Thông tin shop",      desc: "Cập nhật logo, mô tả, địa chỉ" },
          { to: "/seller/products",  icon: Package,     label: "Quản lý sản phẩm",    desc: "Thêm, sửa, xóa sản phẩm" },
          { to: "/seller/orders",    icon: ShoppingBag, label: "Quản lý đơn hàng",    desc: "Xác nhận, giao hàng, hủy đơn" },
          { to: "/seller/revenue",   icon: DollarSign,  label: "Doanh thu",            desc: "Thống kê doanh thu, top sản phẩm" },
          { to: "/seller/documents", icon: FileText,    label: "Giấy tờ xác minh",    desc: "Tải lên CMND, giấy phép KD" },
          { to: "/seller/bank",      icon: Landmark,    label: "Tài khoản ngân hàng", desc: "Cài đặt nhận thanh toán" },
        ].map(({ to, icon: Icon, label, desc }) => (
          <Link key={to} to={to}
            className="flex items-center gap-3 bg-white rounded-2xl border border-slate-200 p-4 shadow-sm hover:border-red-300 hover:shadow-md transition-all">
            <div className="w-10 h-10 rounded-xl bg-red-50 flex items-center justify-center shrink-0">
              <Icon className="w-5 h-5 text-red-600" />
            </div>
            <div className="min-w-0">
              <p className="font-bold text-sm text-slate-800">{label}</p>
              <p className="text-xs text-slate-400 truncate">{desc}</p>
            </div>
            <ChevronRight className="w-4 h-4 text-slate-300 ml-auto shrink-0" />
          </Link>
        ))}
      </div>

      {/* Recent orders */}
      {orders.length > 0 && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="flex items-center justify-between px-5 py-3.5 border-b border-slate-100">
            <h3 className="font-black text-slate-800 text-sm">Đơn hàng gần đây</h3>
            <Link to="/seller/orders" className="text-xs text-red-600 font-bold hover:underline">Xem tất cả</Link>
          </div>
          <div className="divide-y divide-slate-50">
            {orders.map(order => {
              const cfg = ORDER_STATUS_CONFIG[order.status] ?? { label: order.status, color: "bg-slate-100 text-slate-500" };
              return (
                <div key={order.id} className="flex items-center gap-3 px-5 py-3">
                  <div className="flex-1 min-w-0">
                    <p className="font-bold text-sm text-slate-800">#{order.orderNumber}</p>
                    <p className="text-xs text-slate-400">{order.recipientName} · {order.items.length} sản phẩm</p>
                  </div>
                  <p className="font-bold text-sm text-slate-800 shrink-0">
                    {order.totalAmount.toLocaleString("vi-VN")}₫
                  </p>
                  <span className={`px-2 py-0.5 rounded-full text-[11px] font-bold shrink-0 ${cfg.color}`}>{cfg.label}</span>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
