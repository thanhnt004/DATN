import { useState, useEffect, useMemo, useCallback, useRef } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import {
  ShoppingCart,
  Star,
  Minus,
  Plus,
  ChevronRight,
  ChevronLeft,
  ChevronDown,
  ChevronUp,
  Store,
  MessageSquare,
  Package,
  Users,
  Truck,
  ShieldCheck,
  X,
  Zap,
  Loader2,
} from "lucide-react";
import { getProductById, getProductsBySeller, getProductsByCategory } from "../api/productApi";
import { getCategoryById } from "../api/categoryApi";
import { getSellerPublic } from "../api/sellerPublicApi";
import { addToCart } from "../api/cartApi";
import { getProductReviews, getReviewSummary } from "../api/reviewApi";
import { checkAvailability } from "../api/inventoryApi";
import type { StockAvailabilityResponse } from "../types/inventory";
import { useAuth } from "../hooks/useAuth";
import { formatPrice } from "../utils/helpers";
import type { ProductResponse, SkuResponse, ProductSummaryResponse } from "../types/product";
import type { PublicCategoryResponse } from "../api/categoryApi";
import type { SellerResponse } from "../types/seller";
import type { ReviewResponse, ReviewSummaryResponse } from "../types/review";

/* ─────────── helpers ─────────── */

function formatCount(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}tr`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}k`;
  return n.toString();
}

function StarRating({ rating, size = 16 }: { rating: number; size?: number }) {
  return (
    <div className="flex items-center gap-0.5">
      {[1, 2, 3, 4, 5].map((s) => {
        const filled = rating >= s;
        const partial = !filled && rating > s - 1;
        const pct = partial ? Math.round((rating - (s - 1)) * 100) : 0;
        return (
          <span key={s} className="relative inline-block" style={{ width: size, height: size }}>
            <Star style={{ width: size, height: size }} className="absolute inset-0 text-gray-200" fill="currentColor" />
            <span className="absolute inset-0 overflow-hidden" style={{ width: filled ? "100%" : `${pct}%` }}>
              <Star style={{ width: size, height: size }} className="text-yellow-400" fill="currentColor" />
            </span>
          </span>
        );
      })}
    </div>
  );
}

/* ─────────── MAIN ─────────── */

