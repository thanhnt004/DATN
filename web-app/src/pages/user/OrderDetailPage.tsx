import { useState, useEffect, useRef } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { ArrowLeft, Package, Truck, MapPin, CreditCard, Clock, CheckCircle, XCircle, Star, Upload, X, Loader2, Image as ImageIcon } from "lucide-react";
import { getOrderById, cancelOrder, confirmDelivery, retryPayment } from "../../api/orderApi";
import { getPaymentByOrder } from "../../api/paymentApi";
import { getOrderReviews, createReview } from "../../api/reviewApi";
import { uploadFile } from "../../api/fileApi";
import { formatPrice, formatDateTime } from "../../utils/helpers";
import toast from "react-hot-toast";
import type { OrderResponse } from "../../types/order";
import type { PaymentResponse } from "../../types/payment";
import type { ReviewResponse, CreateReviewRequest } from "../../types/review";

const statusSteps = [
  { key: "PENDING", label: "Đặt hàng" },
  { key: "CONFIRMED", label: "Xác nhận" },
  { key: "SHIPPED", label: "Vận chuyển" },
  { key: "DELIVERED", label: "Đã giao" },
  { key: "COMPLETED", label: "Hoàn thành" },
];

export default function OrderDetailPage() {
  const { orderId } = useParams<{ orderId: string }>();
  const navigate = useNavigate();
  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [payment, setPayment] = useState<PaymentResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [acting, setActing] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [cancelReason, setCancelReason] = useState("");

  // Review state
  const [orderReviews, setOrderReviews] = useState<ReviewResponse[]>([]);
  const [showReviewModal, setShowReviewModal] = useState(false);
  const [reviewingItem, setReviewingItem] = useState<{ productId: string; skuId: string; productName: string } | null>(null);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState("");
  const [reviewAnonymous, setReviewAnonymous] = useState(false);
  const [submittingReview, setSubmittingReview] = useState(false);
  const [reviewImages, setReviewImages] = useState<string[]>([]);
  const [uploadingImage, setUploadingImage] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (!orderId) return;
    setLoading(true);
    Promise.all([
      getOrderById(orderId),
      getPaymentByOrder(orderId).catch(() => null),
    ])
      .then(([orderRes, paymentRes]) => {
        setOrder(orderRes.data.result);
        if (paymentRes) setPayment(paymentRes.data.result);
      })
      .catch(() => navigate("/user/orders"))
      .finally(() => setLoading(false));
  }, [orderId, navigate]);

  // Fetch existing reviews for this order
  useEffect(() => {
    if (!orderId) return;
    getOrderReviews(orderId)
      .then((res) => setOrderReviews(res.data.result))
      .catch(() => {});
  }, [orderId]);

  const handleCancel = async () => {
    if (!order || !cancelReason.trim()) return;
    setActing(true);
    try {
      const res = await cancelOrder(order.id, { reason: cancelReason });
      setOrder(res.data.result);
      setShowCancelModal(false);
      setCancelReason("");
    } catch {
      alert("Không thể hủy đơn hàng");
    } finally {
      setActing(false);
    }
  };

  const handleConfirmDelivery = async () => {
    if (!order) return;
    if (!confirm("Xác nhận đã nhận hàng?")) return;
    setActing(true);
    try {
      const res = await confirmDelivery(order.id);
      setOrder(res.data.result);
    } catch {
      alert("Lỗi xác nhận nhận hàng");
    } finally {
      setActing(false);
    }
  };

  const handleRetryPayment = async () => {
    if (!order) return;
    setActing(true);
    try {
      const res = await retryPayment(order.id);
      const updatedOrder = res.data.result;
      setOrder(updatedOrder);
      if (updatedOrder.paymentUrl) {
        window.location.replace(updatedOrder.paymentUrl);
      } else {
        toast.error("Không thể lấy link thanh toán");
      }
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || "Lỗi tạo lại link thanh toán";
      toast.error(msg);
    } finally {
      setActing(false);
    }
  };

  const openReviewModal = (item: { productId: string; skuId: string; productName: string }) => {
    setReviewingItem(item);
    setReviewRating(5);
    setReviewComment("");
    setReviewAnonymous(false);
    setReviewImages([]);
    setShowReviewModal(true);
  };

  const handleReviewImageUpload = async (file: File) => {
    if (!file.type.startsWith("image/")) {
      alert("Vui lòng chọn tệp hình ảnh");
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
      alert("Không thể tải lên hình ảnh");
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
    if (!order || !reviewingItem) return;
    setSubmittingReview(true);
    try {
      const payload: CreateReviewRequest = {
        productId: reviewingItem.productId,
        orderId: order.id,
        skuId: reviewingItem.skuId || undefined,
        rating: reviewRating,
        comment: reviewComment.trim() || undefined,
        images: reviewImages.length > 0 ? reviewImages : undefined,
        anonymous: reviewAnonymous,
      };
      const res = await createReview(payload);
      setOrderReviews((prev) => [...prev, res.data.result]);
      setShowReviewModal(false);
      setReviewingItem(null);
      setReviewImages([]);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || "Không thể gửi đánh giá";
      alert(msg);
    } finally {
      setSubmittingReview(false);
    }
  };

  const isItemReviewed = (productId: string) =>
    orderReviews.some((r) => r.productId === productId);

  // Determine step index for progress bar
  const getStepIndex = (status: string) => {
    if (status === "CANCELLED") return -1;
    const idx = statusSteps.findIndex((s) => s.key === status);
    return idx >= 0 ? idx : 0;
  };

  if (loading) {
    return (
      <div className="animate-pulse space-y-4">
        <div className="h-8 bg-gray-200 rounded w-1/3" />
        <div className="h-48 bg-gray-200 rounded" />
        <div className="h-32 bg-gray-200 rounded" />
      </div>
    );
  }

  if (!order) return null;

  const currentStep = getStepIndex(order.status);
  const isCancelled = order.status === "CANCELLED";

  return (
    <div>
      {/* Header */}
      <div className="flex items-center gap-3 mb-6">
        <Link to="/user/orders" className="p-2 hover:bg-gray-100 rounded-lg">
          <ArrowLeft className="w-5 h-5" />
        </Link>
        <div>
          <h1 className="text-lg font-bold text-gray-900">Chi tiết đơn hàng</h1>
          <p className="text-sm text-gray-500 font-mono">#{order.orderNumber}</p>
        </div>
      </div>

      {/* Status Progress */}
      {!isCancelled && (
        <div className="bg-white rounded-xl border border-gray-200 p-6 mb-4 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            {statusSteps.map((step, idx) => (
              <div key={step.key} className="flex flex-col items-center flex-1">
                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${
                    idx <= currentStep ? "bg-red-500 text-white" : "bg-gray-200 text-gray-400"
                  }`}
                >
                  {idx <= currentStep ? <CheckCircle className="w-5 h-5" /> : idx + 1}
                </div>
                <span className={`text-xs mt-1 ${idx <= currentStep ? "text-red-600 font-medium" : "text-gray-400"}`}>
                  {step.label}
                </span>
              </div>
            ))}
          </div>
          <div className="relative h-1 bg-gray-200 rounded-full">
            <div
              className="absolute left-0 top-0 h-full bg-red-500 rounded-full transition-all"
              style={{ width: `${currentStep >= 0 ? (currentStep / (statusSteps.length - 1)) * 100 : 0}%` }}
            />
          </div>
        </div>
      )}

      {isCancelled && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-4 mb-4 flex items-center gap-3 shadow-sm">
          <XCircle className="w-6 h-6 text-red-500" />
          <div>
            <p className="font-medium text-red-700">Đơn hàng đã bị hủy</p>
            {order.cancelReason && <p className="text-sm text-red-600">Lý do: {order.cancelReason}</p>}
          </div>
        </div>
      )}

      {/* Shipping Info */}
      <div className="bg-white rounded-xl border border-gray-200 p-6 mb-4 shadow-sm">
        <div className="flex items-center gap-2 mb-3">
          <MapPin className="w-5 h-5 text-red-500" />
          <h3 className="font-bold">Thông tin giao hàng</h3>
        </div>
        <div className="text-sm space-y-1">
          <p className="font-medium">{order.recipientName} | {order.recipientPhone}</p>
          <p className="text-gray-600">
            {[order.shippingAddress, order.shippingWard, order.shippingDistrict, order.shippingCity]
              .filter(Boolean)
              .join(", ")}
          </p>
          {order.shippingProvider && (
            <div className="flex items-center gap-2 mt-2 text-blue-600">
              <Truck className="w-4 h-4" />
              <span>{order.shippingProvider}</span>
              {order.trackingNumber && <span className="font-mono">- {order.trackingNumber}</span>}
            </div>
          )}
        </div>
      </div>

      {/* Order Items */}
      <div className="bg-white rounded-xl border border-gray-200 mb-4 overflow-hidden shadow-sm">
        <div className="p-4 sm:p-5 border-b bg-gray-50/50 flex items-center gap-2">
          <Package className="w-5 h-5 text-red-500" />
          <h3 className="font-bold text-gray-900">Sản phẩm</h3>
        </div>
        <div className="divide-y">
          {(order.items || []).map((item) => (
            <div key={item.id} className="p-4 sm:p-5 space-y-3">
              {/* Item header */}
              <div className="flex gap-3 sm:gap-4">
                <Link to={`/product/${item.productId}`} className="shrink-0">
                  <img
                    src={item.imageUrl || "https://placehold.co/80x80"}
                    alt={item.productName}
                    className="w-20 h-20 sm:w-24 sm:h-24 object-cover rounded-lg border"
                  />
                </Link>
                <div className="flex-1 min-w-0">
                  <Link to={`/product/${item.productId}`} className="text-sm sm:text-base font-medium text-gray-900 hover:text-red-500 line-clamp-2">
                    {item.productName}
                  </Link>
                  {item.skuCode && (
                    <p className="text-xs text-gray-500 mt-1">Phân loại: {item.skuCode}</p>
                  )}
                  {item.variantInfo && Object.keys(item.variantInfo).length > 0 && (
                    <p className="text-xs text-gray-500">
                      {Object.entries(item.variantInfo).map(([k, v]) => `${k}: ${v}`).join(", ")}
                    </p>
                  )}
                  <p className="text-xs text-gray-500 mt-1">Số lượng: x{item.quantity}</p>
                </div>
              </div>

              {/* Price row */}
              <div className="flex items-end justify-between pt-1">
                <div className="flex flex-col gap-0.5">
                  <p className="text-sm text-gray-500">
                    {formatPrice(item.unitPrice)} × {item.quantity}
                  </p>
                  <p className="text-lg sm:text-xl font-bold text-red-600">{formatPrice(item.subtotal)}</p>
                </div>
              </div>

              {/* Action section - visible when order completed and not reviewed */}
              {order.status === "COMPLETED" && (
                <div className="pt-2 space-y-2">
                  {!isItemReviewed(item.productId) ? (
                    <>
                      {/* Action buttons */}
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => openReviewModal({ productId: item.productId, skuId: item.skuId, productName: item.productName })}
                          className="px-4 py-2 text-sm bg-red-500 text-white rounded-lg hover:bg-red-600 font-medium transition-colors"
                        >
                          Đánh giá
                        </button>
                        <Link
                          to={`/product/${item.productId}`}
                          className="px-4 py-2 text-sm border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 font-medium transition-colors"
                        >
                          Mua lại
                        </Link>
                      </div>
                    </>
                  ) : (
                    <div className="flex items-center justify-between">
                      <span className="text-xs text-green-600 font-medium">✓ Đã đánh giá</span>
                      <Link
                        to={`/product/${item.productId}`}
                        className="px-4 py-2 text-sm border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 font-medium transition-colors"
                      >
                        Mua lại
                      </Link>
                    </div>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Payment Info */}
      <div className="bg-white rounded-xl border border-gray-200 p-6 mb-4 shadow-sm">
        <div className="flex items-center gap-2 mb-3">
          <CreditCard className="w-5 h-5 text-red-500" />
          <h3 className="font-bold">Thanh toán</h3>
        </div>
        <div className="space-y-2 text-sm">
          <div className="flex justify-between">
            <span className="text-gray-500">Phương thức</span>
            <span>{order.paymentMethod === "COD" ? "Thanh toán khi nhận hàng" : order.paymentMethod}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Trạng thái thanh toán</span>
            <span className={order.paymentStatus === "PAID" ? "text-green-600 font-medium" : "text-yellow-600"}>
              {order.paymentStatus === "PAID" ? "Đã thanh toán" : order.paymentStatus === "PENDING" ? "Chờ thanh toán" : order.paymentStatus}
            </span>
          </div>
          {payment?.bankCode && (
            <div className="flex justify-between">
              <span className="text-gray-500">Ngân hàng</span>
              <span>{payment.bankCode}</span>
            </div>
          )}
          <div className="border-t pt-3 space-y-1">
            <div className="flex justify-between">
              <span className="text-gray-500">Tạm tính</span>
              <span>{formatPrice(order.subtotal)}</span>
            </div>
            {order.discountAmount > 0 && (
              <div className="flex justify-between text-green-600">
                <span>Voucher Shop A</span>
                <span>-{formatPrice(order.discountAmount)}</span>
              </div>
            )}
            {order.platformVoucherShare > 0 && (
              <div className="flex justify-between text-green-600">
                <span>Voucher Sellico (chia)</span>
                <span>-{formatPrice(order.platformVoucherShare)}</span>
              </div>
            )}
            <div className="flex justify-between">
              <span className="text-gray-500">Phí vận chuyển</span>
              <span>{order.shippingFee ? formatPrice(order.shippingFee) : "Miễn phí"}</span>
            </div>
            <div className="flex justify-between border-t pt-2">
              <span className="font-bold text-lg">Thanh toán</span>
              <span className="font-bold text-lg text-red-600">{formatPrice(order.totalAmount)}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Timeline */}
      <div className="bg-white rounded-xl border border-gray-200 p-6 mb-4 shadow-sm">
        <div className="flex items-center gap-2 mb-3">
          <Clock className="w-5 h-5 text-red-500" />
          <h3 className="font-bold">Lịch sử</h3>
        </div>
        <div className="space-y-3 text-sm">
          <div className="flex justify-between">
            <span className="text-gray-500">Đặt hàng</span>
            <span>{formatDateTime(order.createdAt)}</span>
          </div>
          {order.confirmedAt && (
            <div className="flex justify-between">
              <span className="text-gray-500">Xác nhận</span>
              <span>{formatDateTime(order.confirmedAt)}</span>
            </div>
          )}
          {order.shippedAt && (
            <div className="flex justify-between">
              <span className="text-gray-500">Giao hàng</span>
              <span>{formatDateTime(order.shippedAt)}</span>
            </div>
          )}
          {order.deliveredAt && (
            <div className="flex justify-between">
              <span className="text-gray-500">Nhận hàng</span>
              <span>{formatDateTime(order.deliveredAt)}</span>
            </div>
          )}
          {order.completedAt && (
            <div className="flex justify-between text-green-600">
              <span>Hoàn thành</span>
              <span>{formatDateTime(order.completedAt)}</span>
            </div>
          )}
          {order.cancelledAt && (
            <div className="flex justify-between text-red-600">
              <span>Hủy</span>
              <span>{formatDateTime(order.cancelledAt)}</span>
            </div>
          )}
        </div>
      </div>

      {/* Actions */}
      <div className="flex justify-end gap-3">
        {order.paymentStatus === "PENDING" && order.paymentMethod !== "COD" && (
          <button
            onClick={handleRetryPayment}
            disabled={acting}
            className="px-6 py-2.5 bg-green-500 text-white rounded-lg hover:bg-green-600 font-medium disabled:bg-gray-300"
          >
            {acting ? "Đang xử lý..." : "Thanh toán lại"}
          </button>
        )}
        {order.status === "PENDING" && order.paymentStatus !== "PENDING" && (
          <button
            onClick={() => setShowCancelModal(true)}
            disabled={acting}
            className="px-6 py-2.5 border border-red-300 text-red-500 rounded-lg hover:bg-red-50 font-medium"
          >
            Hủy đơn hàng
          </button>
        )}
        {order.status === "DELIVERED" && (
          <button
            onClick={handleConfirmDelivery}
            disabled={acting}
            className="px-6 py-2.5 bg-green-500 text-white rounded-lg hover:bg-green-600 font-medium"
          >
            {acting ? "Đang xử lý..." : "Đã nhận hàng"}
          </button>
        )}
      </div>

      {/* Cancel Modal */}
      {showCancelModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg p-6 max-w-md w-full">
            <h3 className="font-bold text-lg mb-4">Hủy đơn hàng</h3>
            <textarea
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              placeholder="Vui lòng nhập lý do hủy đơn..."
              className="w-full border rounded-lg p-3 text-sm resize-none h-24 mb-4"
            />
            <div className="flex justify-end gap-3">
              <button
                onClick={() => { setShowCancelModal(false); setCancelReason(""); }}
                className="px-4 py-2 text-sm border rounded-lg hover:bg-gray-50"
              >
                Đóng
              </button>
              <button
                onClick={handleCancel}
                disabled={!cancelReason.trim() || acting}
                className="px-4 py-2 text-sm bg-red-500 text-white rounded-lg hover:bg-red-600 disabled:bg-gray-300"
              >
                {acting ? "Đang hủy..." : "Xác nhận hủy"}
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
                    alert("Tối đa 3 hình ảnh");
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
