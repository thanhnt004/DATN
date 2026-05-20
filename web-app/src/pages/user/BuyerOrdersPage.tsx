import { useState, useEffect, useCallback, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Package, ChevronRight, Truck, Clock, CheckCircle, XCircle, Star, Upload, Loader2, X, Image as ImageIcon } from "lucide-react";
import toast from "react-hot-toast";
import { getMyOrders, cancelOrder, retryPayment } from "../../api/orderApi";
import { getOrderReviews, createReview } from "../../api/reviewApi";
import { uploadFile } from "../../api/fileApi";
import { formatPrice, formatDateTime } from "../../utils/helpers";
import type { OrderResponse, OrderStatus } from "../../types/order";
import type { ReviewResponse, CreateReviewRequest } from "../../types/review";

const STATUS_TABS: { value: string; label: string }[] = [
  { value: "", label: "Tất cả" },
  { value: "PAYMENT_PENDING", label: "Chưa thanh toán" },
  { value: "PENDING", label: "Chờ xác nhận" },
  { value: "CONFIRMED", label: "Đã xác nhận" },
  { value: "SHIPPED", label: "Đang giao" },
  { value: "DELIVERED", label: "Đã giao" },
  { value: "COMPLETED", label: "Hoàn thành" },
  { value: "CANCELLED", label: "Đã hủy" },
];

const statusConfig: Record<string, { color: string; bg: string; icon: React.ReactNode; label: string }> = {
  PENDING: { color: "text-yellow-700", bg: "bg-yellow-50", icon: <Clock className="w-4 h-4" />, label: "Chờ xác nhận" },
  CONFIRMED: { color: "text-blue-700", bg: "bg-blue-50", icon: <CheckCircle className="w-4 h-4" />, label: "Đã xác nhận" },
  SHIPPED: { color: "text-blue-700", bg: "bg-blue-50", icon: <Truck className="w-4 h-4" />, label: "Đang giao" },
  DELIVERED: { color: "text-green-700", bg: "bg-green-50", icon: <CheckCircle className="w-4 h-4" />, label: "Đã giao" },
  COMPLETED: { color: "text-green-700", bg: "bg-green-50", icon: <CheckCircle className="w-4 h-4" />, label: "Hoàn thành" },
  CANCELLED: { color: "text-red-700", bg: "bg-red-50", icon: <XCircle className="w-4 h-4" />, label: "Đã hủy" },
};

