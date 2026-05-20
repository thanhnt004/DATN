import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Search, SlidersHorizontal, Star, X } from "lucide-react";
import { getRelatedCategories, searchProducts } from "../api/searchApi";
import { getCategoryTree } from "../api/categoryApi";
import { formatPrice } from "../utils/helpers";
import type { CategoryTreeResponse } from "../types/admin";
import type { RelatedCategoryResponse, SearchProductResponse } from "../types/search";

const SORT_TABS = [
  { value: "relevance", label: "Liên quan" },
  { value: "newest", label: "Mới nhất" },
  { value: "best_selling", label: "Bán chạy" },
] as const;

const RATING_OPTIONS = [
  { value: "", label: "Tất cả" },
  { value: "4", label: "Từ 4 sao" },
  { value: "3", label: "Từ 3 sao" },
  { value: "2", label: "Từ 2 sao" },
] as const;

type DraftFilters = {
  category: string;
  minPrice: string;
  maxPrice: string;
  rating: string;
};

function flattenCategories(nodes: CategoryTreeResponse[]): CategoryTreeResponse[] {
  const result: CategoryTreeResponse[] = [];
  const stack = [...nodes];
  while (stack.length > 0) {
    const node = stack.shift();
    if (!node) continue;
    result.push(node);
    const children = Array.isArray(node.children) ? node.children : [];
    stack.unshift(...children);
  }
  return result;
}

