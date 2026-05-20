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
  Store,
  MessageSquare,
  Package,
  Users,
  Truck,
  ShieldCheck,
  ChevronDown,
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
import type { CartBySellerResponse, CartItemResponse } from "../types/cart";

/* ────────────────── tiny helpers ────────────────── */

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

/* ────────────────── MAIN COMPONENT ────────────────── */

export default function ProductDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  /* ─── state ─── */
  const [product, setProduct] = useState<ProductResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // category breadcrumb
  const [category, setCategory] = useState<PublicCategoryResponse | null>(null);
  const [parentCategory, setParentCategory] = useState<PublicCategoryResponse | null>(null);

  // seller
  const [seller, setSeller] = useState<SellerResponse | null>(null);

  // image gallery
  const [selectedImage, setSelectedImage] = useState(0);
  const thumbRef = useRef<HTMLDivElement>(null);
  const imageRef = useRef<HTMLDivElement>(null);
  const mainImageRef = useRef<HTMLImageElement>(null);
  const [zoomActive, setZoomActive] = useState(false);
  const [zoomPoint, setZoomPoint] = useState({ x: 0.5, y: 0.5 });
  const [lensPos, setLensPos] = useState({ left: 0, top: 0 });

  // variant selection
  const [selectedOptions, setSelectedOptions] = useState<Record<string, string>>({});
  const [allSelected, setAllSelected] = useState(false);

  // quantity & cart
  const [quantity, setQuantity] = useState(1);
  const [addingToCart, setAddingToCart] = useState(false);
  const [addedToCart, setAddedToCart] = useState(false);

  // related products
  const [shopProducts, setShopProducts] = useState<ProductSummaryResponse[]>([]);
  const [similarProducts, setSimilarProducts] = useState<ProductSummaryResponse[]>([]);

  // inventory (stock availability per SKU)
  const [stockMap, setStockMap] = useState<Record<string, StockAvailabilityResponse>>({});

  // reviews
  const [reviews, setReviews] = useState<ReviewResponse[]>([]);
  const [reviewSummary, setReviewSummary] = useState<ReviewSummaryResponse | null>(null);
  const [reviewFilter, setReviewFilter] = useState<string>("all");
  const [reviewPage, setReviewPage] = useState(0);
  const [reviewTotalPages, setReviewTotalPages] = useState(0);
  const [reviewLoading, setReviewLoading] = useState(false);

  /* ─── fetch product ─── */
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

        // don't pre-select options – let user choose
        if (!p.options || p.options.length === 0) {
          setAllSelected(true);
        }

        // fetch category chain
        if (p.categoryId) {
          getCategoryById(p.categoryId)
            .then((cRes) => {
              const cat = cRes.data.result;
              setCategory(cat);
              if (cat.parentId) {
                getCategoryById(cat.parentId)
                  .then((pRes) => setParentCategory(pRes.data.result))
                  .catch(() => {});
              }
            })
            .catch(() => {});
        }

        // fetch seller
        if (p.sellerId) {
          getSellerPublic(p.sellerId)
            .then((sRes) => setSeller(sRes.data.result))
            .catch(() => {});
        }

        // fetch inventory for all SKUs
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

        // shop products
        getProductsBySeller(p.sellerId, { page: 0, size: 12 })
          .then((r) => setShopProducts(r.data.result.content.filter((x) => x.id !== p.id)))
          .catch(() => {});

        // similar products (same category)
        if (p.categoryId) {
          getProductsByCategory(p.categoryId, { page: 0, size: 12 })
            .then((r) => setSimilarProducts(r.data.result.content.filter((x) => x.id !== p.id)))
            .catch(() => {});
        }
      })
      .catch(() => setError("Không tìm thấy sản phẩm"))
      .finally(() => setLoading(false));
  }, [id]);

  /* ─── fetch reviews ─── */
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
    [id]
  );

  useEffect(() => {
    if (!id) return;
    getReviewSummary(id)
      .then((res) => setReviewSummary(res.data.result))
      .catch(() => {});
    fetchReviews("all", 0);
  }, [id, fetchReviews]);

  const handleReviewFilterChange = (filter: string) => {
    setReviewFilter(filter);
    setReviewPage(0);
    fetchReviews(filter, 0);
  };

  const handleReviewPageChange = (newPage: number) => {
    setReviewPage(newPage);
    fetchReviews(reviewFilter, newPage);
  };

  /* ─── derived: selected SKU ─── */
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

  /* ─── image nav (merge product gallery + unique option value images) ─── */
  const images = useMemo(() => {
    const base = product?.images?.length
      ? [...product.images]
      : [{ id: "0", url: "https://placehold.co/600x600?text=No+Image", isPrimary: true, sortOrder: 0 }];
    if (!product?.options?.length) return base;
    const existingUrls = new Set(base.map((img) => img.url));
    // Add images from first option's values
    const firstOption = product.options[0];
    if (firstOption) {
      firstOption.values.forEach((val) => {
        if (val.imageUrl && !existingUrls.has(val.imageUrl)) {
          existingUrls.add(val.imageUrl);
          base.push({ id: `optval-${val.id}`, url: val.imageUrl, isPrimary: false, sortOrder: base.length });
        }
      });
    }
    return base;
  }, [product]);

  /* ─── variant image switching ─── */
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

  /* ─── handlers ─── */
  const handleOptionSelect = useCallback(
    (optionName: string, value: string) => {
      setSelectedOptions((prev) => {
        const next = { ...prev };
        if (prev[optionName] === value) {
          delete next[optionName];
        } else {
          next[optionName] = value;
        }
        if (product?.options) {
          const full = product.options.every((opt) => !!next[opt.name]);
          setAllSelected(full);
        }
        return next;
      });
    },
    [product]
  );

  const handleAddToCart = async () => {
    if (!isAuthenticated) { navigate("/login"); return; }
    if (!product || !selectedSku) {
      toast.error("Vui lòng chọn phân loại sản phẩm");
      return;
    }
    setAddingToCart(true);
    try {
      await addToCart({ skuId: selectedSku.id, productId: product.id, sellerId: product.sellerId, quantity });
      toast.success(`Đã thêm ${quantity} sản phẩm vào giỏ hàng`);
      setAddedToCart(true);
      setTimeout(() => setAddedToCart(false), 2000);
      window.dispatchEvent(new Event("cart-updated"));
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || "Không thể thêm vào giỏ hàng";
      toast.error(msg);
    } finally {
      setAddingToCart(false);
    }
  };

  const handleBuyNow = () => {
    if (!isAuthenticated) { navigate("/login"); return; }
    if (!product || !selectedSku) {
      toast.error("Vui lòng chọn phân loại sản phẩm");
      return;
    }

    // Create a temporary cart item for this product
    const cartItem: CartItemResponse = {
      id: `temp-${Date.now()}`,
      skuId: selectedSku.id,
      productId: product.id,
      sellerId: product.sellerId,
      quantity,
      price: selectedSku.price,
      subtotal: selectedSku.price * quantity,
      selected: true,
      createdAt: new Date().toISOString(),
      productName: product.name,
      skuCode: selectedSku.skuCode || "",
      imageUrl: product.images?.[0]?.url || null,
      sellerName: seller?.shopName || null,
      availableStock: stockMap[selectedSku.id]?.availableStock || 0,
      inStock: stockMap[selectedSku.id]?.availableStock > 0,
      attributes: selectedSku.attributes?.reduce((acc, attr) => {
        acc[attr.optionName] = attr.valueName;
        return acc;
      }, {} as Record<string, string>) || undefined,
    };

    // Create a temporary cart by seller response
    const buyNowCart: CartBySellerResponse = {
      sellerId: product.sellerId,
      sellerName: seller?.shopName || "Seller",
      items: [cartItem],
      subtotal: cartItem.subtotal,
      itemCount: 1,
      allSelected: true,
    };

    // Navigate to checkout with buy now data
    navigate("/checkout", { state: { buyNowCart } });
  };

  const handleStartChat = () => {
    if (!isAuthenticated) {
      toast.error("Vui lòng đăng nhập để chat với shop");
      navigate("/login");
      return;
    }
    if (!product) {
      toast.error("Không tìm thấy thông tin sản phẩm");
      return;
    }
    const peerId = seller?.userId ?? product.sellerId;
    navigate(`/user/messages?peerId=${encodeURIComponent(peerId)}`);
  };

  const handleImageMouseEnter = () => {
    setZoomActive(true);
  };

  const handleImageMouseLeave = () => {
    setZoomActive(false);
  };

  const handleImageMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!imageRef.current) return;
    const rect = imageRef.current.getBoundingClientRect();
    const cursorX = e.clientX - rect.left;
    const cursorY = e.clientY - rect.top;

    const imageEl = mainImageRef.current;
    if (!imageEl || !imageEl.naturalWidth || !imageEl.naturalHeight) {
      const x = Math.max(0, Math.min(1, cursorX / rect.width));
      const y = Math.max(0, Math.min(1, cursorY / rect.height));
      setZoomPoint({ x, y });
      return;
    }

    const containerAspect = rect.width / rect.height;
    const imageAspect = imageEl.naturalWidth / imageEl.naturalHeight;

    let renderWidth = rect.width;
    let renderHeight = rect.height;
    let offsetX = 0;
    let offsetY = 0;

    if (imageAspect > containerAspect) {
      renderHeight = rect.width / imageAspect;
      offsetY = (rect.height - renderHeight) / 2;
    } else {
      renderWidth = rect.height * imageAspect;
      offsetX = (rect.width - renderWidth) / 2;
    }

    const clampedX = Math.max(offsetX, Math.min(offsetX + renderWidth, cursorX));
    const clampedY = Math.max(offsetY, Math.min(offsetY + renderHeight, cursorY));

    const relX = (clampedX - offsetX) / renderWidth;
    const relY = (clampedY - offsetY) / renderHeight;
    setZoomPoint({ x: relX, y: relY });

    const lensSize = 96;
    const half = lensSize / 2;
    const lensLeft = Math.max(offsetX, Math.min(offsetX + renderWidth - lensSize, clampedX - half));
    const lensTop = Math.max(offsetY, Math.min(offsetY + renderHeight - lensSize, clampedY - half));
    setLensPos({ left: lensLeft, top: lensTop });
  };

  // scroll thumbnails when selected image changes
  useEffect(() => {
    if (!thumbRef.current) return;
    const child = thumbRef.current.children[selectedImage] as HTMLElement | undefined;
    child?.scrollIntoView({ behavior: "smooth", inline: "center", block: "nearest" });
  }, [selectedImage]);

  /* ────────────────── LOADING ────────────────── */
  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-8">
        <div className="animate-pulse">
          <div className="h-4 bg-gray-200 rounded w-1/3 mb-6" />
          <div className="grid grid-cols-1 lg:grid-cols-5 gap-8">
            <div className="lg:col-span-2 bg-gray-200 rounded-lg aspect-square" />
            <div className="lg:col-span-3 space-y-4">
              <div className="h-8 bg-gray-200 rounded w-3/4" />
              <div className="h-5 bg-gray-200 rounded w-1/2" />
              <div className="h-14 bg-gray-200 rounded w-2/5" />
              <div className="h-32 bg-gray-200 rounded" />
            </div>
          </div>
        </div>
      </div>
    );
  }

  /* ────────────────── ERROR ────────────────── */
  if (error || !product) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-20 text-center">
        <h2 className="text-2xl font-bold text-gray-800 mb-4">{error || "Sản phẩm không tồn tại"}</h2>
        <Link to="/" className="text-red-500 hover:text-red-600 font-medium">Quay về trang chủ</Link>
      </div>
    );
  }

  /* ─── price display ─── */
  const showPriceRange = !allSelected && product.minPrice !== product.maxPrice;
  const currentPrice = selectedSku?.price ?? product.minPrice;
  const originalPrice = selectedSku?.originalPrice;
  const discountPercent =
    originalPrice && originalPrice > currentPrice
      ? Math.round(((originalPrice - currentPrice) / originalPrice) * 100)
      : 0;
  const selectedStock = selectedSku ? stockMap[selectedSku.id] : null;
  const stockLoaded = Object.keys(stockMap).length > 0;
  const allOutOfStock = stockLoaded && product.skus?.length
    ? product.skus.every((sku) => {
        const st = stockMap[sku.id];
        return st ? st.availableStock <= 0 : false;
      })
    : false;
  const inStock = allOutOfStock
    ? false
    : selectedSku
      ? selectedSku.status === "ACTIVE" && (selectedStock ? selectedStock.availableStock > 0 : true)
      : true;
  const availableQty = selectedStock?.availableStock ?? 999;

  return (
    <div className="bg-gray-100 min-h-screen pb-10">
      <div className="max-w-7xl mx-auto px-0 sm:px-4 pt-4">
        {/* ═══════════════ BREADCRUMB ═══════════════ */}
        <nav className="flex items-center flex-wrap text-sm text-gray-500 mb-4 bg-white rounded px-4 py-2">
          <Link to="/" className="hover:text-red-500 transition-colors">Trang chủ</Link>

          {parentCategory && (
            <>
              <ChevronRight className="w-4 h-4 mx-1 text-gray-400 flex-shrink-0" />
              <Link
                to={`/search?category=${parentCategory.id}`}
                className="hover:text-red-500 transition-colors"
              >
                {parentCategory.name}
              </Link>
            </>
          )}

          {category && (
            <>
              <ChevronRight className="w-4 h-4 mx-1 text-gray-400 flex-shrink-0" />
              <Link
                to={`/search?category=${category.id}`}
                className="hover:text-red-500 transition-colors"
              >
                {category.name}
              </Link>
            </>
          )}

          <ChevronRight className="w-4 h-4 mx-1 text-gray-400 flex-shrink-0" />
          <span className="text-gray-800 font-medium truncate max-w-xs">{product.name}</span>
        </nav>

        {/* ═══════════════ TOP: IMAGE + INFO ═══════════════ */}
        <div className="bg-white rounded-lg p-4 lg:p-6 grid grid-cols-1 lg:grid-cols-5 gap-6">
          {/* ── Left: Image Gallery ── */}
          <div className="lg:col-span-2">
            {/* main image */}
            <div className="relative">
              <div
                ref={imageRef}
                className="relative bg-gray-50 rounded-lg overflow-hidden border aspect-square group"
                onMouseEnter={handleImageMouseEnter}
                onMouseLeave={handleImageMouseLeave}
                onMouseMove={handleImageMouseMove}
              >
                <img
                  ref={mainImageRef}
                  src={images[selectedImage]?.url}
                  alt={product.name}
                  className="w-full h-full object-contain"
                />

                {/* Zoom lens indicator (only show on desktop when zooming) */}
                {zoomActive && (
                  <div
                    className="absolute hidden lg:block w-24 h-24 border-2 border-red-500 pointer-events-none bg-black/5"
                    style={{ left: lensPos.left, top: lensPos.top }}
                  />
                )}

                {images.length > 1 && (
                  <>
                    <button
                      onClick={() => setSelectedImage((prev) => (prev - 1 + images.length) % images.length)}
                      className="absolute left-2 top-1/2 -translate-y-1/2 bg-black/40 hover:bg-black/60 text-white rounded-full p-1.5 opacity-0 group-hover:opacity-100 transition-opacity"
                    >
                      <ChevronLeft className="w-5 h-5" />
                    </button>
                    <button
                      onClick={() => setSelectedImage((prev) => (prev + 1) % images.length)}
                      className="absolute right-2 top-1/2 -translate-y-1/2 bg-black/40 hover:bg-black/60 text-white rounded-full p-1.5 opacity-0 group-hover:opacity-100 transition-opacity"
                    >
                      <ChevronRight className="w-5 h-5" />
                    </button>
                  </>
                )}
              </div>

              {/* Zoom preview (desktop only) */}
              {zoomActive && (
                <div className="hidden lg:block absolute top-0 left-full ml-3 w-80 h-80 rounded-lg bg-white border border-gray-200 shadow-lg overflow-hidden pointer-events-none z-20">
                  <div
                    className="w-full h-full bg-no-repeat"
                    style={{
                      backgroundImage: `url(${images[selectedImage]?.url})`,
                      backgroundPosition: `${zoomPoint.x * 100}% ${zoomPoint.y * 100}%`,
                      backgroundSize: "250% 250%",
                    }}
                  />
                </div>
              )}
            </div>

            {/* thumbnails */}
            {images.length > 1 && (
              <div className="relative mt-3">
                <div ref={thumbRef} className="flex gap-2 overflow-x-auto scrollbar-hide">
                  {images.map((img, idx) => (
                    <button
                      key={img.id}
                      onClick={() => setSelectedImage(idx)}
                      className={`flex-shrink-0 w-16 h-16 rounded border-2 overflow-hidden transition-colors ${
                        selectedImage === idx ? "border-red-500" : "border-transparent hover:border-red-300"
                      }`}
                    >
                      <img src={img.url} alt="" className="w-full h-full object-cover" />
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* ── Right: Product Info ── */}
          <div className="lg:col-span-3 flex flex-col">
            {/* name */}
            <h1 className="text-xl lg:text-2xl font-bold text-gray-900 leading-snug mb-2">{product.name}</h1>

            {/* rating line */}
            <div className="flex items-center flex-wrap gap-x-4 gap-y-1 text-sm mb-4">
              {product.ratingAvg != null && (
                <div className="flex items-center gap-1.5">
                  <span className="text-red-500 font-semibold border-b border-red-500">
                    {product.ratingAvg.toFixed(1)}
                  </span>
                  <StarRating rating={product.ratingAvg} size={14} />
                </div>
              )}
              {product.ratingCount != null && (
                <div className="flex items-center gap-1 text-gray-500">
                  <span className="font-semibold text-gray-800">{formatCount(product.ratingCount)}</span>
                  <span>Đánh Giá</span>
                </div>
              )}
              {product.soldCount != null && product.soldCount > 0 && (
                <div className="flex items-center gap-1 text-gray-500">
                  <span className="font-semibold text-gray-800">{formatCount(product.soldCount)}</span>
                  <span>Đã Bán</span>
                </div>
              )}
            </div>

            {/* price block */}
            <div className="bg-gray-50 rounded-lg px-5 py-4 mb-5">
              {showPriceRange ? (
                <span className="text-2xl font-bold text-red-600">
                  {formatPrice(product.minPrice)} – {formatPrice(product.maxPrice)}
                </span>
              ) : (
                <div className="flex items-end gap-3">
                  <span className="text-3xl font-bold text-red-600">{formatPrice(currentPrice)}</span>
                  {originalPrice != null && originalPrice > currentPrice && (
                    <>
                      <span className="text-base text-gray-400 line-through">{formatPrice(originalPrice)}</span>
                      <span className="bg-red-100 text-red-600 text-xs font-bold px-2 py-0.5 rounded">
                        -{discountPercent}%
                      </span>
                    </>
                  )}
                </div>
              )}
            </div>

            {/* variant options */}
            {product.options?.map((option) => (
              <div key={option.id} className="mb-4">
                <span className="text-sm text-gray-500 mb-2 block">{option.name}</span>
                <div className="flex flex-wrap gap-2">
                  {option.values.map((val) => {
                    const isActive = selectedOptions[option.name] === val.value;
                    return (
                      <button
                        key={val.id}
                        onClick={() => handleOptionSelect(option.name, val.value)}
                        className={`px-4 py-2 rounded border text-sm font-medium transition-all ${
                          isActive
                            ? "border-red-500 bg-red-50 text-red-600"
                            : "border-gray-300 hover:border-red-400 text-gray-700"
                        }`}
                      >
                        {val.value}
                      </button>
                    );
                  })}
                </div>
              </div>
            ))}

            {/* quantity */}
            <div className="mb-5">
              <span className="text-sm text-gray-500 mb-2 block">Số Lượng</span>
              <div className="flex items-center gap-3">
                <div className="flex items-center border rounded">
                  <button
                    onClick={() => setQuantity(Math.max(1, quantity - 1))}
                    className="px-3 py-2 hover:bg-gray-100 transition-colors"
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
                    className="w-14 text-center py-2 border-x outline-none [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                  />
                  <button
                    onClick={() => setQuantity(Math.min(availableQty, quantity + 1))}
                    className="px-3 py-2 hover:bg-gray-100 transition-colors"
                  >
                    <Plus className="w-4 h-4" />
                  </button>
                </div>
                {selectedSku && selectedStock && (
                  <span className="text-sm text-gray-500">{selectedStock.availableStock} sản phẩm có sẵn</span>
                )}
              </div>
            </div>

            {/* ── Shipping info ── */}
            <div className="mb-4 text-sm">
              <div className="flex gap-x-6">
                <span className="text-gray-500 w-[110px] shrink-0">Vận Chuyển</span>
                <div className="flex-1">
                  <div className="flex items-center gap-2 text-gray-700 mb-1">
                    <Truck className="w-5 h-5 text-teal-500 shrink-0" />
                    <span>
                      Nhận từ{" "}
                      <span className="font-medium">
                        {new Date(Date.now() + 2 * 86400000).toLocaleDateString("vi-VN", { day: "2-digit", month: "short" })}
                        {" - "}
                        {new Date(Date.now() + 6 * 86400000).toLocaleDateString("vi-VN", { day: "2-digit", month: "short" })}
                      </span>
                    </span>
                    <ChevronRight className="w-4 h-4 text-gray-400" />
                  </div>
                  <div className="text-teal-600 font-medium mb-0.5">Phí ship 0₫</div>
                  <div className="text-xs text-gray-400">Tặng Voucher 15.000₫ nếu đơn giao sau thời gian trên.</div>
                </div>
              </div>
            </div>

            {/* ── Return policy ── */}
            <div className="mb-5 text-sm">
              <div className="flex gap-x-6">
                <span className="text-gray-500 w-[110px] shrink-0 leading-snug">An Tâm Mua Sắm Cùng Sellico</span>
                <div className="flex-1">
                  <div className="flex items-center gap-2 text-gray-700">
                    <ShieldCheck className="w-5 h-5 text-red-500 shrink-0" />
                    <span>
                      Trả hàng miễn phí 15 ngày
                    </span>
                    <ChevronDown className="w-4 h-4 text-gray-400" />
                  </div>
                </div>
              </div>
            </div>

            {/* action buttons */}
            <div className="flex gap-3 mt-auto">
              <button
                onClick={handleAddToCart}
                disabled={addingToCart || !inStock}
                className={`flex-1 flex items-center justify-center gap-2 px-6 py-3 border-2 rounded font-semibold transition-all ${
                  addedToCart
                    ? "border-green-500 bg-green-50 text-green-600"
                    : !inStock
                    ? "border-gray-300 bg-gray-100 text-gray-400 cursor-not-allowed"
                    : addingToCart
                    ? "border-red-300 text-red-400 bg-red-50 cursor-wait"
                    : "border-red-500 text-red-500 hover:bg-red-50"
                }`}
              >
                {addingToCart ? (
                  <Loader2 className="w-5 h-5 animate-spin" />
                ) : addedToCart ? null : (
                  <ShoppingCart className="w-5 h-5" />
                )}
                {addedToCart ? "Đã thêm ✓" : addingToCart ? "Đang thêm..." : "Thêm Vào Giỏ Hàng"}
              </button>
              <button
                onClick={handleBuyNow}
                disabled={addingToCart || !inStock}
                className={`flex-1 flex items-center justify-center gap-2 py-3 rounded font-semibold transition-all ${
                  !inStock ? "bg-gray-300 text-gray-500 cursor-not-allowed" : "bg-red-500 text-white hover:bg-red-600"
                }`}
              >
                {inStock ? "Mua Ngay" : "Hết hàng"}
              </button>
            </div>
          </div>
        </div>

        {/* ═══════════════ SHOP INFO ═══════════════ */}
        <div className="bg-white rounded-lg mt-4 p-4 lg:p-6">
          <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4">
            {/* logo + name */}
            <div className="flex items-center gap-4 flex-shrink-0">
              {seller?.logoUrl ? (
                <img
                  src={seller.logoUrl}
                  alt={seller.shopName}
                  className="w-16 h-16 rounded-full object-cover border"
                />
              ) : (
                <div className="w-16 h-16 rounded-full bg-red-100 flex items-center justify-center flex-shrink-0">
                  <Store className="w-8 h-8 text-red-500" />
                </div>
              )}
              <div>
                <p className="font-semibold text-gray-900 text-base">
                  {seller?.shopName ?? `Shop #${product.sellerId.slice(0, 8)}`}
                </p>
                <div className="flex items-center gap-2 mt-1">
                  <Link
                    to={`/shop/${product.sellerId}`}
                    className="text-xs border border-red-500 text-red-500 rounded px-3 py-1 hover:bg-red-50 transition-colors"
                  >
                    <Store className="w-3 h-3 inline mr-1" />
                    Xem Shop
                  </Link>
                  <button
                    onClick={handleStartChat}
                    className="text-xs border border-blue-500 text-blue-600 rounded px-3 py-1 hover:bg-blue-50 transition-colors"
                  >
                    <MessageSquare className="w-3 h-3 inline mr-1" />
                    Chat
                  </button>
                </div>
              </div>
            </div>

            {/* stats */}
            <div className="flex flex-wrap gap-x-6 gap-y-2 sm:ml-auto text-sm text-gray-600">
              {seller?.ratingAvg != null && (
                <div className="flex items-center gap-1.5">
                  <Star className="w-4 h-4 text-yellow-400" />
                  <span>
                    Đánh Giá: <span className="text-red-500 font-semibold">{seller.ratingAvg.toFixed(1)}</span>
                  </span>
                </div>
              )}
              {seller?.totalProducts != null && (
                <div className="flex items-center gap-1.5">
                  <Package className="w-4 h-4 text-gray-400" />
                  <span>
                    Sản Phẩm: <span className="text-red-500 font-semibold">{formatCount(seller.totalProducts)}</span>
                  </span>
                </div>
              )}
              {seller?.followerCount != null && (
                <div className="flex items-center gap-1.5">
                  <Users className="w-4 h-4 text-gray-400" />
                  <span>
                    Người Theo Dõi:{" "}
                    <span className="text-red-500 font-semibold">{formatCount(seller.followerCount)}</span>
                  </span>
                </div>
              )}
              {seller?.ratingCount != null && (
                <div className="flex items-center gap-1.5">
                  <MessageSquare className="w-4 h-4 text-gray-400" />
                  <span>
                    Lượt Đánh Giá: <span className="text-red-500 font-semibold">{formatCount(seller.ratingCount)}</span>
                  </span>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* ═══════════════ PRODUCT DETAILS (specs) ═══════════════ */}
        <div className="bg-white rounded-lg mt-4 p-4 lg:p-6">
          <h2 className="text-lg font-bold text-gray-900 mb-4 uppercase">Chi Tiết Sản Phẩm</h2>

          {/* specifications table */}
          {product.specifications && product.specifications.length > 0 && (
            <div className="mb-6">
              <div className="divide-y border rounded">
                {product.specifications.map((spec, idx) => (
                  <div key={spec.name} className={`flex text-sm ${idx % 2 === 0 ? "bg-gray-50" : "bg-white"}`}>
                    <span className="w-2/5 lg:w-1/4 px-4 py-3 text-gray-500 font-medium">{spec.name}</span>
                    <span className="flex-1 px-4 py-3 text-gray-800">{String(spec.value)}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* description */}
          <h3 className="text-base font-bold text-gray-900 mb-3 uppercase">Mô Tả Sản Phẩm</h3>
          {product.description ? (
            <div className="prose max-w-none text-gray-700 whitespace-pre-line text-sm leading-relaxed">
              {product.description}
            </div>
          ) : (
            <p className="text-sm text-gray-400 italic">Chưa có mô tả sản phẩm.</p>
          )}
        </div>

        {/* ═══════════════ REVIEWS (Shopee-style) ═══════════════ */}
        <div className="bg-white rounded-lg mt-4 p-4 lg:p-6">
          <h2 className="text-lg font-bold text-gray-900 mb-4 uppercase">Đánh Giá Sản Phẩm</h2>

          {/* ── Rating overview bar ── */}
          <div className="bg-[#fffbf8] border border-[#f9ede5] rounded-sm p-5 lg:p-7 flex flex-col md:flex-row gap-6 mb-6">
            {/* Left: big score */}
            <div className="flex flex-col items-center justify-center min-w-[140px]">
              <div className="text-red-500">
                <span className="text-3xl font-semibold">{reviewSummary?.ratingAvg?.toFixed(1) ?? "0"}</span>
                <span className="text-lg text-red-500"> trên 5</span>
              </div>
              <div className="flex items-center gap-0.5 mt-1">
                <StarRating rating={reviewSummary?.ratingAvg ?? 0} size={20} />
              </div>
            </div>

            {/* Right: filter buttons */}
            <div className="flex flex-wrap gap-2 items-center">
              {[
                { key: "all", label: `Tất Cả` },
                { key: "star-5", label: `5 Sao (${reviewSummary?.ratingDistribution?.[5] ?? 0})` },
                { key: "star-4", label: `4 Sao (${reviewSummary?.ratingDistribution?.[4] ?? 0})` },
                { key: "star-3", label: `3 Sao (${reviewSummary?.ratingDistribution?.[3] ?? 0})` },
                { key: "star-2", label: `2 Sao (${reviewSummary?.ratingDistribution?.[2] ?? 0})` },
                { key: "star-1", label: `1 Sao (${reviewSummary?.ratingDistribution?.[1] ?? 0})` },
                { key: "comment", label: `Có Bình Luận (${reviewSummary?.withCommentCount ?? 0})` },
                { key: "images", label: `Có Hình Ảnh / Video (${reviewSummary?.withImagesCount ?? 0})` },
              ].map(({ key, label }) => (
                <button
                  key={key}
                  onClick={() => handleReviewFilterChange(key)}
                  className={`px-4 py-2 text-sm border rounded-sm transition-colors whitespace-nowrap ${
                    reviewFilter === key
                      ? "border-red-500 text-red-500 bg-white"
                      : "border-gray-300 text-gray-600 bg-white hover:border-gray-400"
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>
          </div>

          {/* ── Rating bars (star distribution) ── */}
          <div className="space-y-2.5 mb-8">
            {[5, 4, 3, 2, 1].map((star) => {
              const count = reviewSummary?.ratingDistribution?.[star] ?? 0;
              const total = reviewSummary?.totalCount ?? 0;
              const pct = total > 0 ? (count / total) * 100 : 0;
              return (
                <div key={star} className="flex items-center gap-3 text-sm">
                  <div className="flex items-center gap-1 w-[70px] justify-end text-gray-600">
                    <span>{star}</span>
                    <Star className="w-3.5 h-3.5 text-yellow-400 fill-yellow-400" />
                  </div>
                  <div className="flex-1 h-2.5 bg-gray-100 rounded-full overflow-hidden">
                    <div className="h-full bg-red-500 rounded-full transition-all" style={{ width: `${pct}%` }} />
                  </div>
                  <span className="w-[32px] text-xs text-gray-400 text-right">{count}</span>
                </div>
              );
            })}
          </div>

          {/* ── Review list ── */}
          {reviewLoading ? (
            <div className="py-12 flex justify-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-red-500" />
            </div>
          ) : reviews.length > 0 ? (
            <div className="divide-y">
              {reviews.map((review) => (
                <div key={review.id} className="py-4 flex gap-3">
                  {/* Avatar */}
                  <div className="flex-shrink-0">
                    {review.anonymous ? (
                      <div className="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center text-gray-500 text-sm font-semibold">
                        *
                      </div>
                    ) : review.userAvatar ? (
                      <img src={review.userAvatar} alt="" className="w-10 h-10 rounded-full object-cover" />
                    ) : (
                      <div className="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center text-gray-500 text-sm font-semibold">
                        {(review.userName ?? "U").charAt(0).toUpperCase()}
                      </div>
                    )}
                  </div>
                  {/* Content */}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-800">
                      {review.anonymous ? "*****" : (review.userName ?? "Người dùng")}
                    </p>
                    <div className="flex items-center gap-1 mt-0.5">
                      <StarRating rating={review.rating} size={14} />
                    </div>
                    <p className="text-xs text-gray-400 mt-1">
                      {new Date(review.createdAt).toLocaleDateString("vi-VN", { year: "numeric", month: "2-digit", day: "2-digit" })}
                    </p>
                    {review.comment && (
                      <p className="text-sm text-gray-700 mt-2 whitespace-pre-line">{review.comment}</p>
                    )}
                    {/* Images */}
                    {review.images && review.images.length > 0 && (
                      <div className="flex gap-2 mt-2 flex-wrap">
                        {review.images.map((img, i) => (
                          <img key={i} src={img} alt="" className="w-20 h-20 object-cover rounded border cursor-pointer hover:opacity-80" />
                        ))}
                      </div>
                    )}
                    {/* Seller reply */}
                    {review.reply && (
                      <div className="mt-3 bg-gray-50 rounded p-3 border-l-2 border-red-400">
                        <p className="text-xs font-semibold text-gray-600 mb-1">Phản Hồi Của Người Bán</p>
                        <p className="text-sm text-gray-700 whitespace-pre-line">{review.reply.comment}</p>
                      </div>
                    )}
                  </div>
                </div>
              ))}

              {/* Pagination */}
              {reviewTotalPages > 1 && (
                <div className="flex justify-center gap-2 pt-4">
                  <button
                    onClick={() => handleReviewPageChange(reviewPage - 1)}
                    disabled={reviewPage === 0}
                    className="px-3 py-1.5 text-sm border rounded disabled:opacity-40 hover:bg-gray-50"
                  >
                    ‹
                  </button>
                  {Array.from({ length: reviewTotalPages }, (_, i) => (
                    <button
                      key={i}
                      onClick={() => handleReviewPageChange(i)}
                      className={`px-3 py-1.5 text-sm border rounded ${
                        i === reviewPage ? "border-red-500 text-red-500 bg-red-50" : "hover:bg-gray-50"
                      }`}
                    >
                      {i + 1}
                    </button>
                  ))}
                  <button
                    onClick={() => handleReviewPageChange(reviewPage + 1)}
                    disabled={reviewPage >= reviewTotalPages - 1}
                    className="px-3 py-1.5 text-sm border rounded disabled:opacity-40 hover:bg-gray-50"
                  >
                    ›
                  </button>
                </div>
              )}
            </div>
          ) : (
            /* ── Empty state ── */
            <div className="py-16 flex flex-col items-center justify-center text-gray-400">
              <svg className="w-24 h-24 mb-4 text-gray-200" viewBox="0 0 120 120" fill="none">
                <rect x="15" y="30" width="90" height="70" rx="4" stroke="currentColor" strokeWidth="2" fill="none" />
                <path d="M15 45h90" stroke="currentColor" strokeWidth="2" />
                <rect x="25" y="55" width="30" height="6" rx="2" fill="currentColor" opacity="0.3" />
                <rect x="25" y="67" width="50" height="4" rx="2" fill="currentColor" opacity="0.2" />
                <rect x="25" y="77" width="40" height="4" rx="2" fill="currentColor" opacity="0.2" />
                <circle cx="90" cy="25" r="18" stroke="currentColor" strokeWidth="2" fill="#fff" />
                <path d="M84 25l4 4 8-8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
              <p className="text-base font-medium text-gray-500">Chưa có đánh giá</p>
            </div>
          )}
        </div>

        {/* ═══════════════ OTHER PRODUCTS FROM SHOP ═══════════════ */}
        {shopProducts.length > 0 && (
          <div className="bg-white rounded-lg mt-4 p-4 lg:p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold text-gray-900 uppercase">Các Sản Phẩm Khác Của Shop</h2>
              <Link
                to={`/shop/${product.sellerId}`}
                className="text-sm text-red-500 hover:text-red-600 font-medium flex items-center gap-0.5"
              >
                Xem Tất Cả
                <ChevronRight className="w-4 h-4" />
              </Link>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
              {shopProducts.slice(0, 6).map((p) => (
                <MiniProductCard key={p.id} product={p} />
              ))}
            </div>
          </div>
        )}

        {/* ═══════════════ SIMILAR PRODUCTS ═══════════════ */}
        {similarProducts.length > 0 && (
          <div className="bg-white rounded-lg mt-4 p-4 lg:p-6">
            <h2 className="text-lg font-bold text-gray-900 mb-4 uppercase">Sản Phẩm Tương Tự</h2>
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
              {similarProducts.slice(0, 12).map((p) => (
                <MiniProductCard key={p.id} product={p} />
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

/* ────────────────── Mini Product Card ────────────────── */

function getProductImage(product: ProductSummaryResponse | ProductResponse): string {
  if ("primaryImageUrl" in product && product.primaryImageUrl) return product.primaryImageUrl;
  if ("images" in product && Array.isArray(product.images) && product.images.length > 0) {
    const primary = product.images.find((i) => i.isPrimary);
    return primary ? primary.url : product.images[0].url;
  }
  return "https://placehold.co/300x300?text=No+Image";
}

function MiniProductCard({ product }: { product: ProductSummaryResponse | ProductResponse }) {
  const navigate = useNavigate();

  return (
    <div
      onClick={() => navigate(`/product/${product.id}`)}
      className="cursor-pointer group bg-white rounded-lg border border-gray-100 hover:border-red-200 hover:shadow-md overflow-hidden transition-all"
    >
      <div className="aspect-square bg-gray-50 overflow-hidden">
        <img
          src={getProductImage(product)}
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
