import { useEffect, useState, useCallback } from "react";
import { useSearchParams, Link } from "react-router-dom";
import { CheckCircle, XCircle, Loader2 } from "lucide-react";
import { vnPayCallback, getPaymentByOrder } from "../api/paymentApi";
import { formatPrice } from "../utils/helpers";

export default function PaymentResultPage() {
  const [searchParams] = useSearchParams();
  const [status, setStatus] = useState<"loading" | "success" | "failed">("loading");
  const [orderId, setOrderId] = useState<string | null>(null);
  const [amount, setAmount] = useState<number>(0);

  const processResult = useCallback(async () => {
    const directStatus = searchParams.get("status");
    const directOrderId = searchParams.get("orderId");
    const vnpaySuccess = searchParams.get("success");

    // Handle direct status (COD payments)
    if (directStatus === "success" && directOrderId) {
      setOrderId(directOrderId);
      try {
        const res = await getPaymentByOrder(directOrderId);
        setAmount(res.data.result.amount);
      } catch { /* */ }
      setStatus("success");
      return;
    }

    // Handle VNPay return redirect (success=true/false)
    if (vnpaySuccess !== null && directOrderId) {
      setOrderId(directOrderId);
      try {
        const res = await getPaymentByOrder(directOrderId);
        setAmount(res.data.result.amount);
      } catch { /* */ }
      setStatus(vnpaySuccess === "true" ? "success" : "failed");
      return;
    }

    // Handle VNPay IPN callback params
    const vnpParams: Record<string, string> = {};
    searchParams.forEach((value, key) => {
      vnpParams[key] = value;
    });

    if (vnpParams.vnp_ResponseCode) {
      try {
        const res = await vnPayCallback(vnpParams);
        const payment = res.data.result;
        setOrderId(payment.orderId);
        setAmount(payment.amount);
        setStatus(payment.status === "COMPLETED" || payment.status === "COD_PENDING" ? "success" : "failed");
      } catch {
        setStatus("failed");
      }
    } else {
      setStatus("failed");
    }
  }, [searchParams]);

  useEffect(() => {
    processResult();
  }, [processResult]);

  if (status === "loading") {
    return (
      <div className="max-w-lg mx-auto px-4 py-20 text-center">
        <Loader2 className="w-16 h-16 text-red-500 animate-spin mx-auto mb-4" />
        <p className="text-lg text-gray-600">Đang xử lý kết quả thanh toán...</p>
      </div>
    );
  }

  return (
    <div className="max-w-lg mx-auto px-4 py-20 text-center">
      {status === "success" ? (
        <>
          <div className="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <CheckCircle className="w-12 h-12 text-green-500" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900 mb-2">Đặt hàng thành công!</h1>
          <p className="text-gray-500 mb-2">Cảm ơn bạn đã mua hàng tại Sellico</p>
          {amount > 0 && (
            <p className="text-lg font-semibold text-red-600 mb-6">
              Tổng thanh toán: {formatPrice(amount)}
            </p>
          )}
          <div className="flex flex-col sm:flex-row gap-3 justify-center">
            {orderId && (
              <Link
                to={`/user/orders/${orderId}`}
                className="px-6 py-2.5 bg-red-500 text-white rounded-lg font-medium hover:bg-red-600"
              >
                Xem đơn hàng
              </Link>
            )}
            <Link
              to="/user/orders"
              className="px-6 py-2.5 border rounded-lg font-medium hover:bg-gray-50"
            >
              Tất cả đơn hàng
            </Link>
            <Link
              to="/"
              className="px-6 py-2.5 border rounded-lg font-medium hover:bg-gray-50"
            >
              Tiếp tục mua sắm
            </Link>
          </div>
        </>
      ) : (
        <>
          <div className="w-20 h-20 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <XCircle className="w-12 h-12 text-red-500" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900 mb-2">Thanh toán thất bại</h1>
          <p className="text-gray-500 mb-6">Vui lòng thử lại hoặc chọn phương thức thanh toán khác</p>
          <div className="flex flex-col sm:flex-row gap-3 justify-center">
            <Link
              to="/user/orders"
              className="px-6 py-2.5 bg-red-500 text-white rounded-lg font-medium hover:bg-red-600"
            >
              Xem đơn hàng
            </Link>
            <Link
              to="/"
              className="px-6 py-2.5 border rounded-lg font-medium hover:bg-gray-50"
            >
              Trang chủ
            </Link>
          </div>
        </>
      )}
    </div>
  );
}