export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const keyword = searchParams.get("q") || "";
  const categoryId = searchParams.get("category") || "";
  const sortBy = searchParams.get("sort") || "relevance";
  const page = parseInt(searchParams.get("page") || "0", 10);
  const minPrice = searchParams.get("minPrice") || "";
  const maxPrice = searchParams.get("maxPrice") || "";
  const rating = searchParams.get("rating") || "";

  const [products, setProducts] = useState<SearchProductResponse[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const [loading, setLoading] = useState(true);

  const [categories, setCategories] = useState<CategoryTreeResponse[]>([]);
  const [relatedCategories, setRelatedCategories] = useState<RelatedCategoryResponse[]>([]);

  const [mobileFilterOpen, setMobileFilterOpen] = useState(false);
  const [draftFilters, setDraftFilters] = useState<DraftFilters>({
    category: categoryId,
    minPrice,
    maxPrice,
    rating,
  });

  const flatCategories = useMemo(() => flattenCategories(categories), [categories]);
  const categoryNameMap = useMemo(() => {
    const map = new Map<string, string>();
    flatCategories.forEach((c) => map.set(c.id, c.name));
    return map;
  }, [flatCategories]);

  const relatedCategoryItems = useMemo(
    () =>
      relatedCategories
        .map((item) => ({
          ...item,
          name: categoryNameMap.get(item.categoryId) || "",
        }))
        .filter((item) => !!item.name),
    [relatedCategories, categoryNameMap]
  );

  useEffect(() => {
    getCategoryTree()
      .then((res) => setCategories(res.data.result ?? []))
      .catch(() => setCategories([]));
  }, []);

  useEffect(() => {
    setDraftFilters({
      category: categoryId,
      minPrice,
      maxPrice,
      rating,
    });
  }, [categoryId, minPrice, maxPrice, rating]);

  const collectDescendantIds = useCallback((catId: string, cats: CategoryTreeResponse[]): string[] => {
    const ids: string[] = [];

    function gather(node: CategoryTreeResponse) {
      ids.push(node.id);
      const children = Array.isArray(node.children) ? node.children : [];
      children.forEach(gather);
    }

    function walk(nodes: CategoryTreeResponse[]): boolean {
      for (const node of nodes) {
        if (node.id === catId) {
          gather(node);
          return true;
        }
        const children = Array.isArray(node.children) ? node.children : [];
        if (children.length > 0 && walk(children)) {
          return true;
        }
      }
      return false;
    }

    walk(cats);
    return ids;
  }, []);

  const updateParams = useCallback(
    (updates: Record<string, string>) => {
      const newParams = new URLSearchParams(searchParams);
      Object.entries(updates).forEach(([k, v]) => {
        if (v) newParams.set(k, v);
        else newParams.delete(k);
      });
      setSearchParams(newParams);
    },
    [searchParams, setSearchParams]
  );

  const applyFilters = useCallback(() => {
    updateParams({
      category: draftFilters.category,
      minPrice: draftFilters.minPrice,
      maxPrice: draftFilters.maxPrice,
      rating: draftFilters.rating,
      page: "",
    });
    setMobileFilterOpen(false);
  }, [draftFilters, updateParams]);

  const resetFilters = useCallback(() => {
    const reset: DraftFilters = { category: "", minPrice: "", maxPrice: "", rating: "" };
    setDraftFilters(reset);
    updateParams({ category: "", minPrice: "", maxPrice: "", rating: "", page: "" });
    setMobileFilterOpen(false);
  }, [updateParams]);

  useEffect(() => {
    const handler = () => setMobileFilterOpen(true);
    window.addEventListener("open-search-filters", handler);
    return () => window.removeEventListener("open-search-filters", handler);
  }, []);

  useEffect(() => {
    let active = true;
    const fetchRelatedCategories = async () => {
      try {
        const response = await getRelatedCategories({
          keyword: keyword || undefined,
          minPrice: minPrice ? Number(minPrice) : undefined,
          maxPrice: maxPrice ? Number(maxPrice) : undefined,
          minRating: rating ? Number(rating) : undefined,
          size: 12,
        });
        if (!active) return;
        setRelatedCategories(response.data.result ?? []);
      } catch {
        if (!active) return;
        setRelatedCategories([]);
      }
    };

    fetchRelatedCategories();
    return () => {
      active = false;
    };
  }, [keyword, minPrice, maxPrice, rating]);

  useEffect(() => {
    let active = true;

    const fetchProducts = async () => {
      setLoading(true);
      try {
        let categoryIds: string | undefined;
        if (categoryId && categories.length > 0) {
          const allIds = collectDescendantIds(categoryId, categories);
          if (allIds.length > 1) {
            categoryIds = allIds.join(",");
          }
        }

        const res = await searchProducts({
          keyword: keyword || undefined,
          categoryId: categoryIds ? undefined : categoryId || undefined,
          categoryIds,
          minPrice: minPrice ? Number(minPrice) : undefined,
          maxPrice: maxPrice ? Number(maxPrice) : undefined,
          minRating: rating ? Number(rating) : undefined,
          sortBy,
          page,
          size: 24,
        });

        if (!active) return;
        const data = res.data.result;
        setProducts(data.content ?? []);
        setTotalPages(data.totalPages ?? 0);
        setTotalItems(data.totalElements ?? 0);
      } catch {
        if (!active) return;
        setProducts([]);
        setTotalPages(0);
        setTotalItems(0);
      } finally {
        if (active) setLoading(false);
      }
    };

    fetchProducts();
    return () => {
      active = false;
    };
  }, [keyword, categoryId, sortBy, page, minPrice, maxPrice, rating, categories, collectDescendantIds]);

  const renderFilterContent = () => (
    <div className="space-y-6">
      <section>
        <h3 className="text-sm font-bold text-gray-900 mb-3">Danh mục liên quan</h3>
        <div className="space-y-2 max-h-56 overflow-auto pr-1">
          <label className="flex items-center gap-2 text-sm">
            <input
              type="radio"
              name="category"
              checked={draftFilters.category === ""}
              onChange={() => setDraftFilters((prev) => ({ ...prev, category: "" }))}
            />
            <span>Tất cả danh mục</span>
          </label>
          {relatedCategoryItems.map((item) => (
            <label key={item.categoryId} className="flex items-center justify-between gap-2 text-sm">
              <span className="flex items-center gap-2">
                <input
                  type="radio"
                  name="category"
                  checked={draftFilters.category === item.categoryId}
                  onChange={() => setDraftFilters((prev) => ({ ...prev, category: item.categoryId }))}
                />
                <span>{item.name}</span>
              </span>
              <span className="text-xs text-gray-400">({item.count})</span>
            </label>
          ))}
          {relatedCategoryItems.length === 0 && (
            <p className="text-xs text-gray-500">Chưa có danh mục liên quan cho bộ lọc hiện tại.</p>
          )}
        </div>
      </section>

      <section>
        <h3 className="text-sm font-bold text-gray-900 mb-3">Khoảng giá</h3>
        <div className="grid grid-cols-2 gap-2">
          <input
            type="number"
            placeholder="Từ"
            value={draftFilters.minPrice}
            onChange={(e) => setDraftFilters((prev) => ({ ...prev, minPrice: e.target.value.replace(/^0+(?=\d)/, "") }))}
            className="border rounded-lg px-3 py-2 text-sm"
          />
          <input
            type="number"
            placeholder="Đến"
            value={draftFilters.maxPrice}
            onChange={(e) => setDraftFilters((prev) => ({ ...prev, maxPrice: e.target.value.replace(/^0+(?=\d)/, "") }))}
            className="border rounded-lg px-3 py-2 text-sm"
          />
        </div>
      </section>

      <section>
        <h3 className="text-sm font-bold text-gray-900 mb-3">Đánh giá</h3>
        <div className="space-y-2">
          {RATING_OPTIONS.map((opt) => (
            <label key={opt.value} className="flex items-center gap-2 text-sm">
              <input
                type="radio"
                name="rating"
                checked={draftFilters.rating === opt.value}
                onChange={() => setDraftFilters((prev) => ({ ...prev, rating: opt.value }))}
              />
              <span>{opt.label}</span>
            </label>
          ))}
        </div>
      </section>

      <div className="flex items-center gap-2">
        <button
          onClick={applyFilters}
          className="flex-1 bg-red-600 text-white text-sm font-semibold py-2.5 rounded-lg hover:bg-red-700"
        >
          Áp dụng
        </button>
        <button
          onClick={resetFilters}
          className="flex-1 border border-gray-300 text-gray-700 text-sm font-semibold py-2.5 rounded-lg hover:bg-gray-50"
        >
          Thiết lập lại
        </button>
      </div>
    </div>
  );

  return (
    <div className="max-w-7xl mx-auto px-3 sm:px-4 py-4 sm:py-6">
      <div className="mb-4 sm:mb-5">
        {keyword ? (
          <h1 className="text-base sm:text-lg font-bold text-gray-900">
            Kết quả cho “{keyword}”
            <span className="ml-2 text-sm text-gray-500 font-medium">({totalItems} sản phẩm)</span>
          </h1>
        ) : (
          <h1 className="text-base sm:text-lg font-bold text-gray-900">
            Tất cả sản phẩm
            <span className="ml-2 text-sm text-gray-500 font-medium">({totalItems} sản phẩm)</span>
          </h1>
        )}
      </div>

      <div className="lg:grid lg:grid-cols-[280px_1fr] gap-6">
        <aside className="hidden lg:block bg-white rounded-xl border p-4 h-fit sticky top-24">
          {renderFilterContent()}
        </aside>

        <section>
          <div className="bg-white border rounded-xl px-3 py-2 mb-4 overflow-x-auto">
            <div className="flex items-center gap-2 min-w-max">
              {SORT_TABS.map((tab) => (
                <button
                  key={tab.value}
                  onClick={() => updateParams({ sort: tab.value, page: "" })}
                  className={`px-3 py-1.5 rounded-lg text-sm font-medium ${
                    sortBy === tab.value ? "bg-red-600 text-white" : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                  }`}
                >
                  {tab.label}
                </button>
              ))}

              <span className="text-sm font-medium text-gray-500 pl-2">Giá:</span>
              <button
                onClick={() => updateParams({ sort: "price_desc", page: "" })}
                className={`px-3 py-1.5 rounded-lg text-sm font-medium ${
                  sortBy === "price_desc" ? "bg-red-600 text-white" : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                }`}
              >
                Cao
              </button>
              <button
                onClick={() => updateParams({ sort: "price_asc", page: "" })}
                className={`px-3 py-1.5 rounded-lg text-sm font-medium ${
                  sortBy === "price_asc" ? "bg-red-600 text-white" : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                }`}
              >
                Thấp
              </button>
            </div>
          </div>

          {loading ? (
            <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-3 sm:gap-4">
              {Array.from({ length: 12 }).map((_, i) => (
                <div key={i} className="animate-pulse bg-white rounded-xl border p-2.5">
                  <div className="bg-gray-200 aspect-square rounded-lg mb-2" />
                  <div className="h-3.5 bg-gray-200 rounded mb-2" />
                  <div className="h-3.5 bg-gray-200 rounded w-2/3" />
                </div>
              ))}
            </div>
          ) : products.length === 0 ? (
            <div className="text-center py-16 bg-white border rounded-xl">
              <Search className="w-14 h-14 text-gray-300 mx-auto mb-3" />
              <p className="text-gray-600 font-medium">Không tìm thấy sản phẩm phù hợp</p>
              <p className="text-sm text-gray-400 mt-1">Thử từ khóa hoặc bộ lọc khác.</p>
            </div>
          ) : (
            <>
              <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-3 sm:gap-4">
                {products.map((product) => (
                  <Link
                    key={product.id}
                    to={`/product/${product.id}`}
                    className="group bg-white rounded-xl border overflow-hidden hover:shadow-md transition"
                  >
                    <div className="aspect-square bg-gray-100 overflow-hidden">
                      <img
                        src={product.thumbnailUrl || "https://placehold.co/280x280?text=No+Image"}
                        alt={product.name}
                        className="w-full h-full object-cover group-hover:scale-105 transition duration-300"
                      />
                    </div>
                    <div className="p-3">
                      <h3 className="text-sm text-gray-800 line-clamp-2 min-h-[2.5rem] mb-2">{product.name}</h3>
                      <div className="text-red-600 font-bold text-sm mb-1">{formatPrice(product.minPrice)}</div>
                      {product.minPrice !== product.maxPrice && (
                        <div className="text-xs text-gray-500 mb-1">Đến {formatPrice(product.maxPrice)}</div>
                      )}
                      <div className="flex items-center justify-between text-xs text-gray-500">
                        {product.ratingAvg != null && (
                          <span className="flex items-center gap-0.5">
                            <Star className="w-3 h-3 text-yellow-400 fill-yellow-400" />
                            {product.ratingAvg.toFixed(1)}
                          </span>
                        )}
                        {product.soldCount != null && product.soldCount > 0 && (
                          <span>
                            Đã bán {product.soldCount >= 1000 ? `${(product.soldCount / 1000).toFixed(1)}k` : product.soldCount}
                          </span>
                        )}
                      </div>
                    </div>
                  </Link>
                ))}
              </div>

              {totalPages > 1 && (
                <div className="flex items-center justify-center gap-2 mt-6 sm:mt-8 flex-wrap">
                  <button
                    onClick={() => updateParams({ page: String(Math.max(0, page - 1)) })}
                    disabled={page === 0}
                    className="px-3 sm:px-4 py-2 border rounded-lg text-sm disabled:opacity-50 hover:bg-gray-50"
                  >
                    Trước
                  </button>
                  {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                    const start = Math.max(0, Math.min(page - 2, totalPages - 5));
                    const pageNum = start + i;
                    return (
                      <button
                        key={pageNum}
                        onClick={() => updateParams({ page: String(pageNum) })}
                        className={`px-3 py-2 rounded-lg text-sm ${
                          pageNum === page ? "bg-red-600 text-white" : "border hover:bg-gray-50"
                        }`}
                      >
                        {pageNum + 1}
                      </button>
                    );
                  })}
                  <button
                    onClick={() => updateParams({ page: String(Math.min(totalPages - 1, page + 1)) })}
                    disabled={page >= totalPages - 1}
                    className="px-3 sm:px-4 py-2 border rounded-lg text-sm disabled:opacity-50 hover:bg-gray-50"
                  >
                    Sau
                  </button>
                </div>
              )}
            </>
          )}
        </section>
      </div>

      {mobileFilterOpen && (
        <div className="fixed inset-0 z-[70] lg:hidden">
          <div className="absolute inset-0 bg-black/40" onClick={() => setMobileFilterOpen(false)} />
          <div className="absolute left-0 right-0 top-0 h-[66vh] bg-white rounded-b-2xl shadow-2xl border-b flex flex-col">
            <div className="flex items-center justify-between px-4 py-3 border-b">
              <div className="flex items-center gap-2">
                <SlidersHorizontal className="w-4 h-4 text-red-600" />
                <h2 className="text-sm font-bold">Bộ lọc tìm kiếm</h2>
              </div>
              <button onClick={() => setMobileFilterOpen(false)} className="p-1.5 rounded-lg hover:bg-gray-100">
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-4">{renderFilterContent()}</div>
          </div>
        </div>
      )}
    </div>
  );
}
