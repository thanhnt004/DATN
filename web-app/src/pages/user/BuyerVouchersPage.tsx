import { useState, useEffect, useCallback } from "react";
import { Tag, Clock, CheckCircle, Ticket, Gift, Copy, Check } from "lucide-react";
import { getMyCoupons, getAvailableCoupons, getCouponById, claimCoupon } from "../../api/couponApi";
import { formatPrice, formatDate } from "../../utils/helpers";
import type { UserCouponResponse, CouponResponse } from "../../types/coupon";

type Tab = "my" | "available";

const normalizeCouponCode = (value?: string | null) => (value ?? "").trim();

const enrichUserCoupons = async (coupons: UserCouponResponse[]) => {
  const uniqueCouponIds = Array.from(new Set(coupons.map((coupon) => coupon.couponId).filter(Boolean)));
  const couponMap = new Map<string, CouponResponse>();

  await Promise.all(uniqueCouponIds.map(async (couponId) => {
    try {
      const res = await getCouponById(couponId);
      if (res.data?.result) {
        couponMap.set(couponId, res.data.result);
      }
    } catch {
      // ignore missing coupon details
    }
  }));

  return coupons.map((coupon) => {
    const details = couponMap.get(coupon.couponId);
    return {
      ...coupon,
      couponCode: normalizeCouponCode(coupon.couponCode || details?.code),
      discountType: coupon.discountType || details?.discountType || "FIXED_AMOUNT",
      discountValue: coupon.discountValue ?? details?.discountValue ?? 0,
      minOrderAmount: coupon.minOrderAmount ?? details?.minOrderAmount ?? null,
      maxDiscountAmount: coupon.maxDiscountAmount ?? details?.maxDiscountAmount ?? null,
      couponEndDate: coupon.couponEndDate ?? details?.endDate ?? coupon.couponEndDate ?? "",
    };
  });
};

