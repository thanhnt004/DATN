import { useState, useEffect } from "react";
import { NavLink, Outlet, useNavigate, Link, useLocation } from "react-router-dom";
import {
  Bell,
  User,
  MapPin,
  Settings,
  ShoppingBag,
  Ticket,
  FileText,
  MessageSquare,
  LogOut,
  ChevronDown,
  ChevronRight,
  Store,
  ShoppingCart,
  Search,
  Menu,
  X,
  Heart,
  Sparkles,
} from "lucide-react";
import { Toaster, toast } from "react-hot-toast";
import { jwtDecode } from "jwt-decode";
import { useDispatch, useSelector } from "react-redux";
import type { AppDispatch, RootState } from "../../store/store";
import { logoutThunk } from "../../store/authThunks";
import { clearUnreadCount } from "../../store/notificationSlice";
import { TYPE_ICON } from "./NotificationPages";

const ACCOUNT_CHILDREN = [
  { to: "/user/profile", icon: User, label: "Hồ sơ cá nhân" },
  { to: "/user/addresses", icon: MapPin, label: "Sổ địa chỉ" },
  { to: "/user/notification-settings", icon: Settings, label: "Cài đặt thông báo" },
];

const TOP_NAV = [
  { to: "/user/notifications", icon: Bell, label: "Thông báo" },
];

const SELLER_NAV = {
  to: "/user/sell",
  icon: Store,
  label: "Đăng ký cửa hàng",
};

const BOTTOM_NAV = [
  { to: "/user/orders", icon: ShoppingBag, label: "Lịch sử mua hàng" },
  { to: "/user/following", icon: Heart, label: "Theo dõi" },
  { to: "/user/vouchers", icon: Ticket, label: "Kho voucher" },
  { to: "/user/messages", icon: MessageSquare, label: "Tin nhắn" },
  { to: "/user/ai-chat", icon: Sparkles, label: "AI Tư vấn sản phẩm" },
  { to: "/user/terms", icon: FileText, label: "Điều khoản sử dụng" },
  { to: "/user/support", icon: MessageSquare, label: "Góp ý - Phản hồi - Hỗ trợ" },
];

const navBase =
  "flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-semibold transition-all ";
const navActive = "bg-red-50 text-red-600";
const navIdle = "text-gray-600 hover:bg-gray-100 hover:text-gray-900";
const subNavActive = "bg-red-50 text-red-600 font-bold";
const subNavIdle = "text-gray-500 hover:bg-gray-100 hover:text-gray-800";

