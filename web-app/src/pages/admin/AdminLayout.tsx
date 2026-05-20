import { useState } from "react";
import { NavLink, Outlet, Link, useNavigate } from "react-router-dom";
import {
  LayoutDashboard,
  Users,
  Store,
  Image,
  Tag,
  LogOut,
  Menu,
  X,
  ChevronRight,
  ShieldCheck,
  KeyRound,
  SlidersHorizontal,
  Package,
} from "lucide-react";
import { useDispatch } from "react-redux";
import type { AppDispatch } from "../../store/store";
import { logoutThunk } from "../../store/authThunks";

const NAV = [
  { to: "/admin",              icon: LayoutDashboard, label: "Tổng quan",         end: true },
  { to: "/admin/users",        icon: Users,           label: "Quản lý người dùng" },
  { to: "/admin/sellers",      icon: Store,           label: "Quản lý bán hàng"   },
  { to: "/admin/products",     icon: Package,         label: "Duyệt sản phẩm"     },
  { to: "/admin/banners",      icon: Image,           label: "Quản lý banner"     },
  { to: "/admin/categories",   icon: Tag,             label: "Quản lý danh mục"   },
  { to: "/admin/options",      icon: SlidersHorizontal, label: "Phân loại hàng"   },
  { to: "/admin/roles",        icon: KeyRound,        label: "Quản lý roles"      },
  { to: "/admin/coupons",      icon: Tag,             label: "Quản lý coupon"     },
];

const base = "flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-semibold transition-all";
const active = "bg-red-600 text-white shadow-sm shadow-red-200";
const idle   = "text-slate-400 hover:bg-slate-800 hover:text-white";

function Sidebar({ onNavClick, onLogout }: { onNavClick: () => void; onLogout: () => void }) {
  return (
    <nav className="flex flex-col h-full py-4">
      {/* Brand */}
      <Link
        to="/admin"
        className="flex items-center gap-2.5 px-4 pb-5 border-b border-slate-800 mb-3"
      >
        <div className="w-8 h-8 bg-red-600 rounded-xl flex items-center justify-center">
          <ShieldCheck className="w-4 h-4 text-white" />
        </div>
        <span className="font-black text-white text-base tracking-tight">
          Sellico <span className="text-red-400 font-bold text-xs">ADMIN</span>
        </span>
      </Link>

      <div className="flex-1 px-2 space-y-0.5 overflow-y-auto">
        {NAV.map(({ to, icon: Icon, label, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            onClick={onNavClick}
            className={({ isActive }) => base + " " + (isActive ? active : idle)}
          >
            <Icon className="w-4 h-4 shrink-0" />
            {label}
          </NavLink>
        ))}
      </div>

      <div className="px-2 pt-3 border-t border-slate-800">
        <button
          onClick={onLogout}
          className={base + " w-full text-slate-400 hover:bg-red-900/40 hover:text-red-400"}
        >
          <LogOut className="w-4 h-4 shrink-0" />
          Đăng xuất
        </button>
      </div>
    </nav>
  );
}

export default function AdminLayout() {
  const [open, setOpen] = useState(false);
  const dispatch  = useDispatch<AppDispatch>();
  const navigate  = useNavigate();

  const handleLogout = async () => {
    await dispatch(logoutThunk());
    navigate("/login");
  };

  return (
    <div className="flex min-h-screen bg-slate-100 font-sans">
      {/* ── Desktop sidebar ──────────────────────────────────────────────── */}
      <aside className="hidden md:flex flex-col w-56 shrink-0 bg-slate-900 fixed inset-y-0 left-0 z-30">
        <Sidebar onNavClick={() => setOpen(false)} onLogout={handleLogout} />
      </aside>

      {/* ── Mobile sidebar ───────────────────────────────────────────────── */}
      {open && (
        <div className="fixed inset-0 z-50 md:hidden flex">
          <div className="absolute inset-0 bg-black/60" onClick={() => setOpen(false)} />
          <div className="relative w-56 bg-slate-900 shadow-2xl">
            <button
              onClick={() => setOpen(false)}
              className="absolute top-3 right-3 p-1.5 hover:bg-slate-800 rounded-lg"
            >
              <X className="w-4 h-4 text-slate-400" />
            </button>
            <Sidebar onNavClick={() => setOpen(false)} onLogout={handleLogout} />
          </div>
        </div>
      )}

      {/* ── Main area ────────────────────────────────────────────────────── */}
      <div className="flex-1 md:ml-56 flex flex-col min-h-screen">
        {/* Top bar */}
        <header className="bg-white border-b border-slate-200 px-4 h-14 flex items-center gap-3 sticky top-0 z-20 shadow-sm">
          <button
            className="md:hidden p-1.5 rounded-lg hover:bg-slate-100"
            onClick={() => setOpen(true)}
          >
            <Menu className="w-5 h-5 text-slate-600" />
          </button>
          {/* Breadcrumb placeholder */}
          <div className="flex items-center gap-1 text-xs text-slate-400 font-semibold">
            <ShieldCheck className="w-3.5 h-3.5 text-red-500" />
            <ChevronRight className="w-3 h-3" />
            <span className="text-slate-600">Admin</span>
          </div>
          <div className="ml-auto flex items-center gap-2">
            <span className="text-xs font-bold bg-red-100 text-red-700 px-2.5 py-1 rounded-full">
              Quản trị viên
            </span>
          </div>
        </header>

        <main className="flex-1 p-5 md:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
