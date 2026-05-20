import { useState } from "react";
import { ShoppingCart, Star, Eye } from "lucide-react";

export interface Product {
  id: number;
  name: string;
  image: string;
  price: number;
  originalPrice?: number;
  rating: number;
  reviewCount: number;
  sold: number;
}

interface ProductCardProps {
  product: Product;
  onAddToCart?: (product: Product) => void;
  onViewDetail?: (product: Product) => void;
}

function formatPrice(price: number): string {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(price);
}

function formatSold(sold: number): string {
  if (sold >= 1000) return `${(sold / 1000).toFixed(1)}k`;
  return sold.toString();
}

function StarRating({ rating, count }: { rating: number; count: number }) {
  return (
    <div className="flex items-center gap-1">
      <div className="flex items-center gap-0.5">
        {[1, 2, 3, 4, 5].map((star) => {
          const filled = rating >= star;
          const partial = !filled && rating > star - 1;
          const fillPercent = partial ? Math.round((rating - (star - 1)) * 100) : 0;

          return (
            <span key={star} className="relative inline-block w-3 h-3">
              {/* Empty star */}
              <Star className="absolute inset-0 w-3 h-3 text-slate-200" fill="currentColor" />
              {/* Filled star (clipped) */}
              <span
                className="absolute inset-0 overflow-hidden"
                style={{ width: filled ? "100%" : `${fillPercent}%` }}
              >
                <Star className="w-3 h-3 text-amber-400" fill="currentColor" />
              </span>
            </span>
          );
        })}
      </div>
      <span className="text-[10px] text-slate-400 font-medium">({count})</span>
    </div>
  );
}

export default function ProductCard({ product, onAddToCart, onViewDetail }: ProductCardProps) {
  const [hovered, setHovered] = useState(false);
  const [addedFeedback, setAddedFeedback] = useState(false);

  const discountPercent =
    product.originalPrice && product.originalPrice > product.price
      ? Math.round(((product.originalPrice - product.price) / product.originalPrice) * 100)
      : null;

  const handleAddToCart = (e: React.MouseEvent) => {
    e.stopPropagation();
    onAddToCart?.(product);
    setAddedFeedback(true);
    setTimeout(() => setAddedFeedback(false), 1500);
  };

  return (
    <div
      className="group relative bg-white rounded-2xl overflow-hidden shadow-sm hover:shadow-xl transition-all duration-300 cursor-pointer border border-slate-100 hover:border-slate-200 hover:-translate-y-1"
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      onClick={() => onViewDetail?.(product)}
    >
      {/* Image Container */}
      <div className="relative w-full aspect-square overflow-hidden bg-slate-50">
        <img
          src={product.image}
          alt={product.name}
          className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
          loading="lazy"
        />

        {/* Discount Badge */}
        {discountPercent && (
          <div className="absolute top-2.5 left-2.5 z-10">
            <span className="bg-red-600 text-white text-[10px] font-black px-2 py-0.5 rounded-full shadow-md">
              -{discountPercent}%
            </span>
          </div>
        )}

        {/* Hover Action Overlay */}
        <div
          className={`absolute inset-x-0 bottom-0 flex items-center justify-center gap-2 p-3 transition-all duration-300 ${
            hovered ? "opacity-100 translate-y-0" : "opacity-0 translate-y-4"
          }`}
        >
          {/* Add to Cart Button */}
          <button
            onClick={handleAddToCart}
            className={`flex items-center gap-2 px-4 py-2.5 rounded-xl font-black text-xs uppercase tracking-wide shadow-lg transition-all duration-200 active:scale-95 ${
              addedFeedback
                ? "bg-green-500 text-white"
                : "bg-red-600 text-white hover:bg-red-700"
            }`}
          >
            <ShoppingCart className="w-3.5 h-3.5 shrink-0" />
            <span className="whitespace-nowrap">{addedFeedback ? "Đã thêm!" : "Thêm vào giỏ"}</span>
          </button>

          {/* Quick View Button */}
          <button
            onClick={(e) => { e.stopPropagation(); onViewDetail?.(product); }}
            className="p-2.5 rounded-xl bg-white/90 backdrop-blur-sm text-slate-700 hover:bg-white hover:text-red-600 shadow-lg transition-all duration-200 active:scale-95"
            title="Xem nhanh"
          >
            <Eye className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Info Section */}
      <div className="p-3 sm:p-4 space-y-2">
        {/* Product Name */}
        <h3 className="text-sm font-semibold text-slate-800 leading-snug line-clamp-2 group-hover:text-red-600 transition-colors duration-200">
          {product.name}
        </h3>

        {/* Rating + Sold */}
        <div className="flex items-center justify-between">
          <StarRating rating={product.rating} count={product.reviewCount} />
          <span className="text-[10px] text-slate-400 font-medium">
            Đã bán <span className="text-slate-600 font-bold">{formatSold(product.sold)}</span>
          </span>
        </div>

        {/* Price Row */}
        <div className="flex items-end gap-2 pt-0.5">
          <span className="text-base sm:text-lg font-black text-red-600 leading-none">
            {formatPrice(product.price)}
          </span>
          {product.originalPrice && product.originalPrice > product.price && (
            <span className="text-[11px] text-slate-400 font-medium line-through leading-none pb-0.5">
              {formatPrice(product.originalPrice)}
            </span>
          )}
        </div>
      </div>
    </div>
  );
}