export default function BuyerVouchersPage() {
  const [tab, setTab] = useState<Tab>("my");
  const [myCoupons, setMyCoupons] = useState<UserCouponResponse[]>([]);
  const [availableCoupons, setAvailableCoupons] = useState<CouponResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [claimingId, setClaimingId] = useState<string | null>(null);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const safeFormatDate = (value?: string | null) => {
    if (!value) return "";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "";
    return date.toLocaleDateString("vi-VN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    });
  };

  const safeDiscountValue = (value?: number | null) => Number(value ?? 0);
  const safeDiscountType = (type?: string | null) => (type === "PERCENTAGE" ? "PERCENTAGE" : "FIXED_AMOUNT");

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      if (tab === "my") {
        const res = await getMyCoupons();
        const myList = Array.isArray(res.data.result) ? res.data.result : [];
        setMyCoupons(await enrichUserCoupons(myList));
      } else {
        const [myRes, availRes] = await Promise.all([
          getMyCoupons({ status: "AVAILABLE" }).catch(() => ({ data: { result: [] } })),
          getAvailableCoupons({ size: 50 }).catch(() => ({ data: { result: { content: [] } } })),
        ]);

        const myList = Array.isArray(myRes.data.result) ? myRes.data.result : [];
        const enrichedMyList = await enrichUserCoupons(myList);
        const ownedIds = new Set(enrichedMyList.map((coupon) => coupon.couponId));
        const ownedCodes = new Set(enrichedMyList.map((coupon) => (normalizeCouponCode(coupon.couponCode)).toUpperCase()));
        const avail = availRes.data.result.content ?? [];
        setMyCoupons(enrichedMyList);
        setAvailableCoupons(
          avail.filter(
            (coupon) => !ownedIds.has(coupon.id) && !ownedCodes.has((coupon.code ?? "").toUpperCase())
          )
        );
      }
    } catch {
      /* ignore */
    } finally {
      setLoading(false);
    }
  }, [tab]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleClaim = async (couponId: string) => {
    setClaimingId(couponId);
    try {
      await claimCoupon(couponId);
      fetchData();
    } catch {
      alert("Không thể nhận voucher");
    } finally {
      setClaimingId(null);
    }
  };

  const handleCopy = (code: string, id: string) => {
    navigator.clipboard.writeText(code);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  return (
    <div>
      <h1 className="text-xl font-bold text-gray-900 mb-6">Voucher của tôi</h1>

      {/* Tabs */}
      <div className="flex gap-2 mb-6">
        <button
          onClick={() => setTab("my")}
          className={`flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium transition ${
            tab === "my" ? "bg-red-500 text-white" : "bg-gray-100 text-gray-600 hover:bg-gray-200"
          }`}
        >
          <Ticket className="w-4 h-4" />
          Voucher của tôi
        </button>
        <button
          onClick={() => setTab("available")}
          className={`flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium transition ${
            tab === "available" ? "bg-red-500 text-white" : "bg-gray-100 text-gray-600 hover:bg-gray-200"
          }`}
        >
          <Gift className="w-4 h-4" />
          Nhận voucher
        </button>
      </div>

      {loading ? (
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="animate-pulse bg-gray-200 rounded-lg h-24" />
          ))}
        </div>
      ) : tab === "my" ? (
        /* My Coupons */
        myCoupons.length === 0 ? (
          <div className="text-center py-16">
            <Ticket className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-500">Bạn chưa có voucher nào</p>
            <button
              onClick={() => setTab("available")}
              className="text-red-500 hover:text-red-600 text-sm mt-2"
            >
              Nhận voucher ngay
            </button>
          </div>
        ) : (
          <div className="space-y-3">
            {myCoupons.map((coupon) => {
              const endDate = coupon.couponEndDate ?? coupon.endDate ?? coupon.claimedAt ?? "";
              const isExpired = coupon.status === "EXPIRED" || new Date(endDate) < new Date();
              const isUsed = coupon.status === "USED";
              const couponCode = normalizeCouponCode(coupon.couponCode || coupon.code || coupon.id || "");
              const discountType = safeDiscountType(coupon.discountType);
              const discountValue = safeDiscountValue(coupon.discountValue);

              return (
                <div
                  key={coupon.id}
                  className={`flex items-stretch rounded-lg border overflow-hidden ${
                    isExpired || isUsed ? "opacity-60" : ""
                  }`}
                >
                  {/* Left Color Strip */}
                  <div className={`w-2 ${isUsed ? "bg-gray-400" : isExpired ? "bg-red-400" : "bg-red-500"}`} />

                  {/* Content */}
                  <div className="flex-1 p-4">
                    <div className="flex items-start justify-between">
                      <div>
                        <div className="flex items-center gap-2">
                          <Tag className="w-4 h-4 text-red-500" />
                          <span className="font-mono font-bold text-sm">{couponCode}</span>
                          <button
                            onClick={() => handleCopy(couponCode, coupon.id)}
                            className="text-gray-400 hover:text-gray-600"
                          >
                            {copiedId === coupon.id ? (
                              <Check className="w-3.5 h-3.5 text-green-500" />
                            ) : (
                              <Copy className="w-3.5 h-3.5" />
                            )}
                          </button>
                        </div>
                        <p className="text-sm text-gray-600 mt-1">
                          Giảm{" "}
                          {discountType === "PERCENTAGE"
                            ? `${discountValue}%`
                            : formatPrice(discountValue)}
                          {coupon.maxDiscountAmount ? ` (tối đa ${formatPrice(safeDiscountValue(coupon.maxDiscountAmount))})` : ""}
                        </p>
                        {coupon.minOrderAmount != null && coupon.minOrderAmount > 0 && (
                          <p className="text-xs text-gray-500">Đơn tối thiểu {formatPrice(coupon.minOrderAmount)}</p>
                        )}
                      </div>

                      <div className="text-right">
                        {isUsed ? (
                          <span className="flex items-center gap-1 text-xs text-gray-500">
                            <CheckCircle className="w-3.5 h-3.5" /> Đã dùng
                          </span>
                        ) : isExpired ? (
                          <span className="text-xs text-red-500">Hết hạn</span>
                        ) : (
                          <span className="text-xs text-green-600 font-medium">Có hiệu lực</span>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center gap-1 mt-2 text-xs text-gray-400">
                      <Clock className="w-3 h-3" />
                      <span>HSD: {safeFormatDate(endDate)}</span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )
      ) : (
        /* Available Coupons */
        availableCoupons.length === 0 ? (
          <div className="text-center py-16">
            <Gift className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-500">Hiện không có voucher nào khả dụng</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {availableCoupons.map((coupon) => {
              const discountType = safeDiscountType(coupon.discountType);
              const discountValue = safeDiscountValue(coupon.discountValue);
              const endDate = coupon.endDate ?? coupon.startDate ?? "";
              const couponCode = normalizeCouponCode(coupon.code || coupon.id || "");

              return (
                <div key={coupon.id} className="flex items-stretch rounded-lg border overflow-hidden bg-white">
                  <div className="w-2 bg-red-500" />
                  <div className="flex-1 p-4">
                    <div className="flex items-start justify-between">
                      <div>
                        <span className="font-mono font-bold text-sm text-red-600">{couponCode}</span>
                        <p className="text-sm text-gray-600 mt-1">
                          Giảm{" "}
                          {discountType === "PERCENTAGE"
                            ? `${discountValue}%`
                            : formatPrice(discountValue)}
                          {coupon.maxDiscountAmount ? ` (tối đa ${formatPrice(safeDiscountValue(coupon.maxDiscountAmount))})` : ""}
                        </p>
                        {coupon.minOrderAmount != null && coupon.minOrderAmount > 0 && (
                          <p className="text-xs text-gray-500">Đơn tối thiểu {formatPrice(coupon.minOrderAmount)}</p>
                        )}
                      </div>
                      <button
                        onClick={() => handleClaim(coupon.id)}
                        disabled={claimingId === coupon.id}
                        className="px-4 py-1.5 bg-red-500 text-white text-sm rounded-lg hover:bg-red-600 disabled:bg-gray-300 whitespace-nowrap"
                      >
                        {claimingId === coupon.id ? "..." : "Lưu"}
                      </button>
                    </div>
                    <div className="flex items-center justify-between mt-2 text-xs text-gray-400">
                      <span className="flex items-center gap-1">
                        <Clock className="w-3 h-3" />
                        HSD: {safeFormatDate(endDate)}
                      </span>
                      <span>Còn {coupon.remainingQuantity ?? 0}</span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )
      )}
    </div>
  );
}
