import { useState, useEffect } from "react";
import { Star, ChevronDown, ChevronUp, Reply, Trash2, Edit3, Image as ImageIcon, Store, ArrowUp, ArrowDown, List, Grid3x3 } from "lucide-react";
import toast from "react-hot-toast";
import axiosInstance from "../../api/axiosInstance";
import type { ApiResponse } from "../../types/auth";
import type { ReviewResponse, ReviewPageResponse } from "../../types/review";
import type { ProductResponse } from "../../types/product";
import type { PageResponse } from "../../types/user";

type ViewMode = "product" | "review";

interface ReviewWithProduct extends ReviewResponse {
  productInfo?: ProductResponse;
}

export default function SellerReviewsPage() {
  const [viewMode, setViewMode] = useState<ViewMode>("product");
  const [products, setProducts] = useState<ProductResponse[]>([]);
  const [reviews, setReviews] = useState<ReviewWithProduct[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedProductId, setExpandedProductId] = useState<string | null>(null);
  const [reviewPage, setReviewPage] = useState(0);
  const [reviewTotalPages, setReviewTotalPages] = useState(1);
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("desc");

  useEffect(() => {
    if (viewMode === "product") {
      fetchMyProducts();
    } else {
      fetchAllReviews(0);
    }
  }, [viewMode]);

  const fetchMyProducts = async () => {
    try {
      setLoading(true);
      const productsRes = await axiosInstance.get<ApiResponse<PageResponse<ProductResponse>>>("/api/v1/products/seller/me", {
        params: { size: 100, sortBy: "createdAt", sortDirection: "desc" }
      });

      if (!productsRes.data.success) {
        throw new Error("Failed to fetch products");
      }

      const productsData = productsRes.data.result.content;

      // Fetch reviews for all products in parallel
      const productsWithLatestReview = await Promise.all(
        productsData.map(async (product) => {
          try {
            const reviewRes = await axiosInstance.get<ApiResponse<ReviewPageResponse>>(
              `/api/v1/reviews/product/${product.id}`,
              { params: { page: 0, size: 1, sortBy: "createdAt", sortDirection: "desc" } }
            );
            const latestReview = reviewRes.data.success && reviewRes.data.result.content.length > 0
              ? reviewRes.data.result.content[0]
              : null;
            return { product, latestReviewDate: latestReview?.createdAt };
          } catch {
            return { product, latestReviewDate: null };
          }
        })
      );

      // Sort products by latest review date
      const sorted = productsWithLatestReview.sort((a, b) => {
        if (a.latestReviewDate && b.latestReviewDate) {
          return b.latestReviewDate.localeCompare(a.latestReviewDate);
        }
        if (a.latestReviewDate) return -1;
        if (b.latestReviewDate) return 1;
        return 0;
      });

      setProducts(sorted.map(item => item.product));
    } catch (error) {
      toast.error("Không thể tải danh sách sản phẩm.");
    } finally {
      setLoading(false);
    }
  };

  const fetchAllReviews = async (page = 0, direction: "asc" | "desc" = sortDirection) => {
    try {
      setLoading(true);
      
      // Get all products first
      const productsRes = await axiosInstance.get<ApiResponse<PageResponse<ProductResponse>>>("/api/v1/products/seller/me", {
        params: { size: 100 }
      });

      if (!productsRes.data.success) {
        throw new Error("Failed to fetch products");
      }

      const productsMap: Record<string, ProductResponse> = {};
      productsRes.data.result.content.forEach(p => {
        productsMap[p.id] = p;
      });

      // Fetch all reviews for all products
      const allReviews: ReviewWithProduct[] = [];
      await Promise.all(
        productsRes.data.result.content.map(async (product) => {
          try {
            const reviewRes = await axiosInstance.get<ApiResponse<ReviewPageResponse>>(
              `/api/v1/reviews/product/${product.id}`,
              { params: { page: 0, size: 100, sortBy: "createdAt", sortDirection: direction } }
            );
            if (reviewRes.data.success) {
              const reviewsWithProduct = reviewRes.data.result.content.map(r => ({
                ...r,
                productInfo: productsMap[r.productId]
              }));
              allReviews.push(...reviewsWithProduct);
            }
          } catch {
            // Skip product if review fetch fails
          }
        })
      );

      // Sort all reviews by date
      allReviews.sort((a, b) => b.createdAt.localeCompare(a.createdAt));

      // Paginate
      const pageSize = 10;
      const start = page * pageSize;
      const paginatedReviews = allReviews.slice(start, start + pageSize);
      
      setReviews(paginatedReviews);
      setReviewTotalPages(Math.ceil(allReviews.length / pageSize));
      setReviewPage(page);
    } catch (error) {
      toast.error("Không thể tải danh sách đánh giá.");
    } finally {
      setLoading(false);
    }
  };

  const handleSortChange = (direction: "asc" | "desc") => {
    setSortDirection(direction);
    if (viewMode === "review") {
      fetchAllReviews(0, direction);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Quản lý đánh giá</h1>
          <p className="text-slate-500 mt-1">Xem và phản hồi đánh giá của khách hàng</p>
        </div>
        
        {/* View Mode Toggle */}
        <div className="flex gap-2">
          <button
            onClick={() => setViewMode("product")}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-colors ${
              viewMode === "product"
                ? "bg-red-600 text-white"
                : "bg-slate-200 text-slate-700 hover:bg-slate-300"
            }`}
          >
            <Grid3x3 className="w-4 h-4" /> Theo Sản phẩm
          </button>
          <button
            onClick={() => setViewMode("review")}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-colors ${
              viewMode === "review"
                ? "bg-red-600 text-white"
                : "bg-slate-200 text-slate-700 hover:bg-slate-300"
            }`}
          >
            <List className="w-4 h-4" /> Danh sách Đánh giá
          </button>
        </div>
      </div>

      {/* Product View */}
      {viewMode === "product" && (
        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
          {loading ? (
            <div className="p-8 text-center text-slate-500">Đang tải...</div>
          ) : products.length === 0 ? (
            <div className="p-8 text-center text-slate-500">Chưa có sản phẩm nào.</div>
          ) : (
            <div className="divide-y divide-slate-100">
              {products.map((product) => (
                <ProductReviewItem
                  key={product.id}
                  product={product}
                  isExpanded={expandedProductId === product.id}
                  onToggle={() => setExpandedProductId(expandedProductId === product.id ? null : product.id)}
                />
              ))}
            </div>
          )}
        </div>
      )}

      {/* Review List View */}
      {viewMode === "review" && (
        <div className="space-y-4">
          {/* Sort Controls */}
          <div className="flex items-center justify-between bg-white p-4 rounded-xl border border-slate-200">
            <div className="flex items-center gap-2">
              <span className="text-sm font-medium text-slate-700">Sắp xếp:</span>
              <div className="flex gap-2">
                <button
                  onClick={() => handleSortChange("desc")}
                  className={`flex items-center gap-1 px-3 py-1.5 rounded text-sm transition-colors ${
                    sortDirection === "desc"
                      ? "bg-red-100 text-red-700 border border-red-300"
                      : "bg-white text-slate-600 border border-slate-300 hover:bg-slate-100"
                  }`}
                >
                  <ArrowDown className="w-4 h-4" /> Mới nhất
                </button>
                <button
                  onClick={() => handleSortChange("asc")}
                  className={`flex items-center gap-1 px-3 py-1.5 rounded text-sm transition-colors ${
                    sortDirection === "asc"
                      ? "bg-red-100 text-red-700 border border-red-300"
                      : "bg-white text-slate-600 border border-slate-300 hover:bg-slate-100"
                  }`}
                >
                  <ArrowUp className="w-4 h-4" /> Cũ nhất
                </button>
              </div>
            </div>
            <span className="text-xs text-slate-500">Trang {reviewPage + 1} / {reviewTotalPages}</span>
          </div>

          {/* Reviews List */}
          {loading ? (
            <div className="text-center py-8 text-slate-500">Đang tải...</div>
          ) : reviews.length === 0 ? (
            <div className="text-center py-8 text-slate-500">Chưa có đánh giá nào.</div>
          ) : (
            <>
              <div className="space-y-4">
                {reviews.map((review) => (
                  <ReviewListItem key={review.id} review={review} onReplySaved={() => fetchAllReviews(reviewPage, sortDirection)} />
                ))}
              </div>

              {/* Pagination */}
              {reviewTotalPages > 1 && (
                <div className="flex items-center justify-center gap-2 pt-4">
                  <button
                    disabled={reviewPage === 0}
                    onClick={() => fetchAllReviews(reviewPage - 1, sortDirection)}
                    className="px-3 py-2 rounded border border-slate-300 bg-white text-slate-600 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-slate-100 transition-colors text-sm font-medium"
                  >
                    ← Trước
                  </button>

                  <div className="flex gap-1">
                    {Array.from({ length: reviewTotalPages }).map((_, i) => (
                      <button
                        key={i}
                        onClick={() => fetchAllReviews(i, sortDirection)}
                        className={`w-8 h-8 rounded text-xs font-medium transition-colors ${
                          reviewPage === i
                            ? "bg-red-600 text-white border border-red-600"
                            : "border border-slate-300 bg-white text-slate-600 hover:bg-slate-100"
                        }`}
                      >
                        {i + 1}
                      </button>
                    ))}
                  </div>

                  <button
                    disabled={reviewPage >= reviewTotalPages - 1}
                    onClick={() => fetchAllReviews(reviewPage + 1, sortDirection)}
                    className="px-3 py-2 rounded border border-slate-300 bg-white text-slate-600 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-slate-100 transition-colors text-sm font-medium"
                  >
                    Sau →
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
}

function ReviewListItem({ review, onReplySaved }: { review: ReviewWithProduct; onReplySaved: () => void }) {
  const [replyText, setReplyText] = useState("");
  const [isReplying, setIsReplying] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleReplySubmit = async () => {
    if (!replyText.trim()) return;
    try {
      setLoading(true);
      if (isEditing && review.reply) {
        await axiosInstance.put(`/api/v1/seller/reviews/replies/${review.reply.id}`, { comment: replyText });
        toast.success("Cập nhật phản hồi thành công!");
      } else {
        await axiosInstance.post(`/api/v1/seller/reviews/${review.id}/reply`, { comment: replyText });
        toast.success("Phản hồi đánh giá thành công!");
      }
      setIsReplying(false);
      setIsEditing(false);
      setReplyText("");
      onReplySaved();
    } catch (error) {
      toast.error("Có lỗi xảy ra khi gửi phản hồi.");
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteReply = async () => {
    if (!review.reply) return;
    if (!window.confirm("Bạn có chắc chắn muốn xóa phản hồi này?")) return;
    try {
      setLoading(true);
      await axiosInstance.delete(`/api/v1/seller/reviews/replies/${review.reply.id}`);
      toast.success("Xóa phản hồi thành công!");
      onReplySaved();
    } catch (error) {
      toast.error("Không thể xóa phản hồi.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-white border border-slate-200 rounded-lg overflow-hidden">
      {/* Product Info Header */}
      <div className="bg-slate-50 border-b border-slate-200 p-3 flex items-center gap-3">
        {review.productInfo?.images && review.productInfo.images.length > 0 ? (
          <img
            src={review.productInfo.images.find(img => img.isPrimary)?.url || review.productInfo.images[0].url}
            alt={review.productInfo.name}
            className="w-10 h-10 rounded object-cover"
          />
        ) : (
          <div className="w-10 h-10 rounded bg-slate-200 flex items-center justify-center">
            <ImageIcon className="w-5 h-5 text-slate-400" />
          </div>
        )}
        <div className="flex-1 min-w-0">
          <h4 className="font-semibold text-sm text-slate-800 line-clamp-1">{review.productInfo?.name || "Sản phẩm"}</h4>
          <p className="text-xs text-slate-500">{review.productInfo?.id || "N/A"}</p>
        </div>
      </div>

      {/* Review Content */}
      <div className="p-4 space-y-3">
        {/* Reviewer Info */}
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-slate-200 flex items-center justify-center text-slate-600 font-bold text-xs uppercase">
              {review.userName?.charAt(0) || "U"}
            </div>
            <div>
              <div className="font-medium text-sm text-slate-800">
                {review.anonymous ? "Người dùng ẩn danh" : review.userName || "Người dùng"}
              </div>
              <div className="text-xs text-slate-400">{new Date(review.createdAt).toLocaleDateString("vi-VN")}</div>
            </div>
          </div>
          <div className="flex items-center text-amber-500">
            {[...Array(5)].map((_, i) => (
              <Star key={i} className={`w-4 h-4 ${i < review.rating ? "fill-current" : "text-slate-300"}`} />
            ))}
          </div>
        </div>

        {/* Review Text */}
        <div>
          <p className="text-sm text-slate-700">{review.comment}</p>
        </div>

        {/* Review Images */}
        {review.images && review.images.length > 0 && (
          <div className="flex gap-2 flex-wrap">
            {review.images.map((img, i) => (
              <img key={i} src={img} alt="Review" className="w-20 h-20 rounded object-cover border border-slate-200" />
            ))}
          </div>
        )}

        {/* Reply Section */}
        <div className="border-t border-slate-200 pt-3">
          {review.reply ? (
            <div className="bg-slate-50 border-l-2 border-red-500 pl-3 py-2 rounded-r relative group">
              <div className="flex justify-between items-center mb-1">
                <span className="font-semibold text-xs text-slate-800 flex items-center gap-1">
                  <Store className="w-3 h-3 text-red-600" /> Phản hồi của người bán
                </span>
                <span className="text-xs text-slate-400">{new Date(review.reply.createdAt).toLocaleDateString("vi-VN")}</span>
              </div>
              <p className="text-sm text-slate-600 whitespace-pre-wrap">{review.reply.comment}</p>

              <div className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity flex gap-1">
                <button
                  onClick={() => {
                    setReplyText(review.reply!.comment);
                    setIsReplying(true);
                    setIsEditing(true);
                  }}
                  className="p-1 text-slate-400 hover:text-blue-600 bg-white rounded shadow-sm border border-slate-200"
                  title="Sửa phản hồi"
                >
                  <Edit3 className="w-3.5 h-3.5" />
                </button>
                <button
                  onClick={handleDeleteReply}
                  className="p-1 text-slate-400 hover:text-red-600 bg-white rounded shadow-sm border border-slate-200"
                  title="Xóa phản hồi"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          ) : (
            !isReplying && (
              <button
                onClick={() => setIsReplying(true)}
                className="text-sm text-red-600 font-medium flex items-center gap-1 hover:text-red-700"
              >
                <Reply className="w-4 h-4" /> Trả lời
              </button>
            )
          )}

          {isReplying && (
            <div className="mt-3 bg-slate-50 p-3 rounded border border-slate-200">
              <textarea
                className="w-full border border-slate-300 rounded p-2 text-sm focus:ring-2 focus:ring-red-100 outline-none"
                rows={3}
                placeholder="Nhập nội dung phản hồi của bạn..."
                value={replyText}
                onChange={(e) => setReplyText(e.target.value)}
              />
              <div className="flex justify-end gap-2 mt-2">
                <button
                  onClick={() => {
                    setIsReplying(false);
                    setReplyText("");
                    setIsEditing(false);
                  }}
                  className="px-3 py-1.5 text-sm text-slate-600 hover:bg-slate-200 rounded transition-colors"
                  disabled={loading}
                >
                  Hủy
                </button>
                <button
                  onClick={handleReplySubmit}
                  className="px-3 py-1.5 text-sm bg-red-600 text-white hover:bg-red-700 rounded transition-colors"
                  disabled={loading || !replyText.trim()}
                >
                  {loading ? "Đang lưu..." : isEditing ? "Cập nhật" : "Gửi phản hồi"}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function ProductReviewItem({ product, isExpanded, onToggle }: { product: ProductResponse; isExpanded: boolean; onToggle: () => void }) {
  const [reviews, setReviews] = useState<ReviewResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("desc");

  const fetchReviews = async (p = 0, direction: "asc" | "desc" = sortDirection) => {
    try {
      setLoading(true);
      const res = await axiosInstance.get<ApiResponse<ReviewPageResponse>>(`/api/v1/reviews/product/${product.id}`, {
        params: { page: p, size: 10, sortBy: "createdAt", sortDirection: direction }
      });
      if (res.data.success) {
        setReviews(res.data.result.content);
        setTotalPages(res.data.result.totalPages);
      }
    } catch (error) {
      toast.error("Không thể tải đánh giá.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isExpanded) {
      fetchReviews(0, sortDirection);
    }
  }, [isExpanded]);

  const handleSortChange = (direction: "asc" | "desc") => {
    setSortDirection(direction);
    setPage(0);
    fetchReviews(0, direction);
  };

  const handleReplySaved = () => {
    fetchReviews(page, sortDirection);
  };

  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < totalPages) {
      setPage(newPage);
      fetchReviews(newPage, sortDirection);
    }
  };

  return (
    <div className="flex flex-col">
      <div
        className="flex items-center justify-between p-4 cursor-pointer hover:bg-slate-50 transition-colors"
        onClick={onToggle}
      >
        <div className="flex items-center gap-4">
          {product.images && product.images.length > 0 ? (
            <img src={product.images.find(img => img.isPrimary)?.url || product.images[0].url} alt={product.name} className="w-12 h-12 rounded object-cover border border-slate-200" />
          ) : (
            <div className="w-12 h-12 rounded bg-slate-100 border border-slate-200 flex items-center justify-center">
              <ImageIcon className="w-5 h-5 text-slate-400" />
            </div>
          )}
          <div>
            <h3 className="font-semibold text-slate-800 line-clamp-1">{product.name}</h3>
            <div className="flex items-center gap-2 mt-1 text-sm text-slate-500">
              <span className="flex items-center text-amber-500"><Star className="w-3.5 h-3.5 fill-current mr-1" /> {product.ratingAvg?.toFixed(1) || "0.0"}</span>
              <span>•</span>
              <span>{product.ratingCount || 0} đánh giá</span>
              <span>•</span>
              <span>{product.soldCount || 0} đã bán</span>
            </div>
          </div>
        </div>
        <div>
          {isExpanded ? <ChevronUp className="w-5 h-5 text-slate-400" /> : <ChevronDown className="w-5 h-5 text-slate-400" />}
        </div>
      </div>

      {isExpanded && (
        <div className="border-t border-slate-100 bg-slate-50 p-4">
          {/* Sort Controls */}
          <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-200">
            <div className="flex items-center gap-2">
              <span className="text-sm font-medium text-slate-700">Sắp xếp:</span>
              <div className="flex gap-2">
                <button
                  onClick={() => handleSortChange("desc")}
                  className={`flex items-center gap-1 px-3 py-1.5 rounded text-sm transition-colors ${
                    sortDirection === "desc"
                      ? "bg-red-100 text-red-700 border border-red-300"
                      : "bg-white text-slate-600 border border-slate-300 hover:bg-slate-100"
                  }`}
                >
                  <ArrowDown className="w-4 h-4" /> Mới nhất
                </button>
                <button
                  onClick={() => handleSortChange("asc")}
                  className={`flex items-center gap-1 px-3 py-1.5 rounded text-sm transition-colors ${
                    sortDirection === "asc"
                      ? "bg-red-100 text-red-700 border border-red-300"
                      : "bg-white text-slate-600 border border-slate-300 hover:bg-slate-100"
                  }`}
                >
                  <ArrowUp className="w-4 h-4" /> Cũ nhất
                </button>
              </div>
            </div>
            <span className="text-xs text-slate-500">Trang {page + 1} / {totalPages}</span>
          </div>

          {loading ? (
            <div className="text-center py-8 text-slate-500 text-sm">Đang tải đánh giá...</div>
          ) : reviews.length === 0 ? (
            <div className="text-center py-8 text-slate-500 text-sm">Chưa có đánh giá nào cho sản phẩm này.</div>
          ) : (
            <>
              <div className="space-y-4 mb-4">
                {reviews.map(review => (
                  <ReviewItem key={review.id} review={review} onReplySaved={handleReplySaved} />
                ))}
              </div>

              {/* Pagination */}
              {totalPages > 1 && (
                <div className="flex items-center justify-center gap-2 pt-4 border-t border-slate-200">
                  <button
                    disabled={page === 0}
                    onClick={() => handlePageChange(page - 1)}
                    className="px-3 py-2 rounded border border-slate-300 bg-white text-slate-600 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-slate-100 transition-colors text-sm font-medium"
                  >
                    ← Trước
                  </button>

                  <div className="flex gap-1">
                    {Array.from({ length: totalPages }).map((_, i) => (
                      <button
                        key={i}
                        onClick={() => handlePageChange(i)}
                        className={`w-8 h-8 rounded text-xs font-medium transition-colors ${
                          page === i
                            ? "bg-red-600 text-white border border-red-600"
                            : "border border-slate-300 bg-white text-slate-600 hover:bg-slate-100"
                        }`}
                      >
                        {i + 1}
                      </button>
                    ))}
                  </div>

                  <button
                    disabled={page >= totalPages - 1}
                    onClick={() => handlePageChange(page + 1)}
                    className="px-3 py-2 rounded border border-slate-300 bg-white text-slate-600 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-slate-100 transition-colors text-sm font-medium"
                  >
                    Sau →
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
}

function ReviewItem({ review, onReplySaved }: { review: ReviewResponse; onReplySaved: () => void }) {
  const [replyText, setReplyText] = useState("");
  const [isReplying, setIsReplying] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleReplySubmit = async () => {
    if (!replyText.trim()) return;
    try {
      setLoading(true);
      if (isEditing && review.reply) {
        await axiosInstance.put(`/api/v1/seller/reviews/replies/${review.reply.id}`, { comment: replyText });
        toast.success("Cập nhật phản hồi thành công!");
      } else {
        await axiosInstance.post(`/api/v1/seller/reviews/${review.id}/reply`, { comment: replyText });
        toast.success("Phản hồi đánh giá thành công!");
      }
      setIsReplying(false);
      setIsEditing(false);
      setReplyText("");
      onReplySaved();
    } catch (error) {
      toast.error("Có lỗi xảy ra khi gửi phản hồi.");
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteReply = async () => {
    if (!review.reply) return;
    if (!window.confirm("Bạn có chắc chắn muốn xóa phản hồi này?")) return;
    try {
      setLoading(true);
      await axiosInstance.delete(`/api/v1/seller/reviews/replies/${review.reply.id}`);
      toast.success("Xóa phản hồi thành công!");
      onReplySaved();
    } catch (error) {
      toast.error("Không thể xóa phản hồi.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-white p-4 rounded-lg border border-slate-200 shadow-sm">
      <div className="flex justify-between items-start mb-2">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-full bg-slate-200 flex items-center justify-center text-slate-600 font-bold text-xs uppercase">
            {review.userName?.charAt(0) || "U"}
          </div>
          <div>
            <div className="font-medium text-sm text-slate-800">{review.anonymous ? "Người dùng ẩn danh" : review.userName || "Người dùng"}</div>
            <div className="flex items-center text-amber-500">
              {[...Array(5)].map((_, i) => (
                <Star key={i} className={`w-3 h-3 ${i < review.rating ? "fill-current" : "text-slate-300"}`} />
              ))}
              <span className="text-xs text-slate-400 ml-2">{new Date(review.createdAt).toLocaleDateString("vi-VN")}</span>
            </div>
          </div>
        </div>
      </div>

      <p className="text-slate-700 text-sm mt-2">{review.comment}</p>

      {review.images && review.images.length > 0 && (
        <div className="flex gap-2 mt-3">
          {review.images.map((img, i) => (
            <img key={i} src={img} alt="Review" className="w-16 h-16 rounded object-cover border border-slate-200" />
          ))}
        </div>
      )}

      <div className="mt-4">
        {review.reply ? (
          <div className="bg-slate-50 border-l-2 border-red-500 pl-3 py-2 mt-3 rounded-r relative group">
            <div className="flex justify-between items-center mb-1">
              <span className="font-semibold text-xs text-slate-800 flex items-center gap-1">
                <Store className="w-3 h-3 text-red-600" /> Phản hồi của người bán
              </span>
              <span className="text-xs text-slate-400">{new Date(review.reply.createdAt).toLocaleDateString("vi-VN")}</span>
            </div>
            <p className="text-sm text-slate-600 whitespace-pre-wrap">{review.reply.comment}</p>

            <div className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity flex gap-1">
              <button
                onClick={() => {
                  setReplyText(review.reply!.comment);
                  setIsReplying(true);
                  setIsEditing(true);
                }}
                className="p-1 text-slate-400 hover:text-blue-600 bg-white rounded shadow-sm border border-slate-200"
                title="Sửa phản hồi"
              >
                <Edit3 className="w-3.5 h-3.5" />
              </button>
              <button
                onClick={handleDeleteReply}
                className="p-1 text-slate-400 hover:text-red-600 bg-white rounded shadow-sm border border-slate-200"
                title="Xóa phản hồi"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        ) : (
          !isReplying && (
            <button
              onClick={() => setIsReplying(true)}
              className="text-sm text-red-600 font-medium flex items-center gap-1 hover:text-red-700"
            >
              <Reply className="w-4 h-4" /> Trả lời
            </button>
          )
        )}

        {isReplying && (
          <div className="mt-3 bg-slate-50 p-3 rounded border border-slate-200">
            <textarea
              className="w-full border border-slate-300 rounded p-2 text-sm focus:ring-2 focus:ring-red-100 outline-none"
              rows={3}
              placeholder="Nhập nội dung phản hồi của bạn..."
              value={replyText}
              onChange={(e) => setReplyText(e.target.value)}
            />
            <div className="flex justify-end gap-2 mt-2">
              <button
                onClick={() => {
                  setIsReplying(false);
                  setReplyText("");
                  setIsEditing(false);
                }}
                className="px-3 py-1.5 text-sm text-slate-600 hover:bg-slate-200 rounded transition-colors"
                disabled={loading}
              >
                Hủy
              </button>
              <button
                onClick={handleReplySubmit}
                className="px-3 py-1.5 text-sm bg-red-600 text-white hover:bg-red-700 rounded transition-colors"
                disabled={loading || !replyText.trim()}
              >
                {loading ? "Đang lưu..." : isEditing ? "Cập nhật" : "Gửi phản hồi"}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