export default function MobileProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  /* ── core state ── */
  const [product, setProduct] = useState<ProductResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [category, setCategory] = useState<PublicCategoryResponse | null>(null);
  const [parentCategory, setParentCategory] = useState<PublicCategoryResponse | null>(null);
  const [seller, setSeller] = useState<SellerResponse | null>(null);

  // image gallery
  const [selectedImage, setSelectedImage] = useState(0);
  const galleryRef = useRef<HTMLDivElement>(null);
  const thumbStripRef = useRef<HTMLDivElement>(null);

  // variant selection
  const [selectedOptions, setSelectedOptions] = useState<Record<string, string>>({});
  const [allSelected, setAllSelected] = useState(false);

  // quantity & cart
  const [quantity, setQuantity] = useState(1);
  const [addingToCart, setAddingToCart] = useState(false);

  // related products
  const [shopProducts, setShopProducts] = useState<ProductSummaryResponse[]>([]);
  const [similarProducts, setSimilarProducts] = useState<ProductSummaryResponse[]>([]);

  // inventory
  const [stockMap, setStockMap] = useState<Record<string, StockAvailabilityResponse>>({});

  // reviews
  const [reviews, setReviews] = useState<ReviewResponse[]>([]);
  const [reviewSummary, setReviewSummary] = useState<ReviewSummaryResponse | null>(null);
  const [reviewFilter, setReviewFilter] = useState<string>("all");
  const [reviewPage, setReviewPage] = useState(0);
  const [reviewTotalPages, setReviewTotalPages] = useState(0);
  const [reviewLoading, setReviewLoading] = useState(false);

  // mobile UI toggles
  const [descExpanded, setDescExpanded] = useState(false);
  const [showSpecSheet, setShowSpecSheet] = useState(false);
  const [showVariantSheet, setShowVariantSheet] = useState(false);
  const [variantAction, setVariantAction] = useState<"cart" | "buy">("cart");

  /* ── fetch product ── */
  useEffect(() => {
    if (!id) return;
    setLoading(true);
    setCategory(null);
    setParentCategory(null);
    setSeller(null);
    setShopProducts([]);
    setSimilarProducts([]);
    setSelectedImage(0);
    setQuantity(1);
    setSelectedOptions({});
    setAllSelected(false);

    getProductById(id)
      .then((res) => {
        const p = res.data.result;
        setProduct(p);
        if (!p.options || p.options.length === 0) setAllSelected(true);

        if (p.categoryId) {
          getCategoryById(p.categoryId)
            .then((cRes) => {
              const cat = cRes.data.result;
              setCategory(cat);
              if (cat.parentId) getCategoryById(cat.parentId).then((pRes) => setParentCategory(pRes.data.result)).catch(() => {});
            })
            .catch(() => {});
        }

        if (p.sellerId) getSellerPublic(p.sellerId).then((sRes) => setSeller(sRes.data.result)).catch(() => {});

        if (p.skus?.length) {
          const skuIds = p.skus.map((s) => s.id);
          checkAvailability(skuIds)
            .then((r) => {
              const map: Record<string, StockAvailabilityResponse> = {};
              r.data.result.forEach((item) => { map[item.skuId] = item; });
              setStockMap(map);
            })
            .catch(() => {});
        }

        getProductsBySeller(p.sellerId, { page: 0, size: 12 })
          .then((r) => setShopProducts(r.data.result.content.filter((x) => x.id !== p.id)))
          .catch(() => {});

        if (p.categoryId) {
          getProductsByCategory(p.categoryId, { page: 0, size: 12 })
            .then((r) => setSimilarProducts(r.data.result.content.filter((x) => x.id !== p.id)))
            .catch(() => {});
        }
      })
      .catch(() => setError("Không tìm thấy sản phẩm"))
      .finally(() => setLoading(false));
  }, [id]);

  /* ── fetch reviews ── */
  const fetchReviews = useCallback(
    (filter: string, page: number) => {
      if (!id) return;
      setReviewLoading(true);
      const params: Record<string, unknown> = { page, size: 5 };
      if (filter.startsWith("star-")) params.rating = parseInt(filter.split("-")[1]);
      else if (filter === "comment") params.hasComment = true;
      else if (filter === "images") params.hasImages = true;

      getProductReviews(id, params as { rating?: number; hasComment?: boolean; hasImages?: boolean; page?: number; size?: number })
        .then((res) => {
          const data = res.data.result;
          setReviews(data.content);
          setReviewTotalPages(data.totalPages);
        })
        .catch(() => setReviews([]))
        .finally(() => setReviewLoading(false));
    },
    [id],
  );

  useEffect(() => {
    if (!id) return;
    getReviewSummary(id).then((res) => setReviewSummary(res.data.result)).catch(() => {});
    fetchReviews("all", 0);
  }, [id, fetchReviews]);

  const handleReviewFilterChange = (filter: string) => { setReviewFilter(filter); setReviewPage(0); fetchReviews(filter, 0); };
  const handleReviewPageChange = (newPage: number) => { setReviewPage(newPage); fetchReviews(reviewFilter, newPage); };

  /* ── derived: selected SKU ── */
  const selectedSku: SkuResponse | null = useMemo(() => {
    if (!product?.skus?.length) return null;
    if (!product.options || product.options.length === 0) return product.skus[0];
    if (!allSelected) return null;
    let matched = product.skus.find((sku) => {
      const attrs = sku.attributes || [];
      if (attrs.length === 0) return false;
      return attrs.every((attr) => selectedOptions[attr.optionName] === attr.valueName);
    }) ?? null;

    // Fallback: if backend returns empty attributes, calculate cartesian index
    if (!matched) {
      const isMissingAttrs = product.skus.every(s => !s.attributes || s.attributes.length === 0);
      if (isMissingAttrs) {
        let expectedCount = 1;
        product.options.forEach(opt => expectedCount *= opt.values.length);
        if (expectedCount === product.skus.length) {
          let index = 0;
          let multiplier = 1;
          for (let i = product.options.length - 1; i >= 0; i--) {
            const opt = product.options[i];
            const valIndex = opt.values.findIndex(v => v.value === selectedOptions[opt.name]);
            if (valIndex !== -1) index += valIndex * multiplier;
            multiplier *= opt.values.length;
          }
          matched = product.skus[index] ?? null;
        }
      }
    }
    return matched;
  }, [product, selectedOptions, allSelected]);

  /* ── image list ── */
  const images = useMemo(() => {
    const base = product?.images?.length
      ? [...product.images]
      : [{ id: "0", url: "https://placehold.co/600x600?text=No+Image", isPrimary: true, sortOrder: 0 }];
    if (!product?.options?.length) return base;

    // Collect option value images (unique)
    const existingUrls = new Set(base.map((img) => img.url));
    const optionImages: typeof base = [];
    const firstOption = product.options[0];
    if (firstOption) {
      firstOption.values.forEach((val) => {
        if (val.imageUrl && !existingUrls.has(val.imageUrl)) {
          existingUrls.add(val.imageUrl);
          optionImages.push({ id: `optval-${val.id}`, url: val.imageUrl, isPrimary: false, sortOrder: 0 });
        }
      });
    }

    // Insert option images right after the primary image
    if (optionImages.length > 0) {
      const primaryIdx = base.findIndex((img) => img.isPrimary);
      const insertAt = primaryIdx >= 0 ? primaryIdx + 1 : 1;
      base.splice(insertAt, 0, ...optionImages);
    }

    return base;
  }, [product]);

  /* ── map image URL → option value name (for thumbnail labels) ── */
  const imageOptionMap = useMemo(() => {
    const map: Record<string, string> = {};
    if (product?.options?.length) {
      const firstOption = product.options[0];
      firstOption.values.forEach((val) => {
        if (val.imageUrl) map[val.imageUrl] = val.value;
      });
    }
    return map;
  }, [product]);

  /* ── variant image switching ── */
  useEffect(() => {
    if (!product?.options?.length) return;
    const firstOption = product.options[0];
    const selectedValue = selectedOptions[firstOption.name];
    if (!selectedValue) return;
    const optVal = firstOption.values.find((v) => v.value === selectedValue);
    if (!optVal?.imageUrl) return;
    const idx = images.findIndex((img) => img.url === optVal.imageUrl);
    if (idx >= 0) setSelectedImage(idx);
  }, [selectedOptions, product, images]);

  /* ── scroll thumbnail strip when selected image changes ── */
  useEffect(() => {
    if (!thumbStripRef.current) return;
    const child = thumbStripRef.current.children[selectedImage] as HTMLElement | undefined;
    child?.scrollIntoView({ behavior: "smooth", inline: "center", block: "nearest" });
  }, [selectedImage]);

  /* ── swipe support for gallery ── */
  const touchStartX = useRef(0);
  const handleTouchStart = (e: React.TouchEvent) => { touchStartX.current = e.touches[0].clientX; };
  const handleTouchEnd = (e: React.TouchEvent) => {
    const diff = touchStartX.current - e.changedTouches[0].clientX;
    if (Math.abs(diff) > 50) {
      if (diff > 0) setSelectedImage((i) => (i >= images.length - 1 ? 0 : i + 1));
      else setSelectedImage((i) => (i <= 0 ? images.length - 1 : i - 1));
    }
  };

  /* ── handlers ── */
  const handleOptionSelect = useCallback(
    (optionName: string, value: string) => {
      setSelectedOptions((prev) => {
        const next = { ...prev };
        if (prev[optionName] === value) delete next[optionName];
        else next[optionName] = value;
        if (product?.options) setAllSelected(product.options.every((opt) => !!next[opt.name]));
        return next;
      });
    },
    [product],
  );

  const handleAddToCart = async () => {
    if (!isAuthenticated) { navigate("/login"); return; }
    if (!product || !selectedSku) { toast.error("Vui lòng chọn phân loại sản phẩm"); return; }
    setAddingToCart(true);
    try {
      await addToCart({ skuId: selectedSku.id, productId: product.id, sellerId: product.sellerId, quantity });
      toast.success(`Đã thêm ${quantity} sản phẩm vào giỏ hàng`);
      window.dispatchEvent(new Event("cart-updated"));
      setShowVariantSheet(false);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || "Không thể thêm vào giỏ hàng";
      toast.error(msg);
    } finally { setAddingToCart(false); }
  };

  const handleBuyNow = async () => {
    if (!isAuthenticated) { navigate("/login"); return; }
    if (!product || !selectedSku) { toast.error("Vui lòng chọn phân loại sản phẩm"); return; }
    setAddingToCart(true);
    try {
      await addToCart({ skuId: selectedSku.id, productId: product.id, sellerId: product.sellerId, quantity });
      window.dispatchEvent(new Event("cart-updated"));
      navigate("/cart");
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || "Lỗi khi thêm vào giỏ hàng";
      toast.error(msg);
    } finally { setAddingToCart(false); }
  };

  const handleStartChat = () => {
    if (!isAuthenticated) {
      toast.error("Vui lòng đăng nhập để chat với shop");
      navigate("/login");
      return;
    }
    const peerId = seller?.userId ?? product?.sellerId;
    if (!peerId) return;
    navigate(`/user/messages?peerId=${encodeURIComponent(peerId)}`);
  };

  const openVariantSheet = (action: "cart" | "buy") => {
    setVariantAction(action);
    setShowVariantSheet(true);
  };

  /* ── price logic ── */
  const showPriceRange = !allSelected && product && product.minPrice !== product.maxPrice;
  const currentPrice = selectedSku?.price ?? product?.minPrice ?? 0;
  const originalPrice = selectedSku?.originalPrice;
  const discountPercent = originalPrice && originalPrice > currentPrice ? Math.round(((originalPrice - currentPrice) / originalPrice) * 100) : 0;
  const selectedStock = selectedSku ? stockMap[selectedSku.id] : null;
  const stockLoaded = Object.keys(stockMap).length > 0;
  const allOutOfStock = stockLoaded && product?.skus?.length ? product.skus.every((sku) => { const st = stockMap[sku.id]; return st ? st.availableStock <= 0 : false; }) : false;
  const inStock = allOutOfStock ? false : selectedSku ? selectedSku.status === "ACTIVE" && (selectedStock ? selectedStock.availableStock > 0 : true) : true;
  const availableQty = selectedStock?.availableStock ?? 999;

  // total classification count
  const totalVariants = product?.skus?.length ?? 0;

  /* ─────────── LOADING ─────────── */
  if (loading) {
    return (
      <div className="min-h-screen bg-gray-100">
        <div className="animate-pulse">
          <div className="bg-gray-200 aspect-square w-full" />
          <div className="p-4 space-y-3">
            <div className="h-5 bg-gray-200 rounded w-3/4" />
            <div className="h-8 bg-gray-200 rounded w-1/2" />
            <div className="h-4 bg-gray-200 rounded w-2/3" />
          </div>
        </div>
      </div>
    );
  }

  /* ─────────── ERROR ─────────── */
  if (error || !product) {
    return (
      <div className="min-h-screen bg-gray-100 flex flex-col items-center justify-center px-4">
        <h2 className="text-xl font-bold text-gray-800 mb-4">{error || "Sản phẩm không tồn tại"}</h2>
        <Link to="/" className="text-red-500 font-medium">Quay về trang chủ</Link>
      </div>
    );
  }

  /* ─────────── SHEET IMAGE (current option value image or primary product image) ─────────── */
  const sheetImage = (() => {
    if (selectedSku && product.options?.length) {
      const firstOpt = product.options[0];
      const selVal = selectedOptions[firstOpt.name];
      const optVal = firstOpt.values.find((v) => v.value === selVal);
      if (optVal?.imageUrl) return optVal.imageUrl;
    }
    if (product.images?.length) {
      const primary = product.images.find((i) => i.isPrimary);
      return primary ? primary.url : product.images[0].url;
    }
    return "https://placehold.co/300x300?text=No+Image";
  })();

  return (
    <>
      <div className="bg-gray-100 min-h-screen pb-20">
        {/* ═══════════ IMAGE CAROUSEL ═══════════ */}
        <div className="relative bg-white">
          <div
            ref={galleryRef}
            className="relative aspect-square overflow-hidden"
            onTouchStart={handleTouchStart}
            onTouchEnd={handleTouchEnd}
          >
            <img
              src={images[selectedImage]?.url}
              alt={product.name}
              className="w-full h-full object-contain"
            />
            {/* left / right arrows */}
            {images.length > 1 && (
              <>
                <button
                  onClick={() => setSelectedImage((i) => (i <= 0 ? images.length - 1 : i - 1))}
                  className="absolute left-2 top-1/2 -translate-y-1/2 bg-black/30 text-white rounded-full p-1"
                >
                  <ChevronLeft className="w-5 h-5" />
                </button>
                <button
                  onClick={() => setSelectedImage((i) => (i >= images.length - 1 ? 0 : i + 1))}
                  className="absolute right-2 top-1/2 -translate-y-1/2 bg-black/30 text-white rounded-full p-1"
                >
                  <ChevronRight className="w-5 h-5" />
                </button>
              </>
            )}
            {/* dot indicators */}
            {images.length > 1 && (
              <div className="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-1.5">
                {images.map((_, idx) => (
                  <button
                    key={idx}
                    onClick={() => setSelectedImage(idx)}
                    className={`w-2 h-2 rounded-full transition-all ${idx === selectedImage ? "bg-white w-4" : "bg-white/50"}`}
                  />
                ))}
              </div>
            )}
          </div>

          {/* thumbnail strip with option value labels + deselect */}
          {images.length > 1 && (
            <div className="px-3 py-2 border-t border-gray-100">
              <div ref={thumbStripRef} className="flex gap-1.5 overflow-x-auto scrollbar-hide">
                {images.map((img, idx) => {
                  const isActive = selectedImage === idx;
                  const optionLabel = imageOptionMap[img.url];
                  return (
                    <button
                      key={img.id}
                      onClick={() => {
                        if (isActive) {
                          setSelectedImage(0);
                          if (optionLabel && product.options?.length) {
                            const optName = product.options[0].name;
                            if (selectedOptions[optName] === optionLabel) {
                              handleOptionSelect(optName, optionLabel);
                            }
                          }
                        } else {
                          setSelectedImage(idx);
                          if (optionLabel && product.options?.length) {
                            const optName = product.options[0].name;
                            if (selectedOptions[optName] !== optionLabel) {
                              handleOptionSelect(optName, optionLabel);
                            }
                          }
                        }
                      }}
                      className={`flex-shrink-0 flex flex-col items-center gap-0.5 transition-colors`}
                    >
                      <div className={`w-14 h-14 rounded border-2 overflow-hidden ${isActive ? "border-red-500" : "border-transparent"}`}>
                        <img src={img.url} alt="" className="w-full h-full object-cover" />
                      </div>
                      {isActive && optionLabel && (
                        <span className="text-[10px] text-red-600 font-medium leading-tight max-w-[56px] truncate">
                          {optionLabel}
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>
            </div>
          )}
        </div>

        {/* ═══════════ PRICE + INFO ═══════════ */}
        <div className="bg-white px-4 pt-3 pb-4">
          {/* price block */}
          <div className="mb-2">
            {showPriceRange ? (
              <span className="text-xl font-bold text-red-600">
                {formatPrice(product.minPrice)} – {formatPrice(product.maxPrice)}
              </span>
            ) : (
              <div className="flex items-end gap-2 flex-wrap">
                <span className="text-2xl font-bold text-red-600">{formatPrice(currentPrice)}</span>
                {originalPrice != null && originalPrice > currentPrice && (
                  <>
                    <span className="text-sm text-gray-400 line-through">{formatPrice(originalPrice)}</span>
                    <span className="bg-red-100 text-red-600 text-xs font-bold px-1.5 py-0.5 rounded">
                      -{discountPercent}%
                    </span>
                  </>
                )}
              </div>
            )}
          </div>

          {/* product name */}
          <h1 className="text-base font-semibold text-gray-900 leading-snug mb-2 line-clamp-3">{product.name}</h1>

          {/* rating / sold */}
          <div className="flex items-center gap-3 text-xs text-gray-500 mb-1">
            {product.ratingAvg != null && (
              <div className="flex items-center gap-1">
                <StarRating rating={product.ratingAvg} size={12} />
                <span className="text-gray-700 font-medium">{product.ratingAvg.toFixed(1)}</span>
              </div>
            )}
            {product.ratingCount != null && product.ratingCount > 0 && (
              <span>{formatCount(product.ratingCount)} đánh giá</span>
            )}
            {product.soldCount != null && product.soldCount > 0 && (
              <span>Đã bán {formatCount(product.soldCount)}</span>
            )}
          </div>
        </div>

        {/* ═══════════ CLASSIFICATION SELECTOR ENTRY ═══════════ */}
        {product.options && product.options.length > 0 && (
          <button
            onClick={() => openVariantSheet("cart")}
            className="bg-white mt-2 px-4 py-3 w-full flex items-center justify-between text-sm"
          >
            <div>
              <span className="text-gray-500">Phân Loại: </span>
              {allSelected ? (
                <span className="text-gray-900 font-medium">
                  {product.options.map((o) => selectedOptions[o.name]).join(", ")}
                </span>
              ) : (
                <span className="text-gray-400">{totalVariants} phân loại có sẵn</span>
              )}
            </div>
            <ChevronRight className="w-4 h-4 text-gray-400" />
          </button>
        )}

        {/* ═══════════ SHIPPING ═══════════ */}
        <div className="bg-white mt-2 px-4 py-3">
          <div className="flex items-start gap-3 text-sm">
            <Truck className="w-5 h-5 text-teal-500 shrink-0 mt-0.5" />
            <div className="flex-1">
              <div className="flex items-center gap-2 text-gray-700">
                <span>
                  Nhận từ{" "}
                  <span className="font-medium">
                    {new Date(Date.now() + 2 * 86400000).toLocaleDateString("vi-VN", { day: "2-digit", month: "short" })}
                    {" - "}
                    {new Date(Date.now() + 6 * 86400000).toLocaleDateString("vi-VN", { day: "2-digit", month: "short" })}
                  </span>
                </span>
              </div>
              <div className="text-teal-600 font-medium text-xs mt-0.5">Phí ship 0₫</div>
            </div>
          </div>
        </div>

        {/* ═══════════ TRUST BADGE ═══════════ */}
        <div className="bg-white mt-2 px-4 py-3">
          <div className="flex items-center gap-3 text-sm">
            <ShieldCheck className="w-5 h-5 text-red-500 shrink-0" />
            <div>
              <span className="text-gray-800 font-medium">An Tâm Mua Sắm Cùng Sellico</span>
              <p className="text-xs text-gray-400 mt-0.5">Trả hàng miễn phí 15 ngày · Hàng chính hãng</p>
            </div>
          </div>
        </div>

        {/* ═══════════ SHOP INFO ═══════════ */}
        <div className="bg-white mt-2 px-4 py-3">
          {/* row 1: avatar + name + view shop */}
          <div className="flex items-center gap-3 mb-3">
            {seller?.logoUrl ? (
              <img src={seller.logoUrl} alt={seller.shopName} className="w-12 h-12 rounded-full object-cover border" />
            ) : (
              <div className="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center shrink-0">
                <Store className="w-6 h-6 text-red-500" />
              </div>
            )}
            <div className="flex-1 min-w-0">
              <p className="font-semibold text-gray-900 text-sm truncate">
                {seller?.shopName ?? `Shop #${product.sellerId.slice(0, 8)}`}
              </p>
              {seller?.address && <p className="text-xs text-gray-400 truncate">{seller.address}</p>}
            </div>
            <Link
              to={`/shop/${product.sellerId}`}
              className="text-xs border border-red-500 text-red-500 rounded px-3 py-1.5 font-medium shrink-0 hover:bg-red-50"
            >
              Xem Shop
            </Link>
            <button
              onClick={handleStartChat}
              className="text-xs border border-blue-500 text-blue-600 rounded px-3 py-1.5 font-medium shrink-0 hover:bg-blue-50"
            >
              Chat
            </button>
          </div>

          {/* row 2: shop stats */}
          <div className="flex items-center gap-4 text-xs text-gray-500 overflow-x-auto">
            {seller?.ratingAvg != null && (
              <div className="flex items-center gap-1 shrink-0">
                <Star className="w-3.5 h-3.5 text-yellow-400 fill-yellow-400" />
                <span>{seller.ratingAvg.toFixed(1)}</span>
              </div>
            )}
            {seller?.totalProducts != null && (
              <div className="flex items-center gap-1 shrink-0">
                <Package className="w-3.5 h-3.5 text-gray-400" />
                <span>{formatCount(seller.totalProducts)} sản phẩm</span>
              </div>
            )}
            {seller?.followerCount != null && (
              <div className="flex items-center gap-1 shrink-0">
                <Users className="w-3.5 h-3.5 text-gray-400" />
                <span>{formatCount(seller.followerCount)} theo dõi</span>
              </div>
            )}
            {seller?.ratingCount != null && (
              <div className="flex items-center gap-1 shrink-0">
                <MessageSquare className="w-3.5 h-3.5 text-gray-400" />
                <span>{formatCount(seller.ratingCount)} đánh giá</span>
              </div>
            )}
          </div>
        </div>

        {/* ═══════════ PRODUCT DETAILS (specs) — tap to expand ═══════════ */}
        <button
          onClick={() => setShowSpecSheet(true)}
          className="bg-white mt-2 px-4 py-3.5 w-full flex items-center justify-between text-sm"
        >
          <span className="font-medium text-gray-900">Chi Tiết Sản Phẩm</span>
          <ChevronRight className="w-4 h-4 text-gray-400" />
        </button>

        {/* ═══════════ DESCRIPTION ═══════════ */}
        <div className="bg-white mt-2 px-4 py-3">
          <h3 className="text-sm font-semibold text-gray-900 mb-2">Mô Tả Sản Phẩm</h3>
          {product.description ? (
            <>
              <div
                className={`text-sm text-gray-700 whitespace-pre-line leading-relaxed overflow-hidden transition-all duration-300 ${
                  descExpanded ? "" : "max-h-[120px]"
                }`}
              >
                {product.description}
              </div>
              {product.description.length > 200 && (
                <button
                  onClick={() => setDescExpanded(!descExpanded)}
                  className="flex items-center gap-1 text-red-500 text-sm font-medium mt-2 mx-auto"
                >
                  {descExpanded ? (
                    <>Thu gọn <ChevronUp className="w-4 h-4" /></>
                  ) : (
                    <>Xem thêm <ChevronDown className="w-4 h-4" /></>
                  )}
                </button>
              )}
            </>
          ) : (
            <p className="text-sm text-gray-400 italic">Chưa có mô tả sản phẩm.</p>
          )}
        </div>

        {/* ═══════════ REVIEWS ═══════════ */}
        <div className="bg-white mt-2 px-4 py-3">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-sm font-semibold text-gray-900">Đánh Giá Sản Phẩm</h3>
            {reviewSummary && (
              <div className="flex items-center gap-1 text-sm">
                <Star className="w-4 h-4 text-yellow-400 fill-yellow-400" />
                <span className="text-red-500 font-bold">{reviewSummary.ratingAvg.toFixed(1)}</span>
                <span className="text-gray-400">/5 ({formatCount(reviewSummary.totalCount)})</span>
              </div>
            )}
          </div>

          {/* rating distribution */}
          {reviewSummary && (
            <div className="space-y-1.5 mb-4">
              {[5, 4, 3, 2, 1].map((star) => {
                const count = reviewSummary.ratingDistribution?.[star] ?? 0;
                const total = reviewSummary.totalCount ?? 0;
                const pct = total > 0 ? (count / total) * 100 : 0;
                return (
                  <div key={star} className="flex items-center gap-2 text-xs">
                    <div className="flex items-center gap-0.5 w-[50px] justify-end text-gray-500">
                      <span>{star}</span>
                      <Star className="w-3 h-3 text-yellow-400 fill-yellow-400" />
                    </div>
                    <div className="flex-1 h-2 bg-gray-100 rounded-full overflow-hidden">
                      <div className="h-full bg-red-400 rounded-full" style={{ width: `${pct}%` }} />
                    </div>
                    <span className="w-[28px] text-gray-400 text-right">{count}</span>
                  </div>
                );
              })}
            </div>
          )}

          {/* filter chips */}
          <div className="flex gap-2 overflow-x-auto pb-2 scrollbar-hide mb-3">
            {[
              { key: "all", label: "Tất Cả" },
              { key: "star-5", label: `5⭐ (${reviewSummary?.ratingDistribution?.[5] ?? 0})` },
              { key: "star-4", label: `4⭐ (${reviewSummary?.ratingDistribution?.[4] ?? 0})` },
              { key: "star-3", label: `3⭐ (${reviewSummary?.ratingDistribution?.[3] ?? 0})` },
              { key: "star-2", label: `2⭐ (${reviewSummary?.ratingDistribution?.[2] ?? 0})` },
              { key: "star-1", label: `1⭐ (${reviewSummary?.ratingDistribution?.[1] ?? 0})` },
              { key: "comment", label: "Có bình luận" },
              { key: "images", label: "Có hình ảnh" },
            ].map(({ key, label }) => (
              <button
                key={key}
                onClick={() => handleReviewFilterChange(key)}
                className={`px-3 py-1.5 text-xs rounded-full whitespace-nowrap border transition-colors ${
                  reviewFilter === key ? "border-red-500 text-red-500 bg-red-50" : "border-gray-200 text-gray-600"
                }`}
              >
                {label}
              </button>
            ))}
          </div>

          {/* review list */}
          {reviewLoading ? (
            <div className="py-8 flex justify-center">
              <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-red-500" />
            </div>
          ) : reviews.length > 0 ? (
            <div className="divide-y">
              {reviews.map((review) => (
                <div key={review.id} className="py-3 flex gap-2.5">
                  <div className="shrink-0">
                    {review.userAvatar ? (
                      <img src={review.userAvatar} alt="" className="w-8 h-8 rounded-full object-cover" />
                    ) : (
                      <div className="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center text-gray-500 text-xs font-semibold">
                        {(review.userName ?? "U").charAt(0).toUpperCase()}
                      </div>
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-xs font-medium text-gray-800">{review.userName ?? "Người dùng"}</p>
                    <div className="flex items-center gap-1 mt-0.5">
                      <StarRating rating={review.rating} size={11} />
                      <span className="text-[10px] text-gray-400 ml-1">
                        {new Date(review.createdAt).toLocaleDateString("vi-VN")}
                      </span>
                    </div>
                    {review.comment && <p className="text-xs text-gray-700 mt-1.5 whitespace-pre-line">{review.comment}</p>}
                    {review.images && review.images.length > 0 && (
                      <div className="flex gap-1.5 mt-2 flex-wrap">
                        {review.images.map((img, i) => (
                          <img key={i} src={img} alt="" className="w-16 h-16 object-cover rounded border" />
                        ))}
                      </div>
                    )}
                    {review.reply && (
                      <div className="mt-2 bg-gray-50 rounded p-2 border-l-2 border-red-300">
                        <p className="text-[10px] font-semibold text-gray-500 mb-0.5">Phản Hồi Của Người Bán</p>
                        <p className="text-xs text-gray-600 whitespace-pre-line">{review.reply.comment}</p>
                      </div>
                    )}
                  </div>
                </div>
              ))}

              {/* pagination */}
              {reviewTotalPages > 1 && (
                <div className="flex justify-center gap-1.5 pt-3">
                  <button onClick={() => handleReviewPageChange(reviewPage - 1)} disabled={reviewPage === 0} className="px-2.5 py-1 text-xs border rounded disabled:opacity-40">‹</button>
                  {Array.from({ length: reviewTotalPages }, (_, i) => (
                    <button
                      key={i}
                      onClick={() => handleReviewPageChange(i)}
                      className={`px-2.5 py-1 text-xs border rounded ${i === reviewPage ? "border-red-500 text-red-500 bg-red-50" : ""}`}
                    >
                      {i + 1}
                    </button>
                  ))}
                  <button onClick={() => handleReviewPageChange(reviewPage + 1)} disabled={reviewPage >= reviewTotalPages - 1} className="px-2.5 py-1 text-xs border rounded disabled:opacity-40">›</button>
                </div>
              )}
            </div>
          ) : (
            <div className="py-10 text-center text-gray-400 text-sm">Chưa có đánh giá</div>
          )}
        </div>

        {/* ═══════════ OTHER SHOP PRODUCTS ═══════════ */}
        {shopProducts.length > 0 && (
          <div className="bg-white mt-2 px-4 py-3">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-semibold text-gray-900">Sản Phẩm Khác Của Shop</h3>
              <Link to={`/shop/${product.sellerId}`} className="text-xs text-red-500 font-medium flex items-center gap-0.5">
                Xem tất cả <ChevronRight className="w-3.5 h-3.5" />
              </Link>
            </div>
            <div className="flex gap-2.5 overflow-x-auto pb-2 scrollbar-hide">
              {shopProducts.slice(0, 10).map((p) => (
                <MiniCard key={p.id} product={p} />
              ))}
            </div>
          </div>
        )}

        {/* ═══════════ SIMILAR PRODUCTS ═══════════ */}
        {similarProducts.length > 0 && (
          <div className="bg-white mt-2 px-4 py-3">
            <h3 className="text-sm font-semibold text-gray-900 mb-3">Sản Phẩm Tương Tự</h3>
            <div className="grid grid-cols-2 gap-2.5">
              {similarProducts.slice(0, 8).map((p) => (
                <MiniCard key={p.id} product={p} />
              ))}
            </div>
          </div>
        )}
      </div>

      {/* ═══════════ FLOATING BOTTOM BAR ═══════════ */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t shadow-lg z-40 px-4 py-2.5 flex gap-2.5 safe-area-pb">
        <button
          onClick={() => openVariantSheet("cart")}
          className="flex-1 flex items-center justify-center gap-1.5 py-3 border-2 border-red-500 text-red-500 rounded-lg font-semibold text-sm active:bg-red-50"
        >
          <ShoppingCart className="w-4 h-4" />
          Thêm vào giỏ
        </button>
        <button
          onClick={() => openVariantSheet("buy")}
          className="flex-1 flex items-center justify-center gap-1.5 py-3 bg-red-500 text-white rounded-lg font-semibold text-sm active:bg-red-600"
        >
          <Zap className="w-4 h-4" />
          Mua ngay
        </button>
      </div>

      {/* ═══════════ SPEC SHEET (half screen overlay) ═══════════ */}
      {showSpecSheet && (
        <div className="fixed inset-0 z-50 flex flex-col justify-end" onClick={() => setShowSpecSheet(false)}>
          {/* backdrop */}
          <div className="absolute inset-0 bg-black/40" />
          {/* sheet */}
          <div
            className="relative bg-white rounded-t-2xl max-h-[70vh] flex flex-col animate-slide-up"
            onClick={(e) => e.stopPropagation()}
          >
            {/* header */}
            <div className="flex items-center justify-between px-4 py-3 border-b shrink-0">
              <h3 className="text-base font-bold text-gray-900">Chi Tiết Sản Phẩm</h3>
              <button onClick={() => setShowSpecSheet(false)} className="p-1 hover:bg-gray-100 rounded-full">
                <X className="w-5 h-5 text-gray-500" />
              </button>
            </div>
            {/* content */}
            <div className="overflow-y-auto px-4 py-4">
              {/* breadcrumb */}
              <div className="text-xs text-gray-400 mb-3 flex items-center gap-1 flex-wrap">
                <span>Danh mục:</span>
                {parentCategory && (
                  <>
                    <Link to={`/search?category=${parentCategory.id}`} className="text-red-500 hover:underline">{parentCategory.name}</Link>
                    <ChevronRight className="w-3 h-3" />
                  </>
                )}
                {category && (
                  <Link to={`/search?category=${category.id}`} className="text-red-500 hover:underline">{category.name}</Link>
                )}
              </div>

              {/* specs table */}
              {product.specifications && product.specifications.length > 0 ? (
                <div className="divide-y border rounded-lg overflow-hidden">
                  {product.specifications.map((spec, idx) => (
                    <div key={spec.name} className={`flex text-sm ${idx % 2 === 0 ? "bg-gray-50" : "bg-white"}`}>
                      <span className="w-2/5 px-3 py-2.5 text-gray-500 text-xs font-medium">{spec.name}</span>
                      <span className="flex-1 px-3 py-2.5 text-gray-800 text-xs">{String(spec.value)}</span>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-gray-400 text-center py-6">Chưa có thông tin chi tiết.</p>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ═══════════ VARIANT SHEET (half screen overlay) ═══════════ */}
      {showVariantSheet && (
        <div className="fixed inset-0 z-50 flex flex-col justify-end" onClick={() => setShowVariantSheet(false)}>
          <div className="absolute inset-0 bg-black/40" />
          <div
            className="relative bg-white rounded-t-2xl max-h-[80vh] flex flex-col animate-slide-up"
            onClick={(e) => e.stopPropagation()}
          >
            {/* header: image + price + close */}
            <div className="px-4 pt-4 pb-3 border-b shrink-0">
              <div className="flex gap-3">
                <img
                  src={sheetImage}
                  alt={product.name}
                  className="w-24 h-24 rounded-lg object-cover border"
                />
                <div className="flex-1 min-w-0 flex flex-col justify-end">
                  <div className="mb-1">
                    {showPriceRange ? (
                      <span className="text-lg font-bold text-red-600">
                        {formatPrice(product.minPrice)} – {formatPrice(product.maxPrice)}
                      </span>
                    ) : (
                      <div className="flex items-end gap-2 flex-wrap">
                        <span className="text-xl font-bold text-red-600">{formatPrice(currentPrice)}</span>
                        {originalPrice != null && originalPrice > currentPrice && (
                          <span className="text-xs text-gray-400 line-through">{formatPrice(originalPrice)}</span>
                        )}
                      </div>
                    )}
                  </div>
                  {selectedSku && selectedStock && (
                    <p className="text-xs text-gray-500">Kho: {selectedStock.availableStock}</p>
                  )}
                  {selectedSku && (
                    <p className="text-xs text-gray-700 truncate mt-0.5">
                      {product.options?.map((o) => selectedOptions[o.name]).filter(Boolean).join(", ")}
                    </p>
                  )}
                </div>
                <button
                  onClick={() => setShowVariantSheet(false)}
                  className="self-start p-1 hover:bg-gray-100 rounded-full"
                >
                  <X className="w-5 h-5 text-gray-500" />
                </button>
              </div>
            </div>

            {/* option selectors */}
            <div className="overflow-y-auto px-4 py-4 flex-1">
              {product.options?.map((option) => (
                <div key={option.id} className="mb-5">
                  <span className="text-xs text-gray-500 mb-2 block font-medium">{option.name}</span>
                  <div className="flex flex-wrap gap-2">
                    {option.values.map((val) => {
                      const isActive = selectedOptions[option.name] === val.value;
                      const hasImage = !!val.imageUrl;
                      return (
                        <button
                          key={val.id}
                          onClick={() => handleOptionSelect(option.name, val.value)}
                          className={`flex items-center gap-1.5 px-3 py-2 rounded-lg border text-sm transition-all ${
                            isActive
                              ? "border-red-500 bg-red-50 text-red-600 ring-1 ring-red-500"
                              : "border-gray-200 text-gray-700 active:border-red-300"
                          }`}
                        >
                          {hasImage && (
                            <img src={val.imageUrl!} alt={val.value} className="w-6 h-6 rounded object-cover" />
                          )}
                          <span className="text-xs font-medium">{val.value}</span>
                        </button>
                      );
                    })}
                  </div>
                </div>
              ))}

              {/* quantity */}
              <div className="mb-2">
                <span className="text-xs text-gray-500 mb-2 block font-medium">Số lượng</span>
                <div className="flex items-center gap-3">
                  <div className="flex items-center border rounded-lg overflow-hidden">
                    <button
                      onClick={() => setQuantity(Math.max(1, quantity - 1))}
                      className="px-3 py-2 hover:bg-gray-50 active:bg-gray-100"
                    >
                      <Minus className="w-4 h-4" />
                    </button>
                    <input
                      type="number"
                      min={1}
                      max={availableQty}
                      value={quantity}
                      onChange={(e) => {
                        e.target.value = e.target.value.replace(/^0+(?=\d)/, "");
                        setQuantity(Math.max(1, parseInt(e.target.value) || 1));
                      }}
                      className="w-12 text-center py-2 border-x outline-none text-sm [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                    />
                    <button
                      onClick={() => setQuantity(Math.min(availableQty, quantity + 1))}
                      className="px-3 py-2 hover:bg-gray-50 active:bg-gray-100"
                    >
                      <Plus className="w-4 h-4" />
                    </button>
                  </div>
                  {selectedSku && selectedStock && (
                    <span className="text-xs text-gray-400">{selectedStock.availableStock} sản phẩm có sẵn</span>
                  )}
                </div>
              </div>
            </div>

            {/* action button */}
            <div className="px-4 py-3 border-t shrink-0 safe-area-pb">
              {variantAction === "cart" ? (
                <button
                  onClick={handleAddToCart}
                  disabled={addingToCart || !inStock || !allSelected}
                  className={`w-full flex items-center justify-center gap-2 py-3 rounded-lg font-semibold text-sm transition-all ${
                    !inStock
                      ? "bg-gray-200 text-gray-400 cursor-not-allowed"
                      : !allSelected
                        ? "bg-gray-200 text-gray-400 cursor-not-allowed"
                        : addingToCart
                        ? "bg-red-400 text-white cursor-wait"
                        : "bg-red-500 text-white active:bg-red-600"
                  }`}
                >
                  {addingToCart && <Loader2 className="w-4 h-4 animate-spin" />}
                  {!inStock ? "Hết hàng" : !allSelected ? "Vui lòng chọn phân loại" : addingToCart ? "Đang thêm..." : "Thêm vào giỏ hàng"}
                </button>
              ) : (
                <button
                  onClick={handleBuyNow}
                  disabled={addingToCart || !inStock || !allSelected}
                  className={`w-full flex items-center justify-center gap-2 py-3 rounded-lg font-semibold text-sm transition-all ${
                    !inStock
                      ? "bg-gray-200 text-gray-400 cursor-not-allowed"
                      : !allSelected
                        ? "bg-gray-200 text-gray-400 cursor-not-allowed"
                        : addingToCart
                        ? "bg-red-400 text-white cursor-wait"
                        : "bg-red-500 text-white active:bg-red-600"
                  }`}
                >
                  {addingToCart && <Loader2 className="w-4 h-4 animate-spin" />}
                  {!inStock ? "Hết hàng" : !allSelected ? "Vui lòng chọn phân loại" : addingToCart ? "Đang xử lý..." : "Mua ngay"}
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ── global animation style ── */}
      <style>{`
        @keyframes slide-up {
          from { transform: translateY(100%); }
          to   { transform: translateY(0); }
        }
        .animate-slide-up {
          animation: slide-up 0.3s ease-out;
        }
        .safe-area-pb {
          padding-bottom: max(0.625rem, env(safe-area-inset-bottom));
        }
        .scrollbar-hide {
          -ms-overflow-style: none;
          scrollbar-width: none;
        }
        .scrollbar-hide::-webkit-scrollbar {
          display: none;
        }
      `}</style>
    </>
  );
}

/* ─────────── Mini Product Card (horizontal scroll) ─────────── */

function getProductImage(product: ProductSummaryResponse | ProductResponse): string {
  if ("primaryImageUrl" in product && product.primaryImageUrl) return product.primaryImageUrl;
  if ("images" in product && Array.isArray(product.images) && product.images.length > 0) {
    const primary = product.images.find((i) => i.isPrimary);
    return primary ? primary.url : product.images[0].url;
  }
  return "https://placehold.co/300x300?text=No+Image";
}

function MiniCard({ product }: { product: ProductSummaryResponse | ProductResponse }) {
  const navigate = useNavigate();
  return (
    <div
      onClick={() => { navigate(`/product/${product.id}`); window.scrollTo(0, 0); }}
      className="cursor-pointer w-[140px] shrink-0 bg-white rounded-lg border border-gray-100 overflow-hidden"
    >
      <div className="aspect-square bg-gray-50 overflow-hidden">
        <img
          src={getProductImage(product)}
          alt={product.name}
          className="w-full h-full object-cover"
          loading="lazy"
        />
      </div>
      <div className="p-2">
        <h4 className="text-[11px] text-gray-800 line-clamp-2 leading-tight mb-1">{product.name}</h4>
        <span className="text-xs font-bold text-red-600">{formatPrice(product.minPrice)}</span>
        <div className="flex items-center justify-between mt-0.5 text-[10px] text-gray-400">
          {product.ratingAvg != null && (
            <div className="flex items-center gap-0.5">
              <Star className="w-2.5 h-2.5 text-yellow-400 fill-yellow-400" />
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
