import { useState } from "react";
import { NavLink, Outlet, Link, useNavigate } from "react-router-dom";
import {
  LayoutDashboard,
  Store,
  Package,
  ShoppingBag,
  Ticket,
  LogOut,
  Menu,
  X,
  ChevronRight,
  DollarSign,
  FileText,
  Landmark,
  SlidersHorizontal,
  MessageSquare,
  Star,
} from "lucide-react";
import { useDispatch } from "react-redux";
import type { AppDispatch } from "../../store/store";
import { logoutThunk } from "../../store/authThunks";

const NAV = [
  { to: "/seller",           icon: LayoutDashboard, label: "Tổng quan",         end: true },
  { to: "/seller/shop",      icon: Store,           label: "Thông tin shop"     },
  { to: "/seller/products",  icon: Package,         label: "Sản phẩm"           },
  { to: "/seller/options",   icon: SlidersHorizontal, label: "Phân loại hàng"    },
  { to: "/seller/orders",    icon: ShoppingBag,     label: "Đơn hàng"           },
  { to: "/seller/reviews",   icon: Star,            label: "Đánh giá"           },
  { to: "/seller/coupons",   icon: Ticket,          label: "Mã giảm giá"        },
  { to: "/seller/revenue",   icon: DollarSign,      label: "Doanh thu"          },
  { to: "/seller/messages",  icon: MessageSquare,   label: "Tin nhắn"           },
  { to: "/seller/documents", icon: FileText,        label: "Giấy tờ xác minh"   },
  { to: "/seller/bank",      icon: Landmark,        label: "Ngân hàng"          },
];

const base   = "flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-semibold transition-all";
const active = "bg-red-600 text-white shadow-sm";
const idle   = "text-slate-400 hover:bg-slate-800 hover:text-white";

function SidebarContent({ onNavClick }: { onNavClick?: () => void }) {
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();
  const handleLogout = async () => {
    await dispatch(logoutThunk());
    navigate("/login");
  };
  return (
    <nav className="flex flex-col h-full py-4">
      <Link to="/seller" className="flex items-center gap-2.5 px-4 pb-5 border-b border-slate-800 mb-3">
        <div className="w-8 h-8 bg-red-600 rounded-xl flex items-center justify-center">
          <Store className="w-4 h-4 text-white" />
        </div>
        <span className="font-black text-white text-base tracking-tight">
          Seller <span className="text-red-400 font-bold text-xs">HUB</span>
        </span>
      </Link>

      <div className="flex-1 px-2 space-y-0.5 overflow-y-auto">
        {NAV.map(({ to, icon: Icon, label, end }) => (
          <NavLink
            key={to} to={to} end={end}
            onClick={onNavClick}
            className={({ isActive }) => `${base} ${isActive ? active : idle}`}
          >
            <Icon className="w-4 h-4 shrink-0" />
            {label}
            <ChevronRight className="w-3 h-3 ml-auto opacity-0 group-hover:opacity-100" />
          </NavLink>
        ))}
      </div>

      <div className="px-2 pt-3 border-t border-slate-800">
        <button onClick={handleLogout}
          className={`${base} w-full text-slate-400 hover:bg-red-900/40 hover:text-red-400`}>
          <LogOut className="w-4 h-4 shrink-0" />
          Đăng xuất
        </button>
      </div>
    </nav>
  );
}

export default function SellerLayout() {
  const [open, setOpen] = useState(false);

  return (
    <div className="flex min-h-screen bg-slate-100 font-sans">
      {/* Desktop sidebar */}
      <aside className="hidden md:flex flex-col w-56 shrink-0 bg-slate-900 fixed inset-y-0 left-0 z-30">
        <SidebarContent />
      </aside>

      {/* Mobile overlay */}
      {open && (
        <div className="fixed inset-0 z-40 md:hidden">
          <div className="absolute inset-0 bg-black/50" onClick={() => setOpen(false)} />
          <div className="absolute left-0 top-0 h-full w-56 bg-slate-900">
            <button onClick={() => setOpen(false)} className="absolute top-3 right-3 p-1 text-slate-400 hover:text-white">
              <X className="w-5 h-5" />
            </button>
            <SidebarContent onNavClick={() => setOpen(false)} />
          </div>
        </div>
      )}

      {/* Main content */}
      <div className="flex-1 md:ml-56 flex flex-col min-h-screen">
        {/* Topbar */}
        <header className="bg-white border-b border-slate-200 sticky top-0 z-20 px-4 md:px-6 py-3 flex items-center gap-3">
          <button onClick={() => setOpen(true)} className="md:hidden p-1.5 rounded-lg hover:bg-slate-100">
            <Menu className="w-5 h-5 text-slate-600" />
          </button>
          <span className="font-black text-slate-700 text-sm ml-auto px-2.5 py-1 bg-red-50 text-red-700 rounded-xl">
            Kênh người bán
          </span>
        </header>

        <main className="flex-1 p-4 md:p-6">
          <div className="max-w-6xl mx-auto w-full">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