export default function BuyerOrdersPage() {
  const navigate = useNavigate();
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [cancellingId, setCancellingId] = useState<string | null>(null);
  const [cancelReason, setCancelReason] = useState("");
  const [showCancelModal, setShowCancelModal] = useState<string | null>(null);
  const [retryingPaymentId, setRetryingPaymentId] = useState<string | null>(null);

  // Review state
  const [orderReviews, setOrderReviews] = useState<ReviewResponse[]>([]);
  const [showReviewModal, setShowReviewModal] = useState(false);
  const [reviewingItem, setReviewingItem] = useState<{ orderId: string; productId: string; skuId: string; productName: string } | null>(null);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState("");
  const [reviewAnonymous, setReviewAnonymous] = useState(false);
  const [submittingReview, setSubmittingReview] = useState(false);
  const [reviewImages, setReviewImages] = useState<string[]>([]);
  const [uploadingImage, setUploadingImage] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const fetchOrders = useCallback(async () => {
    setLoading(true);
    try {
      let data;
      // Handle PAYMENT_PENDING tab: filter client-side for orders with PENDING payment and non-COD method
      if (activeTab === "PAYMENT_PENDING") {
        const res = await getMyOrders({
          page: 0,
          size: 100, // Fetch more to filter locally
        });
        const allOrders = res.data.result?.content ?? [];
        const filtered = allOrders.filter(
          (o) => o.paymentStatus === "PENDING" && o.paymentMethod !== "COD"
        );
        const paginatedOrders = filtered.slice(page * 10, (page + 1) * 10);
        setOrders(paginatedOrders);
        setTotalPages(Math.ceil(filtered.length / 10));
        data = { content: paginatedOrders };
      } else {
        const res = await getMyOrders({
          status: activeTab || undefined,
          page,
          size: 10,
        });
        data = res.data.result;
        setOrders(data?.content ?? []);
        setTotalPages(data?.totalPages ?? 0);
      }

      // Fetch reviews for completed orders
      const completedOrders = activeTab !== "PAYMENT_PENDING" ? (data?.content ?? []).filter((o) => o.status === "COMPLETED") : [];
      if (completedOrders.length > 0 && activeTab !== "PAYMENT_PENDING") {
        try {
          const allReviews: ReviewResponse[] = [];
          for (const order of completedOrders) {
            const reviewRes = await getOrderReviews(order.id);
            allReviews.push(...(reviewRes.data.result ?? []));
          }
          setOrderReviews(allReviews);
        } catch {
          // Ignore review fetch errors
        }
      } else {
        setOrderReviews([]);
      }
    } catch {
      setOrders([]);
    } finally {
      setLoading(false);
    }
  }, [activeTab, page]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  const handleCancel = async (orderId: string) => {
    if (!cancelReason.trim()) return;
    setCancellingId(orderId);
    try {
      await cancelOrder(orderId, { reason: cancelReason });
      setShowCancelModal(null);
      setCancelReason("");
      fetchOrders();
    } catch {
      alert("Không thể hủy đơn hàng");
    } finally {
      setCancellingId(null);
    }
  };

  const handleRetryPayment = async (orderId: string) => {
    setRetryingPaymentId(orderId);
    try {
      const res = await retryPayment(orderId);
      const updatedOrder = res.data.result;
      if (updatedOrder.paymentUrl) {
        window.location.replace(updatedOrder.paymentUrl);
      } else {
        toast.error("Không thể lấy link thanh toán");
      }
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || "Lỗi tạo lại link thanh toán";
      toast.error(msg);
    } finally {
      setRetryingPaymentId(null);
    }
  };

  const getStatusDisplay = (status: OrderStatus) => {
    const config = statusConfig[status] ?? { color: "text-gray-700", bg: "bg-gray-50", icon: null, label: status };
    return config;
  };

  const openReviewModal = (orderId: string, item: { productId: string; skuId: string; productName: string }) => {
    setReviewingItem({ orderId, ...item });
    setReviewRating(5);
    setReviewComment("");
    setReviewAnonymous(false);
    setReviewImages([]);
    setShowReviewModal(true);
  };

  const handleReviewImageUpload = async (file: File) => {
    if (!file.type.startsWith("image/")) {
      toast.error("Vui lòng chọn tệp hình ảnh");
      return;
    }
    setUploadingImage(true);
    try {
      const resp = await uploadFile(file, "reviews");
      const uploaded = resp.data.result;
      const imageUrl = uploaded.secureUrl || uploaded.url;
      setReviewImages((prev) => [...prev, imageUrl]);
    } catch (err) {
      console.error("Upload failed:", err);
      toast.error("Không thể tải lên hình ảnh");
    } finally {
      setUploadingImage(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

  const removeReviewImage = (index: number) => {
    setReviewImages((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmitReview = async () => {
    if (!reviewingItem) return;
    setSubmittingReview(true);
    try {
      const payload: CreateReviewRequest = {
        productId: reviewingItem.productId,
        orderId: reviewingItem.orderId,
        skuId: reviewingItem.skuId || undefined,
        rating: reviewRating,
        comment: reviewComment.trim() || undefined,
        images: reviewImages.length > 0 ? reviewImages : undefined,
        anonymous: reviewAnonymous,
      };
      await createReview(payload);
      toast.success("Đánh giá sản phẩm thành công");
      setShowReviewModal(false);
      setReviewingItem(null);
      setReviewImages([]);
      fetchOrders();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || "Không thể gửi đánh giá";
      toast.error(msg);
    } finally {
      setSubmittingReview(false);
    }
  };

  const isItemReviewed = (productId: string): boolean => {
    return orderReviews.some((r) => r.productId === productId);
  };

  return (
    <div>
      <h1 className="text-xl font-bold text-gray-900 mb-6">Đơn hàng của tôi</h1>

      {/* Tabs */}
      <div className="flex flex-wrap gap-2 mb-6 border-b pb-3">
        {STATUS_TABS.map((tab) => (
          <button
            key={tab.value}
            onClick={() => { setActiveTab(tab.value); setPage(0); }}
            className={`px-4 py-2 rounded-full text-sm font-medium transition ${
              activeTab === tab.value
                ? "bg-red-500 text-white"
                : "bg-gray-100 text-gray-600 hover:bg-gray-200"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="animate-pulse bg-gray-200 rounded-lg h-32" />
          ))}
        </div>
      ) : orders.length === 0 ? (
        <div className="text-center py-16">
          <Package className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <p className="text-gray-500 text-lg">Chưa có đơn hàng nào</p>
          <Link to="/" className="text-red-500 hover:text-red-600 text-sm mt-2 inline-block">
            Mua sắm ngay
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {orders.map((order) => {
            const status = getStatusDisplay(order.status);
            return (
              <div key={order.id} className="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm hover:shadow-md transition-shadow">
                {/* Order Header */}
                <div className="flex items-center justify-between p-4 sm:p-5 border-b bg-gray-50/50">
                  <div className="flex items-center gap-3">
                    <span className="text-sm font-mono text-gray-500 font-semibold">#{order.orderNumber}</span>
                    <span className={`flex items-center gap-1 text-xs font-bold px-3 py-1 rounded-full ${status.bg} ${status.color}`}>
                      {status.icon}
                      {status.label}
                    </span>
                  </div>
                  <span className="text-xs text-gray-500">{formatDateTime(order.createdAt)}</span>
                </div>

                {/* Items */}
                <div className="space-y-3">
                  {(order.items || []).slice(0, 3).map((item) => (
                    <div key={item.id} className="p-3 sm:p-4 border-b last:border-b-0 space-y-2">
                      {/* Item product info */}
                      <div className="flex gap-3">
                        <Link to={`/product/${item.productId}`} className="shrink-0">
                          <img
                            src={item.imageUrl || "https://placehold.co/60x60"}
                            alt={item.productName}
                            className="w-16 h-16 sm:w-20 sm:h-20 object-cover rounded-lg border"
                          />
                        </Link>
                        <div className="flex-1 min-w-0">
                          <Link to={`/product/${item.productId}`} className="text-sm font-medium text-gray-900 hover:text-red-500 line-clamp-2">
                            {item.productName}
                          </Link>
                          {item.skuCode && (
                            <p className="text-xs text-gray-500 mt-1">Phân loại: {item.skuCode}</p>
                          )}
                          <p className="text-xs text-gray-500 mt-1">x{item.quantity}</p>
                        </div>
                        <div className="text-right shrink-0">
                          <p className="text-xs text-gray-500">{formatPrice(item.unitPrice)}</p>
                          <p className="text-sm font-bold text-red-600">{formatPrice(item.subtotal)}</p>
                        </div>
                      </div>

                      {/* Review/action section for completed orders */}
                      {order.status === "COMPLETED" && (
                        <div className="pt-2 flex items-center gap-2">
                          {!isItemReviewed(item.productId) ? (
                            <>
                              <button
                                onClick={() => openReviewModal(order.id, { productId: item.productId, skuId: item.skuId, productName: item.productName })}
                                className="px-3 py-2 text-xs bg-red-500 text-white rounded-lg hover:bg-red-600 font-medium transition-colors"
                              >
                                Đánh giá
                              </button>
                              <Link
                                to={`/product/${item.productId}`}
                                className="px-3 py-2 text-xs border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 font-medium transition-colors"
                              >
                                Mua lại
                              </Link>
                            </>
                          ) : (
                            <>
                              <span className="text-xs text-green-600 font-medium">✓ Đã đánh giá</span>
                              <Link
                                to={`/product/${item.productId}`}
                                className="px-3 py-2 text-xs border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 font-medium transition-colors"
                              >
                                Mua lại
                              </Link>
                            </>
                          )}
                        </div>
                      )}
                    </div>
                  ))}
                  {(order.items || []).length > 3 && (
                    <div className="text-center text-xs text-gray-500 py-2 border-t">
                      +{(order.items || []).length - 3} sản phẩm khác
                    </div>
                  )}
                </div>

                {/* Footer */}
                <div className="flex items-center justify-between p-4 sm:p-5 border-t bg-gray-50">
                  <div className="text-sm sm:text-base">
                    <span className="text-gray-500">Tổng: </span>
                    <span className="font-bold text-red-600 text-lg sm:text-xl">{formatPrice(order.totalAmount)}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    {order.paymentStatus !== "PENDING" && (
                      <button
                        onClick={() => navigate(`/user/messages?peerId=${encodeURIComponent(order.sellerId || "")}`)}
                        className="px-4 py-2 text-sm border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 font-medium transition-colors"
                      >
                        Chat với shop
                      </button>
                    )}
                    {order.paymentStatus === "PENDING" && order.paymentMethod !== "COD" && (
                      <button
                        onClick={() => handleRetryPayment(order.id)}
                        disabled={retryingPaymentId === order.id}
                        className="px-4 py-2 text-sm bg-green-500 text-white rounded-lg hover:bg-green-600 font-medium transition-colors disabled:bg-gray-300"
                      >
                        {retryingPaymentId === order.id ? "Đang xử lý..." : "Thanh toán lại"}
                      </button>
                    )}
                    {order.status === "PENDING" && order.paymentStatus !== "PENDING" && (
                      <button
                        onClick={() => setShowCancelModal(order.id)}
                        className="px-4 py-2 text-sm border border-red-300 text-red-500 rounded-lg hover:bg-red-50 font-medium transition-colors"
                      >
                        Hủy đơn
                      </button>
                    )}
                    <Link
                      to={`/user/orders/${order.id}`}
                      className="flex items-center gap-1 px-4 py-2 text-sm bg-red-500 text-white rounded-lg hover:bg-red-600 font-medium transition-colors"
                    >
                      Chi tiết <ChevronRight className="w-4 h-4" />
                    </Link>
                  </div>
                </div>
              </div>
            );
          })}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 mt-6">
              <button
                onClick={() => setPage(Math.max(0, page - 1))}
                disabled={page === 0}
                className="px-3 py-1.5 text-sm border rounded-lg disabled:opacity-50"
              >
                Trước
              </button>
              <span className="text-sm text-gray-600">
                {page + 1} / {totalPages}
              </span>
              <button
                onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                disabled={page >= totalPages - 1}
                className="px-3 py-1.5 text-sm border rounded-lg disabled:opacity-50"
              >
                Sau
              </button>
            </div>
          )}
        </div>
      )}

      {/* Cancel Modal */}
      {showCancelModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg p-6 max-w-md w-full">
            <h3 className="font-bold text-lg mb-4">Hủy đơn hàng</h3>
            <textarea
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              placeholder="Lý do hủy đơn..."
              className="w-full border rounded-lg p-3 text-sm resize-none h-24 mb-4"
            />
            <div className="flex justify-end gap-3">
              <button
                onClick={() => { setShowCancelModal(null); setCancelReason(""); }}
                className="px-4 py-2 text-sm border rounded-lg hover:bg-gray-50"
              >
                Đóng
              </button>
              <button
                onClick={() => handleCancel(showCancelModal)}
                disabled={!cancelReason.trim() || cancellingId === showCancelModal}
                className="px-4 py-2 text-sm bg-red-500 text-white rounded-lg hover:bg-red-600 disabled:bg-gray-300"
              >
                {cancellingId === showCancelModal ? "Đang hủy..." : "Xác nhận hủy"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Review Modal */}
      {showReviewModal && reviewingItem && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg p-6 max-w-md w-full max-h-[90vh] overflow-y-auto">
            <h3 className="font-bold text-lg mb-1">Đánh giá sản phẩm</h3>
            <p className="text-sm text-gray-500 mb-4 truncate">{reviewingItem.productName}</p>

            {/* Star rating */}
            <div className="flex items-center gap-2 mb-4">
              <span className="text-sm text-gray-600">Chất lượng:</span>
              <div className="flex gap-1">
                {[1, 2, 3, 4, 5].map((s) => (
                  <button key={s} onClick={() => setReviewRating(s)} className="focus:outline-none">
                    <Star
                      className={`w-7 h-7 transition-colors ${
                        s <= reviewRating ? "text-yellow-400 fill-yellow-400" : "text-gray-300"
                      }`}
                    />
                  </button>
                ))}
              </div>
              <span className="text-sm text-gray-500">
                {reviewRating === 1 ? "Tệ" : reviewRating === 2 ? "Không hài lòng" : reviewRating === 3 ? "Bình thường" : reviewRating === 4 ? "Hài lòng" : "Tuyệt vời"}
              </span>
            </div>

            {/* Comment */}
            <textarea
              value={reviewComment}
              onChange={(e) => setReviewComment(e.target.value)}
              placeholder="Chia sẻ nhận xét của bạn về sản phẩm..."
              className="w-full border rounded-lg p-3 text-sm resize-none h-28 mb-3 focus:border-red-300 focus:ring-1 focus:ring-red-300 outline-none"
            />

            {/* Image Upload Section */}
            <div className="mb-4">
              <label className="block text-sm font-semibold text-gray-700 mb-2">
                <ImageIcon className="w-4 h-4 inline-block mr-1" />
                Hình ảnh (tối đa 3 ảnh)
              </label>
              
              {/* Upload Button */}
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file && reviewImages.length < 3) {
                    handleReviewImageUpload(file);
                  } else if (reviewImages.length >= 3) {
                    toast.error("Tối đa 3 hình ảnh");
                  }
                  e.target.value = "";
                }}
                disabled={uploadingImage || reviewImages.length >= 3}
              />

              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={uploadingImage || reviewImages.length >= 3}
                className="w-full flex items-center justify-center gap-2 px-3 py-2 border-2 border-dashed border-gray-300 rounded-lg hover:border-red-300 hover:bg-red-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                {uploadingImage ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span className="text-sm text-gray-600">Đang tải...</span>
                  </>
                ) : (
                  <>
                    <Upload className="w-4 h-4 text-gray-600" />
                    <span className="text-sm text-gray-600">
                      {reviewImages.length >= 3 ? "Đã đạt số lượng tối đa" : "Chọn hình ảnh"}
                    </span>
                  </>
                )}
              </button>

              {/* Image Previews */}
              {reviewImages.length > 0 && (
                <div className="grid grid-cols-3 gap-2 mt-3">
                  {reviewImages.map((img, idx) => (
                    <div key={idx} className="relative group">
                      <img
                        src={img}
                        alt={`Preview ${idx + 1}`}
                        className="w-full h-20 object-cover rounded-lg border border-gray-200"
                      />
                      <button
                        type="button"
                        onClick={() => removeReviewImage(idx)}
                        className="absolute top-1 right-1 bg-red-500 text-white rounded-full p-1 opacity-0 group-hover:opacity-100 transition-opacity"
                      >
                        <X className="w-3 h-3" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
              <p className="text-xs text-gray-500 mt-2">
                {reviewImages.length}/3 ảnh đã tải
              </p>
            </div>

            {/* Anonymous option */}
            <label className="flex items-center gap-2 mb-4 cursor-pointer text-sm text-gray-600">
              <input
                type="checkbox"
                checked={reviewAnonymous}
                onChange={(e) => setReviewAnonymous(e.target.checked)}
                className="rounded border-gray-300 text-red-500 focus:ring-red-400"
              />
              Đánh giá ẩn danh
            </label>

            <div className="flex justify-end gap-3">
              <button
                onClick={() => { setShowReviewModal(false); setReviewingItem(null); setReviewImages([]); }}
                className="px-4 py-2 text-sm border rounded-lg hover:bg-gray-50"
              >
                Đóng
              </button>
              <button
                onClick={handleSubmitReview}
                disabled={submittingReview || uploadingImage}
                className="px-6 py-2 text-sm bg-red-500 text-white rounded-lg hover:bg-red-600 disabled:bg-gray-300 font-medium"
              >
                {submittingReview ? "Đang gửi..." : "Gửi đánh giá"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
