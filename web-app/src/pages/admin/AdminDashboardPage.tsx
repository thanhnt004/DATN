import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Users, Store, Image, Tag, Clock, CheckCircle2, XCircle, AlertTriangle, ArrowRight, Loader2, KeyRound, Ticket, Megaphone } from "lucide-react";
import { adminGetSellerStats } from "../../api/adminApi";
import type { SellerStatsResponse } from "../../types/admin";

interface StatCardProps {
  label: string;
  value: number | string;
  color: string;
  icon: React.FC<React.SVGProps<SVGSVGElement>>;
}
function StatCard({ label, value, color, icon: Icon }: StatCardProps) {
  return (
    <div className={`bg-white rounded-2xl border border-slate-200 p-5 flex items-center gap-4 shadow-sm`}>
      <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${color}`}>
        <Icon style={{ width: 22, height: 22 }} />
      </div>
      <div>
        <p className="text-2xl font-black text-slate-900">{value}</p>
        <p className="text-xs font-semibold text-slate-500 mt-0.5">{label}</p>
      </div>
    </div>
  );
}

interface QuickLinkProps {
  to: string;
  icon: React.FC<React.SVGProps<SVGSVGElement>>;
  label: string;
  desc: string;
  color: string;
}

function QuickLink({ to, icon: Icon, label, desc, color }: QuickLinkProps) {
  return (
    <Link
      to={to}
      className="group bg-white rounded-2xl border border-slate-200 p-5 shadow-sm hover:shadow-md hover:border-red-200 transition-all flex items-center gap-4"
    >
      <div className={`w-11 h-11 rounded-xl flex items-center justify-center ${color}`}>
        <Icon style={{ width: 20, height: 20 }} />
      </div>
      <div className="flex-1 min-w-0">
        <p className="font-black text-slate-800 text-sm">{label}</p>
        <p className="text-xs text-slate-400 mt-0.5 truncate">{desc}</p>
      </div>
      <ArrowRight className="w-4 h-4 text-slate-300 group-hover:text-red-500 transition-colors" />
    </Link>
  );
}

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<SellerStatsResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    adminGetSellerStats()
      .then((sellerStats) => {
        setStats(sellerStats);
      })
      .catch(() => {
        // ignore – stats are informational
      })
      .finally(() => setLoading(false));
  }, []);

  const sellerStatCards = stats
    ? [
        { label: "Đang chờ duyệt",    value: stats.pendingSellers,   icon: Clock,         color: "bg-yellow-100 text-yellow-600" },
        { label: "Cửa hàng hoạt động", value: stats.activeSellers,   icon: CheckCircle2,  color: "bg-green-100 text-green-600"  },
        { label: "Đã từ chối",         value: stats.rejectedSellers,  icon: XCircle,       color: "bg-red-100 text-red-500"      },
        { label: "Tạm ngưng / Cấm",    value: stats.suspendedSellers + stats.bannedSellers, icon: AlertTriangle, color: "bg-orange-100 text-orange-600" },
        { label: "Tổng cửa hàng",      value: stats.totalSellers,    icon: Store,         color: "bg-red-100 text-red-600" },
      ]
    : [];


  return (
    <div className="space-y-6 max-w-5xl">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-black text-slate-900">Tổng quan hệ thống</h1>
        <p className="text-sm text-slate-500 mt-1">Chào mừng quay trở lại, quản trị viên.</p>
      </div>

      {/* Seller stats */}
      <section>
        <h2 className="text-sm font-black text-slate-500 uppercase tracking-widest mb-3">Thống kê cửa hàng</h2>
        {loading ? (
          <div className="flex items-center gap-2 text-slate-400 text-sm py-6">
            <Loader2 className="w-4 h-4 animate-spin" /> Đang tải...
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
            {sellerStatCards.map(({ label, value, icon, color }) => (
              <StatCard key={label} label={label} value={value} icon={icon} color={color} />
            ))}
          </div>
        )}
      </section>

      {/* Quick links */}
      <section>
        <h2 className="text-sm font-black text-slate-500 uppercase tracking-widest mb-3">Truy cập nhanh</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          <QuickLink
            to="/admin/sellers"
            icon={Store}
            label="Duyệt đơn bán hàng"
            desc="Xem và duyệt các đơn đăng ký chờ phê duyệt"
            color="bg-red-100 text-red-600"
          />
          <QuickLink
            to="/admin/users"
            icon={Users}
            label="Người dùng"
            desc="Quản lý tài khoản và trạng thái người dùng"
            color="bg-blue-100 text-blue-600"
          />
          <QuickLink
            to="/admin/banners"
            icon={Image}
            label="Banners quảng cáo"
            desc="Tạo và quản lý banner hiển thị trên trang chủ"
            color="bg-purple-100 text-purple-600"
          />
          <QuickLink
            to="/admin/categories"
            icon={Tag}
            label="Danh mục sản phẩm"
            desc="Quản lý cây danh mục sản phẩm toàn hệ thống"
            color="bg-emerald-100 text-emerald-600"
          />
          <QuickLink
            to="/admin/roles"
            icon={KeyRound}
            label="Quản lý Roles"
            desc="Phân quyền và quản lý vai trò người dùng"
            color="bg-amber-100 text-amber-600"
          />
          <QuickLink
            to="/admin/coupons"
            icon={Ticket}
            label="Mã giảm giá"
            desc="Tạo và quản lý coupon khuyến mãi toàn sàn"
            color="bg-pink-100 text-pink-600"
          />
          <QuickLink
            to="/admin/campaigns"
            icon={Megaphone}
            label="Chiến dịch"
            desc="Quản lý các chiến dịch khuyến mãi lớn"
            color="bg-orange-100 text-orange-600"
          />
        </div>
      </section>
    </div>
  );
}
