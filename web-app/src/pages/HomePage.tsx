import { useEffect, useState, useRef, useCallback, useLayoutEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Tag, ChevronLeft, ChevronRight, Loader2, Zap, Flame } from "lucide-react";
import ProductCard, { type Product } from "../components/ProductCard";
import { getActiveBanners, trackBannerClick, trackBannerView, type PublicBannerResponse } from "../api/bannerApi";
import { searchProducts } from "../api/searchApi";
import { getCategoryTree } from "../api/categoryApi";
import { getProducts } from "../api/productApi";
import type { CategoryTreeResponse } from "../types/admin";
import type { SearchProductResponse } from "../types/search";
import type { ProductSummaryResponse, ProductResponse } from "../types/product";

// Hook for lazy loading sections
function useLazyLoad(elementRef: React.RefObject<HTMLDivElement | null>): boolean {
  const [isVisible, setIsVisible] = useState(false);

  useLayoutEffect(() => {
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        setIsVisible(true);
        observer.disconnect();
      }
    }, { rootMargin: "100px" });

    if (elementRef.current) {
      observer.observe(elementRef.current);
    }
    return () => observer.disconnect();
  }, [elementRef]);

  return isVisible;
}

function toProductCard(p: SearchProductResponse | ProductSummaryResponse | ProductResponse): Product {
  let img: string | null = null;
  if ("thumbnailUrl" in p && p.thumbnailUrl) {
    img = p.thumbnailUrl as string;
  } else if ("primaryImageUrl" in p && p.primaryImageUrl) {
    img = p.primaryImageUrl as string;
  } else if ("images" in p && Array.isArray(p.images) && p.images.length > 0) {
    const primary = p.images.find((i: { isPrimary?: boolean }) => i.isPrimary);
    img = primary ? primary.url : p.images[0].url;
  }
  return {
    id: p.id as unknown as number,
    name: p.name,
    image: img || "https://placehold.co/400x400?text=No+Image",
    price: p.minPrice,
    originalPrice: p.maxPrice > p.minPrice ? p.maxPrice : undefined,
    rating: p.ratingAvg ?? 0,
    reviewCount: p.ratingCount ?? 0,
    sold: p.soldCount ?? 0,
  };
}