export default function UserLayout() {
  const [accountOpen, setAccountOpen] = useState(true);
  const [mobileOpen, setMobileOpen] = useState(false);
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();
  const location = useLocation();
  const isAuthenticated = useSelector((s: RootState) => s.auth.isAuthenticated);
  const unreadCount = useSelector((s: RootState) => s.notifications.unreadCount);

  const handleLogout = async () => {
    await dispatch(logoutThunk());
    navigate("/login");
  };

  useEffect(() => {
    if (location.pathname === '/user/notifications') {
      dispatch(clearUnreadCount());
    }
  }, [location, dispatch]);

  const SidebarContent = () => (
    <nav className="flex flex-col h-full">
      {/* Brand */}
      <Link
        to="/"
        className="flex items-center gap-2 font-black text-xl text-red-600 tracking-tighter px-4 py-4 border-b border-gray-100 mb-2"
      >
        <Store className="w-5 h-5" />
        <span>SELLICO</span>
      </Link>

      <div className="flex-1 overflow-y-auto space-y-0.5 px-2 pb-4">
        {/* Top nav */}
        {TOP_NAV.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            onClick={() => setMobileOpen(false)}
            className={({ isActive }) =>
              navBase + (isActive ? navActive : navIdle)
            }
          >
            <Icon className="w-4 h-4 shrink-0" />
            {label}
          </NavLink>
        ))}

        {/* Account accordion */}
        <div>
          <button
            onClick={() => setAccountOpen((o) => !o)}
            className={
              navBase +
              "w-full justify-between " +
              (accountOpen ? "text-gray-900" : navIdle)
            }
          >
            <span className="flex items-center gap-3">
              <User className="w-4 h-4 shrink-0" />
              Tài khoản
            </span>
            {accountOpen ? (
              <ChevronDown className="w-3.5 h-3.5" />
            ) : (
              <ChevronRight className="w-3.5 h-3.5" />
            )}
          </button>

          {accountOpen && (
            <div className="ml-4 mt-0.5 space-y-0.5 border-l-2 border-gray-100 pl-3">
              {ACCOUNT_CHILDREN.map(({ to, icon: Icon, label }) => (
                <NavLink
                  key={to}
                  to={to}
                  onClick={() => setMobileOpen(false)}
                  className={({ isActive }) =>
                    navBase + "text-[13px] py-2 " + (isActive ? subNavActive : subNavIdle)
                  }
                >
                  <Icon className="w-3.5 h-3.5 shrink-0" />
                  {label}
                </NavLink>
              ))}
            </div>
          )}
        </div>

        {/* Seller registration — prominent button */}
        <div className="mx-2 my-1">
          <NavLink
            to={SELLER_NAV.to}
            onClick={() => setMobileOpen(false)}
            className={({ isActive }) =>
              `flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-semibold transition-all border ${
                isActive
                  ? "bg-red-600 text-white border-red-600"
                  : "bg-red-50 text-red-600 border-red-200 hover:bg-red-100 hover:border-red-300"
              }`
            }
          >
            <SELLER_NAV.icon className="w-4 h-4 shrink-0" />
            {SELLER_NAV.label}
          </NavLink>
        </div>

        <div className="mx-2 border-t border-gray-100 my-1" />

        {/* Bottom nav */}
        {BOTTOM_NAV.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            onClick={() => setMobileOpen(false)}
            className={({ isActive }) =>
              navBase + (isActive ? navActive : navIdle)
            }
          >
            <Icon className="w-4 h-4 shrink-0" />
            {label}
          </NavLink>
        ))}

        <div className="border-t border-gray-100 pt-2 mt-2">
          <button
            onClick={handleLogout}
            className={navBase + "w-full text-gray-500 hover:bg-red-50 hover:text-red-600"}
          >
            <LogOut className="w-4 h-4 shrink-0" />
            Đăng xuất
          </button>
        </div>
      </div>
    </nav>
  );

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      {/* ── Top header ──────────────────────────────────────────────── */}
      <Toaster position="top-right" />
      <header className="bg-white shadow-sm sticky top-0 z-50 border-b border-gray-100">
        <div className="max-w-7xl mx-auto px-4 h-14 flex items-center justify-between gap-4">
          {/* Mobile hamburger */}
          <button
            className="md:hidden p-1.5 rounded-lg hover:bg-gray-100"
            onClick={() => setMobileOpen(true)}
          >
            <Menu className="w-5 h-5" />
          </button>

          <Link
            to="/"
            className="hidden md:flex items-center gap-2 font-black text-lg text-red-600 tracking-tighter"
          >
            <Store className="w-5 h-5" />
            SELLICO
          </Link>

          <div className="flex-1 max-w-md mx-auto md:mx-8 relative hidden sm:block">
            <input
              placeholder="Tìm kiếm sản phẩm..."
              className="w-full pl-4 pr-10 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:border-red-400 bg-gray-50"
            />
            <Search className="absolute right-3 top-2.5 w-4 h-4 text-gray-400" />
          </div>

          <div className="flex items-center gap-3">
            <Link to="/user/notifications" className="relative p-2 hover:bg-gray-100 rounded-xl transition-colors">
              <Bell className="w-5 h-5 text-gray-600" />
              {unreadCount > 0 && (
                <span className="absolute -top-0.5 -right-0.5 w-4 h-4 bg-red-500 text-white text-[9px] font-bold rounded-full flex items-center justify-center animate-pulse">
                  {unreadCount > 9 ? '9+' : unreadCount}
                </span>
              )}
            </Link>
            <Link
              to="/cart"
              className="relative p-2 hover:bg-gray-100 rounded-xl transition-colors"
            >
              <ShoppingCart className="w-5 h-5 text-gray-600" />
              <span className="absolute -top-0.5 -right-0.5 w-4 h-4 bg-red-500 text-white text-[9px] font-bold rounded-full flex items-center justify-center">
                0
              </span>
            </Link>
            {!isAuthenticated && (
              <Link
                to="/login"
                className="text-sm font-bold text-red-600 px-3 py-1.5 border border-red-200 rounded-xl hover:bg-red-50"
              >
                Đăng nhập
              </Link>
            )}
          </div>
        </div>
      </header>

      <div className="flex flex-1 max-w-7xl mx-auto w-full px-4 py-6 gap-5">
        {/* ── Desktop sidebar ──────────────────────────────────────── */}
        <aside className="hidden md:flex flex-col w-60 shrink-0">
          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 h-fit sticky top-[78px] overflow-hidden">
            <SidebarContent />
          </div>
        </aside>

        {/* ── Mobile sidebar overlay ───────────────────────────────── */}
        {mobileOpen && (
          <div className="fixed inset-0 z-50 md:hidden">
            <div
              className="absolute inset-0 bg-black/40 backdrop-blur-sm"
              onClick={() => setMobileOpen(false)}
            />
            <div className="absolute left-0 top-0 bottom-0 w-72 bg-white shadow-2xl">
              <button
                className="absolute top-3.5 right-3 p-1.5 hover:bg-gray-100 rounded-lg"
                onClick={() => setMobileOpen(false)}
              >
                <X className="w-4 h-4" />
              </button>
              <SidebarContent />
            </div>
          </div>
        )}

        {/* ── Main content ─────────────────────────────────────────── */}
        <main className="flex-1 min-w-0">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
