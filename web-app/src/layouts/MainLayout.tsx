import { useState, useEffect, useCallback } from "react";
import { Outlet, Link, useNavigate, useLocation } from "react-router-dom";
import {
  ShoppingCart, User, Search, Store, LogOut, ChevronDown,
  HelpCircle, Bell, Package, SlidersHorizontal, X,
} from "lucide-react";
import { useSelector, useDispatch } from "react-redux";
import type { RootState, AppDispatch } from "../store/store";
import { logoutThunk } from "../store/authThunks";
import { getCartCount } from "../api/cartApi";
import { getActiveBanners, trackBannerClick, type PublicBannerResponse } from "../api/bannerApi";
import { getCategoryTree } from "../api/categoryApi";
import type { CategoryTreeResponse } from "../types/admin";

export default function MainLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch<AppDispatch>();
  const token = useSelector((s: RootState) => s.auth.accessToken);
  const unreadCount = useSelector((s: RootState) => s.notifications.unreadCount);
  const [search, setSearch] = useState("");
  const [cartCount, setCartCount] = useState(0);
  const [menuOpen, setMenuOpen] = useState(false);
  const [categories, setCategories] = useState<CategoryTreeResponse[]>([]);
  const [catOpen, setCatOpen] = useState(false);
  const [popupBanners, setPopupBanners] = useState<PublicBannerResponse[]>([]);
  const [showPopup, setShowPopup] = useState(false);
  const isSearchPage = location.pathname === "/search";

  // fetch cart count
  const refreshCartCount = useCallback(() => {
    if (!token) return;
    getCartCount().then(r => setCartCount(r.data.result ?? 0)).catch(() => {});
  }, [token]);

  useEffect(() => { refreshCartCount(); }, [refreshCartCount, location.pathname]);

  // listen for cart-updated events from other components
  useEffect(() => {
    const handler = () => refreshCartCount();
    window.addEventListener("cart-updated", handler);
    return () => window.removeEventListener("cart-updated", handler);
  }, [refreshCartCount]);

  // fetch categories for nav
  useEffect(() => {
    getCategoryTree().then(r => setCategories((r.data.result ?? []).slice(0, 10))).catch(() => {});
  }, []);

  // fetch popup banners (show once per session)
  useEffect(() => {
    const shown = sessionStorage.getItem("popup_banner_shown");
    if (shown) return;
    getActiveBanners("POPUP").then(r => {
      const list = r.data.result ?? [];
      if (list.length > 0) {
        setPopupBanners(list);
        setShowPopup(true);
        sessionStorage.setItem("popup_banner_shown", "1");
      }
    }).catch(() => {});
  }, []);

  const handleSearch = useCallback((e: React.FormEvent) => {
    e.preventDefault();
    if (search.trim()) navigate(`/search?q=${encodeURIComponent(search.trim())}`);
  }, [search, navigate]);

  const handleLogout = async () => {
    await dispatch(logoutThunk());
    setMenuOpen(false);
    navigate("/login");
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      {/* ───── Top bar (desktop only) ───── */}
      <div className="hidden md:block bg-gradient-to-r from-red-600 to-red-600 text-white text-xs">
        <div className="container mx-auto px-4 h-8 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link to="/seller" className="hover:text-red-200 flex items-center gap-1 font-medium">
              <Store className="w-3 h-3" /> Kênh người bán
            </Link>
            <span className="text-red-300">|</span>
            <Link to="/user/support" className="hover:text-red-200 flex items-center gap-1">
              <HelpCircle className="w-3 h-3" /> Hỗ trợ
            </Link>
          </div>
          <div className="flex items-center gap-4">
            {token ? (
              <>
                <Link to="/user/orders" className="hover:text-red-200 flex items-center gap-1">
                  <Package className="w-3 h-3" /> Đơn hàng
                </Link>
              </>
            ) : (
              <>
                <Link to="/register" className="hover:text-red-200 font-medium">Đăng ký</Link>
                <span className="text-red-300">|</span>
                <Link to="/login" className="hover:text-red-200 font-medium">Đăng nhập</Link>
              </>
            )}
          </div>
        </div>
      </div>

      {/* ───── Main header ───── */}
      <header className="bg-white shadow-sm sticky top-0 z-50">
        <div className="container mx-auto px-4">
          {/* Primary row - Desktop */}
          <div className="hidden md:flex h-16 items-center gap-6">
            {/* Logo */}
            <Link to="/" className="flex items-center gap-2 font-extrabold text-2xl text-red-600 shrink-0 tracking-tight">
              <Store className="w-7 h-7" />
              <span>Sellico</span>
            </Link>

            {/* Search */}
            <form onSubmit={handleSearch} className="flex flex-1 max-w-2xl relative">
              <input
                type="text"
                value={search}
                onChange={e => setSearch(e.target.value)}
                placeholder="Tìm kiếm sản phẩm, thương hiệu..."
                className="w-full pl-4 pr-24 py-2.5 border-2 border-red-500 rounded-lg focus:outline-none focus:border-red-600 text-sm"
              />
              <button type="submit" className="absolute right-0 top-0 bottom-0 px-5 bg-red-600 text-white rounded-r-lg hover:bg-red-700 transition-colors flex items-center gap-1.5 font-semibold text-sm">
                <Search className="w-4 h-4" />
                <span className="hidden lg:inline">Tìm kiếm</span>
              </button>
            </form>

            {/* Right actions */}
            <div className="flex items-center gap-5 shrink-0">
              {/* Cart */}
              <Link to="/user/notifications" className="relative flex flex-col items-center hover:text-red-600 transition-colors group">
                <Bell className="w-6 h-6" />
                <span className="text-[10px] font-semibold mt-0.5 text-gray-500 group-hover:text-red-600">Thông báo</span>
                {unreadCount > 0 && (
                  <span className="absolute -top-1.5 -right-2.5 bg-red-500 text-white text-[10px] min-w-[18px] h-[18px] rounded-full flex items-center justify-center font-bold px-1">
                    {unreadCount > 99 ? "99+" : unreadCount}
                  </span>
                )}
              </Link>

              <Link to="/cart" className="relative flex flex-col items-center hover:text-red-600 transition-colors group">
                <ShoppingCart className="w-6 h-6" />
                <span className="text-[10px] font-semibold mt-0.5 text-gray-500 group-hover:text-red-600">Giỏ hàng</span>
                {cartCount > 0 && (
                  <span className="absolute -top-1.5 -right-2.5 bg-red-500 text-white text-[10px] min-w-[18px] h-[18px] rounded-full flex items-center justify-center font-bold px-1">
                    {cartCount > 99 ? "99+" : cartCount}
                  </span>
                )}
              </Link>

              {/* Account */}
              {token ? (
                <div className="relative">
                  <button onClick={() => setMenuOpen(!menuOpen)} className="flex flex-col items-center hover:text-red-600 transition-colors group">
                    <User className="w-6 h-6" />
                    <span className="text-[10px] font-semibold mt-0.5 text-gray-500 group-hover:text-red-600 flex items-center gap-0.5">
                      Tài khoản <ChevronDown className="w-2.5 h-2.5" />
                    </span>
                  </button>
                  {menuOpen && (
                    <>
                      <div className="fixed inset-0 z-40" onClick={() => setMenuOpen(false)} />
                      <div className="absolute right-0 top-full mt-2 w-52 bg-white rounded-xl shadow-2xl border border-slate-100 py-2 z-50">
                        <Link to="/user/profile" onClick={() => setMenuOpen(false)} className="block px-4 py-2.5 text-sm hover:bg-slate-50 font-semibold text-slate-700">Tài khoản của tôi</Link>
                        <Link to="/user/orders" onClick={() => setMenuOpen(false)} className="block px-4 py-2.5 text-sm hover:bg-slate-50 font-semibold text-slate-700">Đơn hàng</Link>
                        <Link to="/user/vouchers" onClick={() => setMenuOpen(false)} className="block px-4 py-2.5 text-sm hover:bg-slate-50 font-semibold text-slate-700">Voucher của tôi</Link>
                        <Link to="/user/notifications" onClick={() => setMenuOpen(false)} className="block px-4 py-2.5 text-sm hover:bg-slate-50 font-semibold text-slate-700">Thông báo</Link>
                        <hr className="my-1 border-slate-100" />
                        <button onClick={handleLogout} className="w-full text-left px-4 py-2.5 text-sm hover:bg-red-50 font-semibold text-red-600 flex items-center gap-2">
                          <LogOut className="w-3.5 h-3.5" />Đăng xuất
                        </button>
                      </div>
                    </>
                  )}
                </div>
              ) : (
                <Link to="/login" className="flex flex-col items-center hover:text-red-600 transition-colors group">
                  <User className="w-6 h-6" />
                  <span className="text-[10px] font-semibold mt-0.5 text-gray-500 group-hover:text-red-600">Đăng nhập</span>
                </Link>
              )}
            </div>
          </div>

          {/* ───── Category navigation bar (desktop only) ───── */}
          <nav className="hidden md:flex h-10 items-center gap-1 -mb-px overflow-x-auto scrollbar-hide text-sm border-t border-gray-100">
            <div className="relative"
              onMouseEnter={() => setCatOpen(true)}
              onMouseLeave={() => setCatOpen(false)}>
              <button className="flex items-center gap-1 px-3 py-1.5 font-bold text-red-600 hover:bg-red-50 rounded-lg transition-colors">
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" /></svg>
                Danh mục
                <ChevronDown className="w-3 h-3" />
              </button>
              {catOpen && categories.length > 0 && (
                <div className="absolute left-0 top-full bg-white rounded-xl shadow-2xl border border-slate-100 py-2 min-w-[220px] z-50">
                  {categories.map(cat => (
                    <Link key={cat.id} to={`/search?category=${cat.id}`}
                      onClick={() => setCatOpen(false)}
                      className="flex items-center gap-2.5 px-4 py-2 text-sm hover:bg-red-50 text-slate-700 hover:text-red-600 font-medium transition-colors">
                      {cat.iconUrl ? (
                        <img src={cat.iconUrl} alt="" className="w-5 h-5 object-contain" />
                      ) : (
                        <div className="w-5 h-5 bg-red-100 rounded flex items-center justify-center text-red-500 text-xs">●</div>
                      )}
                      {cat.name}
                    </Link>
                  ))}
                  <hr className="my-1 border-slate-100" />
                  <Link to="/search" onClick={() => setCatOpen(false)}
                    className="block px-4 py-2 text-sm text-red-600 font-bold hover:bg-red-50 transition-colors">
                    Xem tất cả &rarr;
                  </Link>
                </div>
              )}
            </div>

            {categories.slice(0, 6).map(cat => (
              <Link key={cat.id} to={`/search?category=${cat.id}`}
                className="px-3 py-1.5 text-slate-600 hover:text-red-600 hover:bg-red-50 rounded-lg font-medium whitespace-nowrap transition-colors">
                {cat.name}
              </Link>
            ))}
          </nav>
        </div>
      </header>

      {/* ───── Mobile header ───── */}
      <div className="md:hidden bg-white border-b sticky top-0 z-50 px-3 py-2">
        <div className="flex items-center gap-2">
          <Link to="/" className="text-red-600 shrink-0 p-1">
            <Store className="w-6 h-6" />
          </Link>
          <form onSubmit={handleSearch} className="flex-1 relative">
            <input
              type="text"
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="Tìm kiếm..."
              className="w-full pl-4 pr-10 py-2 border border-red-400 rounded-lg focus:outline-none focus:ring-1 focus:ring-red-500 text-sm"
            />
            <button type="submit" className="absolute right-3 top-2.5 text-red-500">
              <Search className="w-4 h-4" />
            </button>
          </form>
          {isSearchPage ? (
            <button
              type="button"
              onClick={() => window.dispatchEvent(new Event("open-search-filters"))}
              className="p-2 hover:text-red-600 transition-colors"
              aria-label="Mở bộ lọc"
            >
              <SlidersHorizontal className="w-5 h-5" />
            </button>
          ) : (
            <>
              <Link to="/user/notifications" className="relative p-2 hover:text-red-600 transition-colors">
                <Bell className="w-5 h-5" />
                {unreadCount > 0 && (
                  <span className="absolute -top-0.5 -right-0.5 bg-red-500 text-white text-[9px] min-w-[16px] h-[16px] rounded-full flex items-center justify-center font-bold px-0.5">
                    {unreadCount > 99 ? "99+" : unreadCount}
                  </span>
                )}
              </Link>
              <Link to="/cart" className="relative p-2 hover:text-red-600 transition-colors">
                <ShoppingCart className="w-5 h-5" />
                {cartCount > 0 && (
                  <span className="absolute -top-0.5 -right-0.5 bg-red-500 text-white text-[9px] min-w-[16px] h-[16px] rounded-full flex items-center justify-center font-bold px-0.5">
                    {cartCount > 99 ? "99+" : cartCount}
                  </span>
                )}
              </Link>
              {token ? (
                <div className="relative">
                  <button onClick={() => setMenuOpen(!menuOpen)} className="p-2 hover:text-red-600 transition-colors">
                    <User className="w-5 h-5" />
                  </button>
                  {menuOpen && (
                    <>
                      <div className="fixed inset-0 z-40" onClick={() => setMenuOpen(false)} />
                      <div className="absolute right-0 top-full mt-1 w-48 bg-white rounded-xl shadow-2xl border border-slate-100 py-2 z-50">
                        <Link to="/user/profile" onClick={() => setMenuOpen(false)} className="block px-4 py-2.5 text-sm hover:bg-slate-50 font-semibold text-slate-700">Tài khoản</Link>
                        <Link to="/user/orders" onClick={() => setMenuOpen(false)} className="block px-4 py-2.5 text-sm hover:bg-slate-50 font-semibold text-slate-700">Đơn hàng</Link>
                        <Link to="/user/notifications" onClick={() => setMenuOpen(false)} className="block px-4 py-2.5 text-sm hover:bg-slate-50 font-semibold text-slate-700">Thông báo</Link>
                        <hr className="my-1 border-slate-100" />
                        <button onClick={handleLogout} className="w-full text-left px-4 py-2.5 text-sm hover:bg-red-50 font-semibold text-red-600 flex items-center gap-2">
                          <LogOut className="w-3.5 h-3.5" />Đăng xuất
                        </button>
                      </div>
                    </>
                  )}
                </div>
              ) : (
                <Link to="/login" className="p-2 hover:text-red-600 transition-colors">
                  <User className="w-5 h-5" />
                </Link>
              )}
            </>
          )}
        </div>
      </div>

      <main className="flex-1 container mx-auto px-0 sm:px-4 py-4 sm:py-6">
        <Outlet />
      </main>

      {/* ───── Footer ───── */}
      <footer className="bg-white border-t">
        <div className="container mx-auto px-4 py-10">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
            <div>
              <Link to="/" className="flex items-center gap-2 font-extrabold text-xl text-red-600 mb-3">
                <Store className="w-6 h-6" /> Sellico
              </Link>
              <p className="text-sm text-gray-500">Nền tảng thương mại điện tử hàng đầu Việt Nam.</p>
            </div>
            <div>
              <h4 className="font-bold text-sm text-slate-900 mb-3 uppercase tracking-wider">Chăm sóc khách hàng</h4>
              <div className="space-y-2 text-sm text-slate-500">
                <Link to="/user/support" className="block hover:text-red-600">Trung tâm trợ giúp</Link>
                <Link to="/user/orders" className="block hover:text-red-600">Theo dõi đơn hàng</Link>
                <span className="block">Hotline: 1900-xxxx</span>
              </div>
            </div>
            <div>
              <h4 className="font-bold text-sm text-slate-900 mb-3 uppercase tracking-wider">Về Sellico</h4>
              <div className="space-y-2 text-sm text-slate-500">
                <Link to="/user/terms" className="block hover:text-red-600">Điều khoản sử dụng</Link>
                <span className="block">Chính sách bảo mật</span>
                <span className="block">Tuyển dụng</span>
              </div>
            </div>
            <div>
              <h4 className="font-bold text-sm text-slate-900 mb-3 uppercase tracking-wider">Bán hàng cùng Sellico</h4>
              <div className="space-y-2 text-sm text-slate-500">
                <Link to="/seller" className="block hover:text-red-600">Đăng ký bán hàng</Link>
                <span className="block">Hoa hồng bán hàng</span>
                <span className="block">Quy định đối với người bán</span>
              </div>
            </div>
          </div>
          <hr className="my-6 border-gray-100" />
          <p className="text-center text-gray-400 text-sm">&copy; 2026 Sellico E-Commerce. All rights reserved.</p>
        </div>
      </footer>

      {/* ───── Popup Banner Modal ───── */}
      {showPopup && popupBanners.length > 0 && (
        <>
          <div className="fixed inset-0 bg-black/50 z-[100] flex items-center justify-center p-4" onClick={() => setShowPopup(false)}>
            <div className="relative max-w-lg w-full animate-in zoom-in-95 duration-300" onClick={e => e.stopPropagation()}>
              <button onClick={() => setShowPopup(false)}
                className="absolute -top-3 -right-3 z-10 bg-white rounded-full shadow-lg p-1.5 hover:bg-gray-100 transition-colors">
                <X className="w-5 h-5 text-gray-600" />
              </button>
              {popupBanners[0].linkUrl ? (
                <Link to={popupBanners[0].linkUrl}
                  onClick={() => { trackBannerClick(popupBanners[0].id); setShowPopup(false); }}>
                  <img src={popupBanners[0].imageUrl} alt={popupBanners[0].title}
                    className="w-full rounded-2xl shadow-2xl" />
                </Link>
              ) : (
                <img src={popupBanners[0].imageUrl} alt={popupBanners[0].title}
                  className="w-full rounded-2xl shadow-2xl" />
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