/* ═══════════════════════════════ Banner Carousel ═══════════════════════════════ */
function BannerCarousel({ banners, height = "h-[340px] sm:h-[420px]" }: { banners: PublicBannerResponse[]; height?: string }) {
  const [idx, setIdx] = useState(0);
  const timerRef = useRef<ReturnType<typeof setInterval>>(undefined);

  const resetTimer = useCallback(() => {
    if (timerRef.current) clearInterval(timerRef.current);
    if (banners.length > 1) {
      timerRef.current = setInterval(() => setIdx(i => (i + 1) % banners.length), 5000);
    }
  }, [banners.length]);

  useEffect(() => { resetTimer(); return () => clearInterval(timerRef.current); }, [resetTimer]);

  useEffect(() => {
    if (banners[idx]) trackBannerView(banners[idx].id).catch(() => {});
  }, [idx, banners]);

  if (banners.length === 0) return null;

  const prev = () => { setIdx(i => (i - 1 + banners.length) % banners.length); resetTimer(); };
  const next = () => { setIdx(i => (i + 1) % banners.length); resetTimer(); };

  return (
    <div className={`relative sm:rounded-2xl overflow-hidden ${height} group`}>
      {banners.map((b, i) => (
        <div key={b.id} className={`absolute inset-0 transition-opacity duration-700 ${i === idx ? "opacity-100 z-10" : "opacity-0 z-0"}`}>
          <img src={b.imageUrl} alt={b.title} className="w-full h-full object-cover" />
          {b.linkUrl && (
            <Link to={b.linkUrl} className="absolute inset-0" onClick={() => trackBannerClick(b.id)} />
          )}
        </div>
      ))}
      {banners.length > 1 && (
        <>
          <button onClick={prev}
            className="absolute left-3 top-1/2 -translate-y-1/2 z-20 bg-black/30 hover:bg-black/50 text-white p-2 rounded-full opacity-0 group-hover:opacity-100 transition-all">
            <ChevronLeft className="w-5 h-5" />
          </button>
          <button onClick={next}
            className="absolute right-3 top-1/2 -translate-y-1/2 z-20 bg-black/30 hover:bg-black/50 text-white p-2 rounded-full opacity-0 group-hover:opacity-100 transition-all">
            <ChevronRight className="w-5 h-5" />
          </button>
          <div className="absolute bottom-3 left-1/2 -translate-x-1/2 z-20 flex gap-1.5">
            {banners.map((_, i) => (
              <button key={i} onClick={() => { setIdx(i); resetTimer(); }}
                className={`w-2 h-2 rounded-full transition-all ${i === idx ? "bg-white w-5" : "bg-white/50"}`} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}

/* ═══════════════════════════════ Banner Grid ═══════════════════════════════ */
function BannerGrid({ banners }: { banners: PublicBannerResponse[] }) {
  if (banners.length === 0) return null;
  const cols = banners.length >= 4 ? "grid-cols-2 md:grid-cols-4" : banners.length === 3 ? "grid-cols-3" : banners.length === 2 ? "grid-cols-2" : "grid-cols-1";
  return (
    <div className={`grid ${cols} gap-2 sm:gap-3 px-2 sm:px-0`}>
      {banners.map(b => (
        <div key={b.id} className="relative rounded-xl overflow-hidden group cursor-pointer h-28 sm:h-40">
          {b.linkUrl ? (
            <Link to={b.linkUrl} onClick={() => trackBannerClick(b.id)} className="block w-full h-full">
              <img src={b.imageUrl} alt={b.title} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" />
            </Link>
          ) : (
            <img src={b.imageUrl} alt={b.title} className="w-full h-full object-cover" />
          )}
        </div>
      ))}
    </div>
  );
}

/* ═══════════════════════════════ Section Header ═══════════════════════════════ */
function SectionHeader({ icon, title, subtitle, linkText, linkTo, accent = "blue" }: {
  icon?: React.ReactNode; title: string; subtitle?: string; linkText?: string; linkTo?: string; accent?: "blue" | "red";
}) {
  const accentMap = { blue: "text-blue-600", red: "text-red-600" };
  return (
    <div className="flex items-center justify-between mb-4 sm:mb-6 px-2 sm:px-0">
      <div className="flex items-center gap-2 sm:gap-3">
        {icon && <div className={`${accentMap[accent]}`}>{icon}</div>}
        <div>
          <h2 className="text-lg sm:text-2xl font-black text-slate-900 tracking-tight">{title}</h2>
          {subtitle && <p className="text-xs sm:text-sm text-slate-500 mt-0.5 hidden sm:block">{subtitle}</p>}
        </div>
      </div>
      {linkText && linkTo && (
        <Link to={linkTo} className={`${accentMap[accent]} font-bold text-xs sm:text-sm hover:underline uppercase tracking-wider`}>{linkText} →</Link>
      )}
    </div>
  );
}

/* ═══════════════ Sidebar Banners (mobile: 2-col grid, last odd = full) ═══════════════ */
function SidebarBannersMobile({ banners }: { banners: PublicBannerResponse[] }) {
  if (banners.length === 0) return null;
  const isOdd = banners.length % 2 !== 0;
  return (
    <div className="grid grid-cols-2 gap-2 px-2 mt-2">
      {banners.map((b, i) => {
        const isLast = i === banners.length - 1;
        const spanFull = isOdd && isLast;
        return (
          <div key={b.id} className={`relative rounded-xl overflow-hidden ${spanFull ? "col-span-2 aspect-[5/2]" : "aspect-[4/3]"}`}>
            {b.linkUrl ? (
              <Link to={b.linkUrl} onClick={() => trackBannerClick(b.id)} className="block w-full h-full">
                <img src={b.imageUrl} alt={b.title} className="w-full h-full object-cover" />
              </Link>
            ) : (
              <img src={b.imageUrl} alt={b.title} className="w-full h-full object-cover" />
            )}
          </div>
        );
      })}
    </div>
  );
}

/* ═══════════════════════════════ HOMEPAGE ═══════════════════════════════ */
export default function HomePage() {
  const navigate = useNavigate();
  const bestSellingScrollRef = useRef<HTMLDivElement>(null);
  const bestSellingRef = useRef<HTMLDivElement>(null);
  const newestRef = useRef<HTMLDivElement>(null);

  const isBestSellingVisible = useLazyLoad(bestSellingRef);
  const isNewestVisible = useLazyLoad(newestRef);

  const [heroBanners, setHeroBanners] = useState<PublicBannerResponse[]>([]);
  const [sidebarBanners, setSidebarBanners] = useState<PublicBannerResponse[]>([]);
  const [flashSaleBanners, setFlashSaleBanners] = useState<PublicBannerResponse[]>([]);
  const [midBanners, setMidBanners] = useState<PublicBannerResponse[]>([]);
  const [bottomBanners, setBottomBanners] = useState<PublicBannerResponse[]>([]);
  const [bestSelling, setBestSelling] = useState<Product[]>([]);
  const [newest, setNewest] = useState<Product[]>([]);
  const [categories, setCategories] = useState<CategoryTreeResponse[]>([]);
  const [catPage, setCatPage] = useState(0);
  const [bestSellingLoading, setBestSellingLoading] = useState(false);
  const [newestLoading, setNewestLoading] = useState(false);
  const [catLoading, setCatLoading] = useState(true);

  
  useEffect(() => {
    const loadCritical = async () => {
      try {
        // Load banners in parallel (Critical load)
        const [heroRes, sideRes, flashRes, midRes, bottomRes] = await Promise.allSettled([
          getActiveBanners("HOME_HERO"),
          getActiveBanners("SIDEBAR"),
          getActiveBanners("FLASH_SALE"),
          getActiveBanners("HOME_MID"),
          getActiveBanners("HOME_BOTTOM"),
        ]);

        if (heroRes.status === "fulfilled") setHeroBanners(heroRes.value.data.result ?? []);
        if (sideRes.status === "fulfilled") setSidebarBanners(sideRes.value.data.result ?? []);
        if (flashRes.status === "fulfilled") setFlashSaleBanners(flashRes.value.data.result ?? []);
        if (midRes.status === "fulfilled") setMidBanners(midRes.value.data.result ?? []);
        if (bottomRes.status === "fulfilled") setBottomBanners(bottomRes.value.data.result ?? []);
      } catch { /* ignore */ }
    };
    loadCritical();
  }, []);

  // Fetch categories independently
  useEffect(() => {
    setCatLoading(true);
    getCategoryTree()
      .then(res => setCategories(res.data.result ?? []))
      .catch(err => console.error("Error loading categories:", err))
      .finally(() => setCatLoading(false));
  }, []);

  // Lazy load Sản phẩm bán chạy
  useEffect(() => {
    if (isBestSellingVisible && bestSelling.length === 0 && !bestSellingLoading) {
      setBestSellingLoading(true);
      searchProducts({ sortBy: "best_selling", size: 15 })
        .then(res => {
          const items = res.data.result;
          const list = items && typeof items === "object" && "content" in items ? (items as { content: SearchProductResponse[] }).content : [];
          setBestSelling(list.map(toProductCard));
        })
        .catch(() => {})
        .finally(() => setBestSellingLoading(false));
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isBestSellingVisible]);

  // Lazy load Sản phẩm mới nhất
  useEffect(() => {
    if (isNewestVisible && newest.length === 0 && !newestLoading) {
      setNewestLoading(true);
      getProducts({ page: 0, size: 20, sortBy: "createdAt", sortDirection: "DESC" })
        .then(res => {
          const items = res.data.result;
          const list = items && typeof items === "object" && "content" in items ? (items as { content: ProductSummaryResponse[] }).content : [];
          setNewest(list.map(toProductCard));
        })
        .catch(() => {})
        .finally(() => setNewestLoading(false));
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isNewestVisible]);

  const handleAddToCart = async (p: Product) => navigate(`/product/${p.id}`);
  const handleViewDetail = (p: Product) => navigate(`/product/${p.id}`);

  const scrollBestSelling = (dir: "left" | "right") => {
    const el = bestSellingScrollRef.current;
    if (!el) return;
    const scrollAmount = el.clientWidth * 0.8;
    el.scrollBy({ left: dir === "left" ? -scrollAmount : scrollAmount, behavior: "smooth" });
  };

  return (
    <div className="space-y-5 sm:space-y-8">
      {/* ═══ 1. Banner Zone: HOME_HERO + SIDEBAR ═══ */}
      {/* Desktop: grid with carousel left + sidebar right */}
      <section className="hidden sm:grid grid-cols-1 lg:grid-cols-4 gap-3">
        <div className={sidebarBanners.length > 0 ? "lg:col-span-3" : "lg:col-span-4"}>
          {heroBanners.length > 0 ? (
            <BannerCarousel banners={heroBanners} height="h-[280px] sm:h-[380px]" />
          ) : (
            <div className="bg-gradient-to-br from-red-600 via-red-600 to-purple-700 text-white rounded-2xl overflow-hidden relative h-[280px] sm:h-[380px] flex items-center px-8 sm:px-12">
              <div className="max-w-xl relative z-10">
                <h1 className="text-3xl sm:text-5xl font-extrabold leading-tight mb-4">Khám phá sản phẩm tuyệt vời</h1>
                <p className="text-base sm:text-lg text-red-100 mb-8 max-w-md">Mua sắm các sản phẩm công nghệ, thời trang và gia dụng mới nhất với giá tốt nhất.</p>
                <div className="flex flex-wrap gap-3">
                  <Link to="/search" className="bg-white text-red-700 px-6 py-3 rounded-xl font-bold hover:bg-red-50 transition-colors shadow-lg">Mua ngay</Link>
                  <Link to="/search?sortBy=best_selling" className="bg-white/10 backdrop-blur border border-white/20 text-white px-6 py-3 rounded-xl font-bold hover:bg-white/20 transition-colors">Xem ưu đãi</Link>
                </div>
              </div>
              <div className="absolute right-0 top-0 w-72 h-72 bg-white/5 rounded-full -translate-y-1/4 translate-x-1/4" />
              <div className="absolute right-24 bottom-0 w-48 h-48 bg-white/5 rounded-full translate-y-1/3" />
            </div>
          )}
        </div>
        {sidebarBanners.length > 0 && (
          <div className="lg:col-span-1 flex flex-row lg:flex-col gap-3 lg:h-[380px]">
            {sidebarBanners.slice(0, 2).map(b => (
              <div key={b.id} className="relative rounded-2xl overflow-hidden flex-1">
                {b.linkUrl ? (
                  <Link to={b.linkUrl} onClick={() => trackBannerClick(b.id)} className="block w-full h-full">
                    <img src={b.imageUrl} alt={b.title} className="w-full h-full object-cover hover:scale-105 transition-transform duration-300" />
                  </Link>
                ) : (
                  <img src={b.imageUrl} alt={b.title} className="w-full h-full object-cover" />
                )}
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Mobile: Hero carousel on top, sidebar as 2-col grid below */}
      <section className="sm:hidden">
        {heroBanners.length > 0 ? (
          <BannerCarousel banners={heroBanners} height="h-[200px]" />
        ) : (
          <div className="bg-gradient-to-br from-red-600 to-purple-700 text-white h-[200px] flex items-center px-6">
            <div>
              <h1 className="text-2xl font-extrabold leading-tight mb-2">Khám phá sản phẩm tuyệt vời</h1>
              <Link to="/search" className="inline-block bg-white text-red-700 px-4 py-2 rounded-lg font-bold text-sm">Mua ngay</Link>
            </div>
          </div>
        )}
        <SidebarBannersMobile banners={sidebarBanners} />
      </section>

      {/* ═══ 2. Flash Sale banners ═══ */}
      {flashSaleBanners.length > 0 && (
        <section className="px-2 sm:px-0">
          <SectionHeader icon={<Zap className="w-5 h-5 sm:w-6 sm:h-6" />} title="Flash Sale" subtitle="Ưu đãi giới hạn - nhanh tay kẻo hết!" accent="red" />
          <BannerCarousel banners={flashSaleBanners} height="h-[160px] sm:h-[240px]" />
        </section>
      )}

      {/* ═══ 3. Categories (2 rows + arrows) ═══ */}
      <div>
        {catLoading ? (
          <section className="bg-white sm:rounded-2xl p-3 sm:p-5 sm:shadow-sm sm:border sm:border-gray-100 animate-pulse">
            <div className="h-7 bg-gray-200 rounded w-40 mb-6"></div>
            <div className="flex gap-4 overflow-hidden">
              {Array.from({ length: 10 }).map((_, i) => (
                <div key={i} className="flex flex-col items-center gap-2 flex-1 min-w-[80px]">
                  <div className="w-12 h-12 bg-gray-200 rounded-xl"></div>
                  <div className="w-16 h-3 bg-gray-200 rounded"></div>
                </div>
              ))}
            </div>
          </section>
        ) : categories.length > 0 ? (() => {
          const DESKTOP_COLS = 10;
          const ROWS = 2;
          const desktopPerPage = DESKTOP_COLS * ROWS;
          const desktopTotalPages = Math.ceil(categories.length / desktopPerPage);
          const safeCatPageDesktop = Math.max(0, Math.min(catPage, desktopTotalPages - 1));
          const desktopSlice = categories.slice(safeCatPageDesktop * desktopPerPage, safeCatPageDesktop * desktopPerPage + desktopPerPage);
          const totalPages = desktopTotalPages;

          return (
            <section className="bg-white sm:rounded-2xl p-3 sm:p-5 sm:shadow-sm sm:border sm:border-gray-100">
              <div className="flex items-center justify-between mb-4 sm:mb-6 px-2 sm:px-0">
                <div>
                  <h2 className="text-lg sm:text-2xl font-black text-slate-900 tracking-tight">Danh mục</h2>
                  <p className="text-xs sm:text-sm text-slate-500 mt-0.5 hidden sm:block">Khám phá theo danh mục yêu thích</p>
                </div>
              </div>
              {/* Desktop: arrows + 10 cols x 2 rows */}
              <div className="hidden sm:flex items-center gap-2">
                <button
                  onClick={() => setCatPage(p => Math.max(0, p - 1))}
                  disabled={catPage === 0}
                  className="shrink-0 p-2 rounded-full bg-gray-100 hover:bg-red-100 text-gray-500 hover:text-red-600 disabled:opacity-20 disabled:hover:bg-gray-100 disabled:hover:text-gray-500 transition-colors">
                  <ChevronLeft className="w-6 h-6" />
                </button>
                <div className="flex-1 grid grid-cols-5 lg:grid-cols-10 gap-3">
                  {desktopSlice.map(cat => (
                    <Link key={cat.id} to={`/search?category=${cat.id}`}
                      className="flex flex-col items-center gap-2 p-3 rounded-xl hover:bg-red-50 transition-all group">
                      {cat.iconUrl ? (
                        <img src={cat.iconUrl} alt={cat.name} className="w-12 h-12 object-contain" />
                      ) : (
                        <div className="w-12 h-12 bg-gradient-to-br from-red-50 to-red-100 rounded-xl flex items-center justify-center">
                          <Tag className="w-5 h-5 text-red-500" />
                        </div>
                      )}
                      <span className="text-xs font-semibold text-slate-700 text-center line-clamp-2 group-hover:text-red-600">{cat.name}</span>
                    </Link>
                  ))}
                </div>
                <button
                  onClick={() => setCatPage(p => p + 1)}
                  disabled={catPage >= totalPages - 1}
                  className="shrink-0 p-2 rounded-full bg-gray-100 hover:bg-red-100 text-gray-500 hover:text-red-600 disabled:opacity-20 disabled:hover:bg-gray-100 disabled:hover:text-gray-500 transition-colors">
                  <ChevronRight className="w-6 h-6" />
                </button>
              </div>
              {/* Mobile: horizontal scroll, 2 rows */}
              <div className="sm:hidden overflow-x-auto scrollbar-hide pb-2 -mx-1 px-1">
                <div className="grid grid-rows-2 grid-flow-col auto-cols-[72px] gap-x-2 gap-y-1">
                  {categories.map(cat => (
                    <Link key={cat.id} to={`/search?category=${cat.id}`}
                      className="flex flex-col items-center gap-1.5 p-2 rounded-xl active:bg-red-50 transition-all">
                      {cat.iconUrl ? (
                        <img src={cat.iconUrl} alt={cat.name} className="w-10 h-10 object-contain" />
                      ) : (
                        <div className="w-10 h-10 bg-gradient-to-br from-red-50 to-red-100 rounded-xl flex items-center justify-center">
                          <Tag className="w-4 h-4 text-red-500" />
                        </div>
                      )}
                      <span className="text-[11px] font-semibold text-slate-700 text-center line-clamp-2 leading-tight">{cat.name}</span>
                    </Link>
                  ))}
                </div>
              </div>
            </section>
          );
        })() : (
          <section className="bg-white sm:rounded-2xl p-5 text-center sm:shadow-sm sm:border sm:border-gray-100">
            <p className="text-gray-400">Không có danh mục nào.</p>
          </section>
        )}
      </div>

      {/* ═══ 4. HOME_MID banners ═══ */}
      {midBanners.length > 0 && (
        <section>
          <BannerGrid banners={midBanners} />
        </section>
      )}

      {/* ═══ 5. Best Selling ═══ */}
      <div ref={bestSellingRef} className="min-h-[300px]">
        {isBestSellingVisible && (
          bestSellingLoading ? (
            <div className="flex justify-center py-16"><Loader2 className="w-8 h-8 animate-spin text-slate-400" /></div>
          ) : bestSelling.length > 0 && (
            <section>
              <SectionHeader
                icon={<Flame className="w-5 h-5 sm:w-6 sm:h-6" />}
                title="Sản phẩm bán chạy"
                subtitle="Những sản phẩm được yêu thích nhất"
                linkText="Xem tất cả"
                linkTo="/search?sortBy=best_selling"
                accent="red"
              />
              {/* Desktop: grid */}
              <div className="hidden sm:grid grid-cols-3 lg:grid-cols-5 gap-4">
                {bestSelling.map(p => (
                  <ProductCard key={p.id} product={p} onAddToCart={handleAddToCart} onViewDetail={handleViewDetail} />
                ))}
              </div>
              {/* Mobile: horizontal scroll */}
              <div className="sm:hidden relative">
                <div ref={bestSellingScrollRef} className="flex gap-2.5 overflow-x-auto scrollbar-hide pb-3 px-2 snap-x snap-mandatory">
                  {bestSelling.map(p => (
                    <div key={p.id} className="min-w-[160px] max-w-[160px] snap-start">
                      <ProductCard product={p} onAddToCart={handleAddToCart} onViewDetail={handleViewDetail} />
                    </div>
                  ))}
                  {/* "Xem thêm" card */}
                  <Link to="/search?sortBy=best_selling"
                    className="min-w-[120px] max-w-[120px] snap-start flex flex-col items-center justify-center gap-2 bg-white rounded-2xl border border-gray-100 text-red-600 font-bold text-sm hover:bg-red-50 transition-colors">
                    <ChevronRight className="w-6 h-6" />
                    <span>Xem thêm</span>
                  </Link>
                </div>
                {/* Scroll buttons (subtle) */}
                <button onClick={() => scrollBestSelling("left")}
                  className="absolute left-0 top-1/2 -translate-y-1/2 z-10 bg-white/80 shadow rounded-full p-1 hidden sm:block">
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <button onClick={() => scrollBestSelling("right")}
                  className="absolute right-0 top-1/2 -translate-y-1/2 z-10 bg-white/80 shadow rounded-full p-1 hidden sm:block">
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </section>
          )
        )}
      </div>

          {/* ═══ 6. Newest Products ═══ */}
      <div ref={newestRef} className="min-h-[300px]">
        {isNewestVisible && (
          newestLoading ? (
            <div className="flex justify-center py-16"><Loader2 className="w-8 h-8 animate-spin text-slate-400" /></div>
          ) : newest.length > 0 && (
            <section>
              <SectionHeader
                title="Sản phẩm mới nhất"
                subtitle="Vừa được đăng bán trên Sellico"
                linkText="Xem tất cả"
                linkTo="/search?sortBy=newest"
              />
              {/* Desktop: grid 5 cols */}
              <div className="hidden sm:grid grid-cols-3 lg:grid-cols-5 gap-4">
                {newest.map(p => (
                  <ProductCard key={p.id} product={p} onAddToCart={handleAddToCart} onViewDetail={handleViewDetail} />
                ))}
              </div>
              {/* Mobile: 2-col grid */}
              <div className="sm:hidden grid grid-cols-2 gap-2 px-2">
                {newest.map(p => (
                  <ProductCard key={p.id} product={p} onAddToCart={handleAddToCart} onViewDetail={handleViewDetail} />
                ))}
              </div>
              {/* "Xem thêm" button */}
              <div className="flex justify-center mt-6 px-2 sm:px-0">
                <Link to="/search?sortBy=newest"
                  className="px-8 py-3 bg-white border-2 border-red-500 text-red-600 font-bold rounded-xl hover:bg-red-50 transition-colors text-sm uppercase tracking-wide">
                  Xem thêm sản phẩm
                </Link>
              </div>
            </section>
          )
        )}
      </div>

          {/* ═══ 7. Bottom banners ═══ */}
      {bottomBanners.length > 0 && (
        <section>
          <BannerGrid banners={bottomBanners} />
        </section>
      )}
    </div>
  );
}
