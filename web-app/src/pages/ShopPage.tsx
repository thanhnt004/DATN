import { useState, useEffect, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  Store,
  Star,
  Package,
  Users,
  MessageSquare,
  Clock,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { getSellerPublic, followSeller, unfollowSeller, isFollowing } from "../api/sellerPublicApi";
import { getProductsBySeller } from "../api/productApi";
import { getCategoryTree } from "../api/categoryApi";
import { useAuth } from "../hooks/useAuth";
import { formatPrice } from "../utils/helpers";
import type { SellerResponse } from "../types/seller";
import type { ProductSummaryResponse } from "../types/product";
import type { CategoryTreeResponse } from "../types/admin";
import toast from "react-hot-toast";

/* ────────────────── helpers ────────────────── */

function formatCount(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}tr`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}k`;
  return n.toString();
}

function joinedSince(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const days = Math.floor(diff / 86_400_000);
  if (days < 30) return `${days} Ngày Trước`;
  const months = Math.floor(days / 30);
  if (months < 12) return `${months} Tháng Trước`;
  const years = Math.floor(months / 12);
  return `${years} Năm Trước`;
}

const PAGE_SIZE = 30;

/* ────────────────── MAIN COMPONENT ────────────────── */

export default function ShopPage() {
  const { sellerId } = useParams<{ sellerId: string }>();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  // seller info
  const [seller, setSeller] = useState<SellerResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // follow
  const [following, setFollowing] = useState(false);
  const [followLoading, setFollowLoading] = useState(false);

  // products
  const [products, setProducts] = useState<ProductSummaryResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [productsLoading, setProductsLoading] = useState(false);

  // tabs / categories
  const [activeTab, setActiveTab] = useState<string>("all"); // "all" or categoryId
  const [shopCategories, setShopCategories] = useState<{ id: string; name: string }[]>([]);
  const [allCategories, setAllCategories] = useState<CategoryTreeResponse[]>([]);

  /* ─── fetch seller ─── */
  useEffect(() => {
    if (!sellerId) return;
    setLoading(true);
    getSellerPublic(sellerId)
      .then((res) => {
        setSeller(res.data.result);
      })
      .catch(() => setError("Không tìm thấy cửa hàng."))
      .finally(() => setLoading(false));
  }, [sellerId]);

  /* ─── check following ─── */
  useEffect(() => {
    if (!sellerId || !isAuthenticated) return;
    isFollowing(sellerId)
      .then((res) => setFollowing(res.data.result ?? false))
      .catch(() => {});
  }, [sellerId, isAuthenticated]);

  /* ─── fetch categories tree ─── */
  useEffect(() => {
    getCategoryTree()
      .then((res) => setAllCategories(res.data.result ?? []))
      .catch(() => {});
  }, []);

  /* ─── fetch ALL products to determine categories ─── */
  useEffect(() => {
    if (!sellerId) return;
    getProductsBySeller(sellerId, { page: 0, size: 200 })
      .then((res) => {
        const items = res.data.result?.content ?? [];
        // extract unique categoryIds from products
        const catIds = [...new Set(items.map((p) => p.categoryId))];
        // flatten category tree to get names
        const flat: { id: string; name: string }[] = [];
        function flattenCats(cats: CategoryTreeResponse[]) {
          cats.forEach((c) => {
            flat.push({ id: c.id, name: c.name });
            if (c.children?.length) flattenCats(c.children);
          });
        }
        flattenCats(allCategories);
        // build category tabs
        const cats = catIds
          .map((cid) => {
            const found = flat.find((c) => c.id === cid);
            return found ? { id: found.id, name: found.name } : null;
          })
          .filter(Boolean) as { id: string; name: string }[];
        setShopCategories(cats);
      })
      .catch(() => {});
  }, [sellerId, allCategories]);

  /* ─── fetch products (paginated) ─── */
  const fetchProducts = useCallback(() => {
    if (!sellerId) return;
    setProductsLoading(true);
    getProductsBySeller(sellerId, { page, size: PAGE_SIZE })
      .then((res) => {
        const data = res.data.result;
        setProducts(data?.content ?? []);
        setTotalPages(data?.totalPages ?? 0);
      })
      .catch(() => {})
      .finally(() => setProductsLoading(false));
  }, [sellerId, page]);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  /* ─── filtered products by tab ─── */
  const displayProducts =
    activeTab === "all"
      ? products
      : products.filter((p) => p.categoryId === activeTab);

  /* ─── follow toggle ─── */
  const handleFollowToggle = async () => {
    if (!sellerId) return;
    if (!isAuthenticated) {
      toast.error("Vui lòng đăng nhập để theo dõi shop");
      navigate("/login");
      return;
    }
    setFollowLoading(true);
    try {
      if (following) {
        await unfollowSeller(sellerId);
        setFollowing(false);
        toast.success("Đã bỏ theo dõi");
      } else {
        await followSeller(sellerId);
        setFollowing(true);
        toast.success("Đã theo dõi shop");
      }
    } catch {
      toast.error("Thao tác thất bại");
    } finally {
      setFollowLoading(false);
    }
  };

  /* ─── loading / error ─── */
  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-20 text-center text-gray-400 animate-pulse text-lg">
        Đang tải thông tin cửa hàng…
      </div>
    );
  }

  if (error || !seller) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-20 text-center">
        <p className="text-xl font-bold text-gray-600">{error ?? "Không tìm thấy cửa hàng"}</p>
        <button
          onClick={() => navigate("/")}
          className="mt-4 px-6 py-2 bg-red-500 text-white rounded hover:bg-red-600 transition-colors"
        >
          Về trang chủ
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-4">
      {/* ═══════════════ SHOP HEADER ═══════════════ */}
      <div className="bg-white rounded-lg overflow-hidden mb-4">
        {/* Banner */}
        {seller.bannerUrl ? (
          <div className="h-40 sm:h-52 bg-gray-100">
            <img
              src={seller.bannerUrl}
              alt="Shop banner"
              className="w-full h-full object-cover"
            />
          </div>
        ) : (
          <div className="h-40 sm:h-52 bg-gradient-to-r from-red-500 to-orange-400" />
        )}

        {/* Shop info bar */}
        <div className="p-4 lg:p-6 -mt-10 relative">
          <div className="flex flex-col sm:flex-row gap-4">
            {/* Avatar */}
            <div className="shrink-0">
              {seller.logoUrl ? (
                <img
                  src={seller.logoUrl}
                  alt={seller.shopName}
                  className="w-20 h-20 rounded-full object-cover border-4 border-white shadow-md"
                />
              ) : (
                <div className="w-20 h-20 rounded-full bg-red-100 border-4 border-white shadow-md flex items-center justify-center">
                  <Store className="w-10 h-10 text-red-500" />
                </div>
              )}
            </div>

            {/* Name + buttons */}
            <div className="flex-1 pt-2">
              <h1 className="text-xl font-bold text-gray-900">{seller.shopName}</h1>
              {seller.description && (
                <p className="text-sm text-gray-500 mt-0.5 line-clamp-1">{seller.description}</p>
              )}
              <div className="flex items-center gap-2 mt-2">
                <button
                  onClick={handleFollowToggle}
                  disabled={followLoading}
                  className={`flex items-center gap-1.5 px-4 py-1.5 rounded text-sm font-medium border transition-all ${
                    following
                      ? "border-gray-300 text-gray-600 hover:bg-gray-50"
                      : "border-red-500 text-red-500 hover:bg-red-50"
                  }`}
                >
                  {following ? "Đang Theo Dõi" : "+ Theo Dõi"}
                </button>
                <button
                  onClick={() => {
                    if (!isAuthenticated) {
                      toast.error("Vui lòng đăng nhập để chat với shop");
                      navigate("/login");
                      return;
                    }
                    const peerId = seller.userId || seller.id;
                    navigate(`/user/messages?peerId=${encodeURIComponent(peerId)}`);
                  }}
                  className="flex items-center gap-1.5 px-4 py-1.5 rounded text-sm font-medium border border-blue-500 text-blue-600 hover:bg-blue-50 transition-all"
                >
                  <MessageSquare className="w-4 h-4" />
                  Chat
                </button>
              </div>
            </div>

            {/* Stats grid */}
            <div className="grid grid-cols-2 lg:grid-cols-3 gap-x-8 gap-y-2 text-sm text-gray-600 self-center">
              <div className="flex items-center gap-1.5">
                <Package className="w-4 h-4 text-gray-400" />
                <span>
                  Sản Phẩm: <span className="text-red-500 font-semibold">{formatCount(seller.totalProducts ?? 0)}</span>
                </span>
              </div>
              <div className="flex items-center gap-1.5">
                <Users className="w-4 h-4 text-gray-400" />
                <span>
                  Người Theo Dõi: <span className="text-red-500 font-semibold">{formatCount(seller.followerCount ?? 0)}</span>
                </span>
              </div>
              {seller.ratingAvg != null && (
                <div className="flex items-center gap-1.5">
                  <Star className="w-4 h-4 text-gray-400" />
                  <span>
                    Đánh Giá:{" "}
                    <span className="text-red-500 font-semibold">
                      {seller.ratingAvg.toFixed(1)}
                      {seller.ratingCount != null && (
                        <span className="text-gray-400 font-normal"> ({formatCount(seller.ratingCount)} Đánh Giá)</span>
                      )}
                    </span>
                  </span>
                </div>
              )}
              {seller.ratingCount != null && (
                <div className="flex items-center gap-1.5">
                  <MessageSquare className="w-4 h-4 text-gray-400" />
                  <span>
                    Lượt Đánh Giá: <span className="text-red-500 font-semibold">{formatCount(seller.ratingCount)}</span>
                  </span>
                </div>
              )}
              <div className="flex items-center gap-1.5">
                <Clock className="w-4 h-4 text-gray-400" />
                <span>
                  Tham Gia: <span className="text-red-500 font-semibold">{joinedSince(seller.createdAt)}</span>
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* ═══════════════ CATEGORY TABS ═══════════════ */}
      <div className="bg-white rounded-lg mb-4">
        <div className="flex overflow-x-auto scrollbar-hide border-b">
          <button
            onClick={() => setActiveTab("all")}
            className={`whitespace-nowrap px-6 py-3 text-sm font-semibold border-b-2 transition-colors ${
              activeTab === "all"
                ? "border-red-500 text-red-500"
                : "border-transparent text-gray-500 hover:text-gray-800"
            }`}
          >
            TẤT CẢ SẢN PHẨM
          </button>
          {shopCategories.map((cat) => (
            <button
              key={cat.id}
              onClick={() => setActiveTab(cat.id)}
              className={`whitespace-nowrap px-6 py-3 text-sm font-semibold border-b-2 transition-colors ${
                activeTab === cat.id
                  ? "border-red-500 text-red-500"
                  : "border-transparent text-gray-500 hover:text-gray-800"
              }`}
            >
              {cat.name}
            </button>
          ))}
        </div>
      </div>

      {/* ═══════════════ PRODUCTS GRID ═══════════════ */}
      <div className="bg-white rounded-lg p-4 lg:p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-base font-bold text-gray-800 uppercase">
            {activeTab === "all"
              ? "Tất Cả Sản Phẩm"
              : shopCategories.find((c) => c.id === activeTab)?.name ?? "Sản Phẩm"}
          </h2>
          <span className="text-sm text-gray-400">
            {displayProducts.length} sản phẩm
          </span>
        </div>

        {productsLoading ? (
          <div className="py-20 text-center text-gray-400 animate-pulse">Đang tải sản phẩm…</div>
        ) : displayProducts.length === 0 ? (
          <div className="py-20 flex flex-col items-center text-gray-400">
            <Package className="w-16 h-16 mb-3 text-gray-200" />
            <p className="text-base font-medium">Chưa có sản phẩm nào</p>
          </div>
        ) : (
          <>
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3">
              {displayProducts.map((product) => (
                <ShopProductCard key={product.id} product={product} />
              ))}
            </div>

            {/* Pagination */}
            {totalPages > 1 && activeTab === "all" && (
              <div className="flex items-center justify-center gap-2 mt-6">
                <button
                  onClick={() => setPage(Math.max(0, page - 1))}
                  disabled={page === 0}
                  className="p-2 rounded border text-gray-500 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                  const start = Math.max(0, Math.min(page - 2, totalPages - 5));
                  const p = start + i;
                  if (p >= totalPages) return null;
                  return (
                    <button
                      key={p}
                      onClick={() => setPage(p)}
                      className={`w-9 h-9 rounded text-sm font-medium transition-colors ${
                        p === page
                          ? "bg-red-500 text-white"
                          : "border text-gray-600 hover:bg-gray-50"
                      }`}
                    >
                      {p + 1}
                    </button>
                  );
                })}
                <button
                  onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                  disabled={page >= totalPages - 1}
                  className="p-2 rounded border text-gray-500 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

/* ────────────────── Shop Product Card ────────────────── */

function ShopProductCard({ product }: { product: ProductSummaryResponse }) {
  const navigate = useNavigate();

  return (
    <div
      onClick={() => navigate(`/product/${product.id}`)}
      className="cursor-pointer group bg-white rounded-lg border border-gray-100 hover:border-red-200 hover:shadow-md overflow-hidden transition-all"
    >
      <div className="aspect-square bg-gray-50 overflow-hidden">
        <img
          src={product.primaryImageUrl ?? "https://placehold.co/300x300?text=No+Image"}
          alt={product.name}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          loading="lazy"
        />
      </div>
      <div className="p-2.5">
        <h4 className="text-xs font-medium text-gray-800 line-clamp-2 leading-snug mb-1.5 group-hover:text-red-600 transition-colors">
          {product.name}
        </h4>
        <div className="flex items-end gap-1.5">
          <span className="text-sm font-bold text-red-600">
            {formatPrice(product.minPrice)}
          </span>
          {product.maxPrice > product.minPrice && (
            <span className="text-[10px] text-gray-400">
              – {formatPrice(product.maxPrice)}
            </span>
          )}
        </div>
        <div className="flex items-center justify-between mt-1 text-[10px] text-gray-400">
          {product.ratingAvg != null && (
            <div className="flex items-center gap-0.5">
              <Star className="w-3 h-3 text-yellow-400 fill-yellow-400" />
              <span>{product.ratingAvg.toFixed(1)}</span>
            </div>
          )}
          {product.soldCount != null && product.soldCount > 0 && (
            <span>Đã bán {formatCount(product.soldCount)}</span>
          )}
        </div>
      </div>
    </div>
  );
}
