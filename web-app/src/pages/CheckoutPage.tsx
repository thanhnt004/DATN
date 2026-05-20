import { useState, useEffect, useRef } from "react";
import { useNavigate, useLocation, Link } from "react-router-dom";
import {
  MapPin,
  CreditCard,
  Store,
  Tag,
  ChevronDown,
  Plus,
  ShoppingBag,
  Loader2,
  X,
  Search,
  ShieldCheck,
} from "lucide-react";
import { useAuth } from "../hooks/useAuth";
import { getCart, getCheckoutPreview } from "../api/cartApi";
import { getMyAddresses, createAddress } from "../api/userApi";
import { createOrder } from "../api/orderApi";
import {
  createCheckoutSession,
  getCheckoutSession,
  updateCheckoutAddress,
  updateCheckoutQuantity,
  updateCheckoutSellerVoucher,
  updateCheckoutPlatformVoucher,
  updateCheckoutBuyerNote,
} from "../api/checkoutApi";
import { getMyCoupons, getAvailableCoupons, claimCoupon } from "../api/couponApi";
import { formatPrice } from "../utils/helpers";
import AddressFormModal from "../components/AddressFormModal";
import type { AddressFormValues } from "../components/AddressFormModal";
import type { CartBySellerResponse } from "../types/cart";
import type { AddressResponse } from "../types/user";
import type { PaymentMethod } from "../types/order";
import type { UserCouponResponse, CouponResponse } from "../types/coupon";
import type { CheckoutSessionResponse, CreateCheckoutSessionRequest } from "../types/checkout";

export default function CheckoutPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, sessionRestored } = useAuth();
  const sessionCreatedRef = useRef(false);
  const sessionCreationPendingRef = useRef(false);
  const sessionLoadedRef = useRef<string | null>(null);

  const [checkoutSession, setCheckoutSession] = useState<CheckoutSessionResponse | null>(null);
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [addresses, setAddresses] = useState<AddressResponse[]>([]);
  const [selectedAddress, setSelectedAddress] = useState<AddressResponse | null>(null);
  const [showAddressModal, setShowAddressModal] = useState(false);
  const [showAddressPicker, setShowAddressPicker] = useState(false);

  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("COD");
  const [sellerNotes, setSellerNotes] = useState<Record<string, string>>({});
  const [sellerVoucherIds, setSellerVoucherIds] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [placing, setPlacing] = useState(false);
  const [checkoutError, setCheckoutError] = useState<string | null>(null);

  const [coupons, setCoupons] = useState<UserCouponResponse[]>([]);
  const [availableCoupons, setAvailableCoupons] = useState<CouponResponse[]>([]);
  const [selectedCoupon, setSelectedCoupon] = useState<UserCouponResponse | null>(null);
  const [activeVoucherModal, setActiveVoucherModal] = useState<{ type: 'PLATFORM' } | { type: 'SHOP', sellerId: string } | null>(null);
  const [couponCode, setCouponCode] = useState("");
  const [applyingCode, setApplyingCode] = useState(false);
  const [couponError, setCouponError] = useState<string | null>(null);

  const [couponsLoaded, setCouponsLoaded] = useState(false);
  const [loadingCoupons, setLoadingCoupons] = useState(false);

  type ShopCouponMeta = UserCouponResponse & { couponType?: string; sellerId?: string };
  const currentSellerId = activeVoucherModal?.type === 'SHOP' ? activeVoucherModal.sellerId : undefined;

  const getCouponMetaById = (couponId: string) => availableCoupons.find((ac) => ac.id === couponId);
  const enrichedCoupons: Array<UserCouponResponse & { couponType?: string; sellerId?: string }> = coupons.map((c) => {
    const meta = getCouponMetaById(c.couponId);
    return {
      ...c,
      couponCode: c.couponCode ?? meta?.code ?? "",
      discountType: c.discountType ?? meta?.discountType ?? "FIXED_AMOUNT",
      discountValue: c.discountValue ?? meta?.discountValue ?? 0,
      minOrderAmount: c.minOrderAmount ?? meta?.minOrderAmount ?? null,
      maxDiscountAmount: c.maxDiscountAmount ?? meta?.maxDiscountAmount ?? null,
      couponEndDate: c.couponEndDate ?? meta?.endDate ?? "",
      couponType: c.couponType ?? meta?.couponType,
      sellerId: c.sellerId ?? meta?.sellerId,
    };
  });

  const claimedCouponIds = new Set(enrichedCoupons.map((c) => c.couponId));
  const claimedCouponCodes = new Set(enrichedCoupons.map((c) => (c.couponCode ?? "").toUpperCase()));

  const displayUserCoupons = activeVoucherModal?.type === "SHOP"
    ? enrichedCoupons.filter((c): c is ShopCouponMeta => {
        return c.couponType === "SHOP" && c.sellerId === currentSellerId;
      })
    : enrichedCoupons.filter((c) => c.couponType === "PLATFORM");

  const displayAvailableCoupons = activeVoucherModal?.type === "SHOP"
    ? availableCoupons.filter((c) => c.couponType === "SHOP" && c.sellerId === currentSellerId)
    : availableCoupons.filter((c) => c.couponType === "PLATFORM");

  const filteredAvailableCoupons = displayAvailableCoupons.filter(
    (ac) => !claimedCouponIds.has(ac.id) && !claimedCouponCodes.has(ac.code.toUpperCase())
  );

  // Check authentication
  useEffect(() => {
    if (!sessionRestored) return;
    if (!isAuthenticated) {
      navigate("/login", { replace: true });
    }
  }, [sessionRestored, isAuthenticated, navigate]);

  // Load or create checkout session
  useEffect(() => {
    if (!sessionRestored || !isAuthenticated) return;
    if (sessionCreatedRef.current || sessionCreationPendingRef.current) return;

    const buyNowCart = (location.state as { buyNowCart?: CartBySellerResponse })?.buyNowCart;
    const searchParams = new URLSearchParams(location.search);
    const urlSessionId = searchParams.get("sessionId");

    // Prevent loading same session multiple times
    if (urlSessionId && sessionLoadedRef.current === urlSessionId) return;

    const loadSession = async () => {
      setLoading(true);
      setCheckoutError(null);
      try {
        const addresses = await getMyAddresses();
        setAddresses(addresses);
        const defaultAddress = addresses.find((a) => a.isDefault) ?? addresses[0] ?? null;
        setSelectedAddress(defaultAddress);

        if (urlSessionId) {
          sessionLoadedRef.current = urlSessionId;
          const res = await getCheckoutSession(urlSessionId);
          setSessionId(urlSessionId);
          setCheckoutSession(res.data.result);
          setSellerNotes(Object.fromEntries(res.data.result.sellerSessions.map((group: { sellerId: string; buyerNote?: string }) => [group.sellerId, group.buyerNote ?? ""])));
          setSellerVoucherIds(Object.fromEntries(res.data.result.sellerSessions.map((group: { sellerId: string; voucherId?: string }) => [group.sellerId, group.voucherId ?? ""])));
          const matchingAddress = addresses.find((addr) =>
            res.data.result && addr.receiverPhone === res.data.result.shippingAddress.recipientPhone &&
            addr.fullAddress === res.data.result.shippingAddress.fullAddress
          );
          setSelectedAddress(matchingAddress ?? defaultAddress ?? (res.data.result ? {
            id: "session-address",
            receiverName: res.data.result.shippingAddress.recipientName,
            receiverPhone: res.data.result.shippingAddress.recipientPhone,
            province: res.data.result.shippingAddress.city ?? "",
            district: res.data.result.shippingAddress.district ?? "",
            ward: res.data.result.shippingAddress.ward ?? "",
            addressLine: res.data.result.shippingAddress.address,
            fullAddress: res.data.result.shippingAddress.fullAddress ?? res.data.result.shippingAddress.address,
            isDefault: false,
            createdAt: "",
            updatedAt: "",
          } : null));
          setLoading(false);
          return;
        }

        const previewRes = buyNowCart ? { data: { result: [buyNowCart] } } : await getCheckoutPreview();
        const previewData = previewRes.data.result ?? [];
        if (!Array.isArray(previewData) || previewData.length === 0) {
          setCheckoutError("Không có sản phẩm để thanh toán");
          return;
        }

        if (!defaultAddress) {
          setCheckoutError("Vui lòng thêm địa chỉ giao hàng trước khi tiếp tục");
          return;
        }

        let cartId: string | undefined;
        if (!buyNowCart) {
          try {
            const cartRes = await getCart();
            cartId = cartRes.data.result?.id;
          } catch (error) {
            console.warn("Không lấy được cartId khi tạo checkout session", error);
          }
        }

        const request: CreateCheckoutSessionRequest = {
          items: previewData.flatMap((group) => (group.items ?? []).map((item) => ({ skuId: item.skuId, quantity: item.quantity }))),
          recipientName: defaultAddress.receiverName,
          recipientPhone: defaultAddress.receiverPhone,
          shippingAddress: defaultAddress.addressLine,
          shippingWard: defaultAddress.ward,
          shippingDistrict: defaultAddress.district,
          shippingCity: defaultAddress.province,
          cartId,
        };

        sessionCreationPendingRef.current = true;
        const res = await createCheckoutSession(request);
        sessionCreatedRef.current = true;
        setSessionId(res.data.result.sessionId);
        setCheckoutSession(res.data.result);
        setSellerNotes(Object.fromEntries(res.data.result.sellerSessions.map((group: { sellerId: string; buyerNote?: string }) => [group.sellerId, group.buyerNote ?? ""])));
        setSellerVoucherIds(Object.fromEntries(res.data.result.sellerSessions.map((group: { sellerId: string; voucherId?: string }) => [group.sellerId, group.voucherId ?? ""])));
        navigate(`/checkout?sessionId=${res.data.result.sessionId}`, { replace: true, state: location.state });
      } catch (error) {
        console.error(error);
        setCheckoutError("Không thể tải thông tin thanh toán. Vui lòng thử lại sau.");
      } finally {
        setLoading(false);
        if (sessionCreationPendingRef.current) {
          sessionCreationPendingRef.current = false;
        }
      }
    };

    loadSession();
  }, [sessionRestored, isAuthenticated, navigate, location.state, location.search]);

  const loadCouponsLazy = async (type: 'PLATFORM' | 'SHOP', sellerId?: string) => {
    setActiveVoucherModal(type === 'PLATFORM' ? { type: 'PLATFORM' } : { type: 'SHOP', sellerId: sellerId! });
    if (couponsLoaded || loadingCoupons) return;
    setLoadingCoupons(true);
    try {
      const [couponRes, availRes] = await Promise.all([
        getMyCoupons({ status: "AVAILABLE" }).catch(() => ({ data: { result: [] } })),
        getAvailableCoupons({ page: 0, size: 50 }).catch(() => ({ data: { result: { content: [] } } })),
      ]);
      setCoupons(Array.isArray(couponRes.data.result) ? couponRes.data.result : []);
      const avail = availRes.data.result;
      setAvailableCoupons(Array.isArray(avail) ? avail : (avail as { content?: CouponResponse[] })?.content ?? []);
      setCouponsLoaded(true);
    } catch (e) {
      console.error(e);
    } finally {
      setLoadingCoupons(false);
    }
  };

  useEffect(() => {
    if (!checkoutSession?.voucherId) {
      setSelectedCoupon(null);
      return;
    }

    const found = coupons.find((c) => c.couponId === checkoutSession.voucherId);
    if (found) {
      setSelectedCoupon(found);
    }
  }, [checkoutSession?.voucherId, coupons]);

  const subtotal = checkoutSession?.sellerSessions.reduce((sum, group) => sum + group.totalAmount, 0) ?? 0;
  const shippingFee = checkoutSession?.sellerSessions.reduce((sum, group) => sum + group.shippingFee, 0) ?? 0;
  const shopVouchersDiscount = checkoutSession?.sellerSessions.reduce((sum, group) => sum + (group.discount ?? 0), 0) ?? 0;
  const platformVoucherDiscount = checkoutSession?.discount ?? 0;
  const discountAmount = shopVouchersDiscount + platformVoucherDiscount;
  const total = checkoutSession?.finalAmount ?? 0;
  const itemsBySeller = checkoutSession?.items.reduce<Record<string, typeof checkoutSession.items>>((acc, item) => {
    const group = acc[item.sellerId] ?? [];
    group.push(item);
    acc[item.sellerId] = group;
    return acc;
  }, {} as Record<string, typeof checkoutSession.items>) ?? {};

  const applyCheckoutSession = (session: CheckoutSessionResponse) => {
    setCheckoutSession(session);
    setSellerNotes(Object.fromEntries(session.sellerSessions.map((group) => [group.sellerId, group.buyerNote ?? ""])));
    setSellerVoucherIds(Object.fromEntries(session.sellerSessions.map((group) => [group.sellerId, group.voucherId ?? ""])));
  };

  const handleApplyCouponFromModal = async (coupon: UserCouponResponse) => {
    if (!sessionId) {
      setCouponError("Phiên thanh toán chưa sẵn sàng");
      return;
    }

    try {
      setCouponError(null);
      if (activeVoucherModal?.type === 'PLATFORM') {
        const res = await updateCheckoutPlatformVoucher(sessionId, { voucherId: coupon.couponId });
        applyCheckoutSession(res.data.result);
        setSelectedCoupon(coupon);
        setActiveVoucherModal(null);
      } else if (activeVoucherModal?.type === 'SHOP') {
        const sellerId = activeVoucherModal.sellerId;
        const res = await updateCheckoutSellerVoucher(sessionId, sellerId, { voucherId: coupon.couponId });
        applyCheckoutSession(res.data.result);
        setActiveVoucherModal(null);
      }
    } catch {
      setCouponError("Không thể áp dụng voucher này");
    }
  };

  const handleApplyByCode = async () => {
    const code = couponCode.trim().toUpperCase();
    if (!code) return;
    if (!sessionId) {
      setCouponError("Phiên thanh toán chưa sẵn sàng");
      return;
    }

    setCouponError(null);
    setApplyingCode(true);
    try {
      const existing = displayUserCoupons.find((c) => (c.couponCode ?? "").toUpperCase() === code);
      if (existing) {
        await handleApplyCouponFromModal(existing);
        return;
      }
      const found = displayAvailableCoupons.find((c) => c.code.toUpperCase() === code);
      if (!found) {
        setCouponError("Mã giảm giá không tồn tại hoặc không hợp lệ");
        return;
      }
      const claimRes = await claimCoupon(found.id);
      const claimed = claimRes.data.result;
      setCoupons((prev) => [...prev, claimed]);
      await handleApplyCouponFromModal(claimed);
    } catch {
      setCouponError("Không thể áp dụng mã giảm giá này");
    } finally {
      setApplyingCode(false);
    }
  };

  const handleSaveAddress = async (values: AddressFormValues) => {
    const newAddr = await createAddress({
      receiverName: values.receiverName,
      receiverPhone: values.receiverPhone,
      province: values.province,
      district: values.district,
      ward: values.ward,
      addressLine: values.addressLine,
      isDefault: values.isDefault,
    });
    setAddresses((prev) => [...prev, newAddr]);
    setSelectedAddress(newAddr);
    setShowAddressModal(false);

    if (sessionId) {
      try {
        const res = await updateCheckoutAddress(sessionId, {
          recipientName: newAddr.receiverName,
          recipientPhone: newAddr.receiverPhone,
          address: newAddr.addressLine,
          ward: newAddr.ward,
          district: newAddr.district,
          city: newAddr.province,
        });
        applyCheckoutSession(res.data.result);
      } catch (error) {
        console.error("Không thể cập nhật địa chỉ giao hàng", error);
      }
    }
  };

  const handleSelectAddress = async (address: AddressResponse) => {
    setSelectedAddress(address);
    setShowAddressPicker(false);

    if (!sessionId) return;
    try {
      const res = await updateCheckoutAddress(sessionId, {
        recipientName: address.receiverName,
        recipientPhone: address.receiverPhone,
        address: address.addressLine,
        ward: address.ward,
        district: address.district,
        city: address.province,
      });
      applyCheckoutSession(res.data.result);
    } catch (error) {
      console.error("Không thể cập nhật địa chỉ giao hàng", error);
    }
  };

  const handleUpdateQuantity = async (skuId: string, quantity: number) => {
    if (!sessionId || quantity < 1) return;
    try {
      const res = await updateCheckoutQuantity(sessionId, { skuId, quantity });
      applyCheckoutSession(res.data.result);
    } catch (error) {
      console.error("Không thể cập nhật số lượng sản phẩm", error);
    }
  };

  const handleUpdateBuyerNote = async (sellerId: string) => {
    if (!sessionId) return;
    try {
      const res = await updateCheckoutBuyerNote(sessionId, sellerId, {
        buyerNote: sellerNotes[sellerId] || "",
      });
      applyCheckoutSession(res.data.result);
    } catch (error) {
      console.error("Không thể cập nhật ghi chú", error);
    }
  };

  const handleRemoveCoupon = async () => {
    if (!sessionId) return;
    setCouponCode("");
    setCouponError(null);
    try {
      if (activeVoucherModal?.type === 'PLATFORM') {
        const res = await updateCheckoutPlatformVoucher(sessionId, { voucherId: undefined });
        applyCheckoutSession(res.data.result);
        setSelectedCoupon(null);
        setActiveVoucherModal(null);
      } else if (activeVoucherModal?.type === 'SHOP') {
        const sellerId = activeVoucherModal.sellerId;
        const res = await updateCheckoutSellerVoucher(sessionId, sellerId, { voucherId: undefined });
        applyCheckoutSession(res.data.result);
        setActiveVoucherModal(null);
      }
    } catch (error) {
      console.error("Không thể xóa voucher", error);
    }
  };

  const handlePlaceOrder = async () => {
    if (!selectedAddress) { alert("Vui lòng chọn địa chỉ giao hàng"); return; }
    if (!checkoutSession?.sessionId) return;

    setPlacing(true);
    try {
      const res = await createOrder({
        checkoutSessionId: checkoutSession.sessionId,
        paymentMethod,
      });

      const orders = res.data.result;
      if (!Array.isArray(orders) || orders.length === 0) {
        throw new Error("Không có đơn hàng được tạo");
      }

      const order = orders[0];
      if (paymentMethod === "VNPAY") {
        if (order.paymentUrl) {
          window.location.href = order.paymentUrl;
          return;
        } else {
          throw new Error("Không thể tạo thanh toán VNPAY");
        }
      }

      navigate("/payment/result?status=success&orderId=" + order.id);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number; data?: { message?: string; result?: unknown } } };
      let message = axiosErr.response?.data?.message || "Đặt hàng thất bại";
      const result = axiosErr.response?.data?.result;
      if (result && typeof result === "object") {
        const fieldErrors = Object.entries(result as Record<string, string>).map(([f, m]) => `• ${f}: ${m}`).join("\n");
        if (fieldErrors) message += "\n\n" + fieldErrors;
      }
      alert(`Lỗi đặt hàng [${axiosErr.response?.status ?? "NETWORK"}]:\n${message}`);
    } finally { setPlacing(false); }
  };

  /* ── Loading ── */
  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 py-8">
        <div className="max-w-4xl mx-auto px-4 space-y-3">
          {[1, 2, 3, 4].map((i) => <div key={i} className="bg-white rounded-lg h-28 animate-pulse" />)}
        </div>
      </div>
    );
  }

  if (!loading && (!checkoutSession || checkoutError)) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center px-4">
          <ShoppingBag className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h2 className="text-xl font-bold text-gray-800 mb-2">Không thể thanh toán</h2>
          <p className="text-gray-500 mb-6">{checkoutError ?? "Giỏ hàng trống hoặc chưa có phiên thanh toán."}</p>
          <Link to="/cart" className="inline-flex items-center gap-2 bg-red-500 text-white px-6 py-2.5 rounded-lg font-semibold hover:bg-red-600 transition">
            Quay lại giỏ hàng
          </Link>
        </div>
      </div>
    );
  }

  /* ─────────────────────────── RENDER ─────────────────────────── */
  return (
    <>
      <div className="min-h-screen bg-gray-100 pb-20 lg:pb-8">
        <div className="max-w-4xl mx-auto px-2 sm:px-4 py-4 sm:py-6">
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 mb-4 px-2 sm:px-0">Thanh toán</h1>

          {/* ═══════════ 1. ADDRESS ═══════════ */}
          <div className="bg-white rounded-lg shadow-sm mb-3 overflow-hidden">
            <div className="border-t-2 border-red-500" />
            <div className="px-4 py-4">
              <div className="flex items-center gap-2 mb-2">
                <MapPin className="w-4 h-4 text-red-500" />
                <h2 className="text-sm font-bold text-red-500">Địa Chỉ Nhận Hàng</h2>
              </div>
              {selectedAddress ? (
                <div className="flex items-start justify-between gap-3">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-semibold text-gray-900 text-sm">{selectedAddress.receiverName}</span>
                      <span className="text-gray-400">|</span>
                      <span className="text-sm text-gray-600">{selectedAddress.receiverPhone}</span>
                      {selectedAddress.isDefault && (
                        <span className="text-[10px] font-bold border border-red-500 text-red-500 px-1.5 py-0.5 rounded">Mặc định</span>
                      )}
                    </div>
                    <p className="text-sm text-gray-500 mt-1 leading-relaxed">{selectedAddress.fullAddress}</p>
                  </div>
                  <button
                    onClick={() => setShowAddressPicker(true)}
                    className="text-sm text-blue-500 hover:text-blue-600 font-medium shrink-0"
                  >
                    Đổi
                  </button>
                </div>
              ) : (
                <div className="text-center py-4">
                  <p className="text-gray-400 text-sm mb-2">Bạn chưa có địa chỉ nào</p>
                  <button
                    onClick={() => setShowAddressModal(true)}
                    className="inline-flex items-center gap-1.5 bg-red-500 text-white px-4 py-2 rounded-lg text-sm font-semibold hover:bg-red-600"
                  >
                    <Plus className="w-4 h-4" /> Thêm địa chỉ
                  </button>
                </div>
              )}
            </div>
          </div>

          {/* ═══════════ 2. PRODUCTS BY SELLER ═══════════ */}
          {checkoutSession?.sellerSessions.map((group) => {
            const items = itemsBySeller[group.sellerId] ?? [];
            const groupTotal = group.totalAmount;
            const groupQty = items.reduce((s, i) => s + i.quantity, 0);
            return (
              <div key={group.sellerId} className="bg-white rounded-lg shadow-sm mb-3">
                {/* seller header */}
                <div className="flex items-center gap-2 px-4 py-3 border-b border-gray-100">
                  <Store className="w-4 h-4 text-red-500" />
                  <span className="font-semibold text-sm text-gray-900 truncate">
                    Shop #{group.sellerId.slice(0, 8)}
                  </span>
                </div>

                {/* items */}
                <div className="divide-y divide-gray-50">
                  {items.map((item) => {
                    const skuName = item.variantInfo ? Object.values(item.variantInfo).join(", ") : item.skuCode;
                    return (
                      <div key={item.skuId} className="flex flex-col gap-3 px-4 py-3">
                        <div className="flex gap-3">
                          <img
                            src={item.imageUrl || "https://placehold.co/80x80?text=No+Image"}
                            alt={item.productName}
                            className="w-16 h-16 sm:w-20 sm:h-20 object-cover rounded-lg border border-gray-100 shrink-0"
                          />
                          <div className="flex-1 min-w-0 flex flex-col justify-between">
                            <div>
                              <p className="text-sm font-medium text-gray-900 line-clamp-2 leading-snug">{item.productName}</p>
                              {skuName && (
                                <p className="text-xs text-gray-400 mt-0.5">Phân loại: {skuName}</p>
                              )}
                            </div>
                            <div className="flex items-center justify-between mt-1 gap-3">
                              <span className="text-sm font-semibold text-red-600">{formatPrice(item.unitPrice)}</span>
                              <div className="flex items-center gap-2 text-sm">
                                <button
                                  onClick={() => handleUpdateQuantity(item.skuId, Math.max(1, item.quantity - 1))}
                                  className="w-8 h-8 rounded-lg border border-gray-200 bg-white text-gray-600"
                                >
                                  -
                                </button>
                                <span className="w-8 text-center">{item.quantity}</span>
                                <button
                                  onClick={() => handleUpdateQuantity(item.skuId, item.quantity + 1)}
                                  className="w-8 h-8 rounded-lg border border-gray-200 bg-white text-gray-600"
                                >
                                  +
                                </button>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>

                {/* seller voucher + note */}
                <div className="px-4 py-3 border-t border-gray-100 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-medium text-gray-900">Voucher của Shop</span>
                    <button
                      onClick={() => loadCouponsLazy('SHOP', group.sellerId)}
                      className="flex items-center gap-2"
                    >
                      {sellerVoucherIds[group.sellerId] && group.discount > 0 ? (
                        <div className="flex items-center gap-2">
                          {coupons.find((c) => c.couponId === sellerVoucherIds[group.sellerId])?.couponCode ? (
                            <span className="text-sm font-medium text-gray-700">
                              {coupons.find((c) => c.couponId === sellerVoucherIds[group.sellerId])?.couponCode}
                            </span>
                          ) : null}
                          <span className="text-sm text-red-500 font-semibold">-{formatPrice(group.discount)}</span>
                          <ChevronDown className="w-4 h-4 text-gray-400" />
                        </div>
                      ) : (
                        <div className="flex items-center gap-1">
                          <span className="text-sm text-gray-400">Chọn voucher shop</span>
                          <ChevronDown className="w-4 h-4 text-gray-400" />
                        </div>
                      )}
                    </button>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-gray-500 shrink-0 whitespace-nowrap">Lời nhắn:</span>
                    <input
                      type="text"
                      value={sellerNotes[group.sellerId] || ""}
                      onChange={(e) => setSellerNotes((prev) => ({ ...prev, [group.sellerId]: e.target.value }))}
                      onBlur={() => handleUpdateBuyerNote(group.sellerId)}
                      placeholder="Lưu ý cho người bán..."
                      className="flex-1 text-sm border-0 bg-transparent outline-none text-gray-700 placeholder:text-gray-300"
                    />
                  </div>
                </div>

                {/* group total */}
                <div className="px-4 py-3 border-t border-gray-50 flex items-center justify-end gap-2 bg-red-50/40">
                  <span className="text-xs text-gray-500">Tổng số tiền ({groupQty} sản phẩm):</span>
                  <span className="text-base font-bold text-red-600">{formatPrice(groupTotal)}</span>
                </div>
              </div>
            );
          })}

          {/* ═══════════ 3. VOUCHER ═══════════ */}
          <div className="bg-white rounded-lg shadow-sm mb-3">
            <button
              onClick={() => loadCouponsLazy('PLATFORM')}
              className="flex items-center justify-between w-full px-4 py-3.5"
            >
              <div className="flex items-center gap-2">
                <Tag className="w-4 h-4 text-red-500" />
                <span className="font-medium text-sm text-gray-900">Sellico Voucher</span>
              </div>
              {(selectedCoupon || checkoutSession?.voucherId) ? (
                <div className="flex items-center gap-2">
                  {selectedCoupon?.couponCode ? (
                    <span className="text-sm font-medium text-gray-700">{selectedCoupon.couponCode}</span>
                  ) : checkoutSession?.voucherId ? (
                    <span className="text-sm font-medium text-gray-700">Voucher đã chọn</span>
                  ) : null}
                  <span className="text-sm text-red-500 font-semibold">-{formatPrice(discountAmount)}</span>
                  <ChevronDown className="w-4 h-4 text-gray-400" />
                </div>
              ) : (
                <div className="flex items-center gap-1">
                  <span className="text-xs text-gray-400">Chọn hoặc nhập mã</span>
                  <ChevronDown className="w-4 h-4 text-gray-400" />
                </div>
              )}
            </button>
          </div>

          {/* ═══════════ 4. PAYMENT METHOD ═══════════ */}
          <div className="bg-white rounded-lg shadow-sm mb-3 px-4 py-4">
            <div className="flex items-center gap-2 mb-3">
              <CreditCard className="w-4 h-4 text-red-500" />
              <h2 className="font-bold text-sm text-gray-900">Phương Thức Thanh Toán</h2>
            </div>
            <div className="flex gap-3">
              {([
                { value: "COD" as PaymentMethod, label: "Thanh toán khi nhận hàng", icon: "💵" },
                { value: "VNPAY" as PaymentMethod, label: "VNPay", icon: "💳" },
              ]).map((m) => (
                <button
                  key={m.value}
                  onClick={() => setPaymentMethod(m.value)}
                  className={`flex-1 flex items-center gap-2.5 p-3 rounded-lg border-2 transition text-left ${
                    paymentMethod === m.value ? "border-red-500 bg-red-50" : "border-gray-200 hover:border-gray-300"
                  }`}
                >
                  <span className="text-xl">{m.icon}</span>
                  <span className={`text-sm font-medium ${paymentMethod === m.value ? "text-red-600" : "text-gray-700"}`}>
                    {m.label}
                  </span>
                </button>
              ))}
            </div>
          </div>

          {/* ═══════════ 5. PAYMENT DETAIL ═══════════ */}
          <div className="bg-white rounded-lg shadow-sm mb-3 px-4 py-4">
            <div className="flex items-center gap-2 mb-3">
              <ShieldCheck className="w-4 h-4 text-red-500" />
              <h2 className="font-bold text-sm text-gray-900">Chi Tiết Thanh Toán</h2>
            </div>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-500">Tổng tiền hàng</span>
                <span className="text-gray-800">{formatPrice(subtotal)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Phí vận chuyển</span>
                <span className="text-teal-600 font-medium">{shippingFee === 0 ? "Miễn phí" : formatPrice(shippingFee)}</span>
              </div>
              {shopVouchersDiscount > 0 && (
                <div className="flex justify-between text-green-600">
                  <span>Giảm giá voucher shop</span>
                  <span className="font-medium">-{formatPrice(shopVouchersDiscount)}</span>
                </div>
              )}
              {platformVoucherDiscount > 0 && (
                <div className="flex justify-between text-green-600">
                  <span>Giảm giá voucher Sellico</span>
                  <span className="font-medium">-{formatPrice(platformVoucherDiscount)}</span>
                </div>
              )}
              <div className="border-t border-gray-100 pt-2.5 flex justify-between items-center">
                <span className="font-bold text-gray-900">Tổng thanh toán</span>
                <span className="font-bold text-xl text-red-600">{formatPrice(total)}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* ═══════════ FLOATING BOTTOM BAR ═══════════ */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t shadow-lg z-40">
        <div className="max-w-4xl mx-auto px-4 py-3 flex items-center justify-between">
          <div className="min-w-0">
            <p className="text-xs text-gray-500">Tổng cộng</p>
            <p className="text-lg sm:text-xl font-bold text-red-600">{formatPrice(total)}</p>
          </div>
          <button
            onClick={handlePlaceOrder}
            disabled={placing || !selectedAddress}
            className="bg-red-500 text-white px-8 sm:px-12 py-3 rounded-lg font-bold text-sm sm:text-base hover:bg-red-600 transition disabled:bg-gray-300 disabled:cursor-not-allowed flex items-center gap-2 shrink-0"
          >
            {placing && <Loader2 className="w-4 h-4 animate-spin" />}
            {placing ? "Đang xử lý..." : "Đặt hàng"}
          </button>
        </div>
      </div>

      {/* ═══════════ ADDRESS PICKER MODAL ═══════════ */}
      {showAddressPicker && (
        <div className="fixed inset-0 z-50 flex items-center justify-center" onClick={() => setShowAddressPicker(false)}>
          <div className="absolute inset-0 bg-black/40" />
          <div className="relative bg-white rounded-2xl w-full max-w-md mx-4 max-h-[80vh] flex flex-col shadow-xl" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between px-5 py-4 border-b shrink-0">
              <h3 className="font-bold text-gray-900">Chọn Địa Chỉ</h3>
              <button onClick={() => setShowAddressPicker(false)} className="p-1 hover:bg-gray-100 rounded-full">
                <X className="w-5 h-5 text-gray-500" />
              </button>
            </div>
            <div className="overflow-y-auto flex-1 px-5 py-3 space-y-2">
              {addresses.map((addr) => (
                <label
                  key={addr.id}
                  className={`flex items-start gap-3 p-3 rounded-xl cursor-pointer transition border-2 ${
                    selectedAddress?.id === addr.id ? "border-red-500 bg-red-50/50" : "border-transparent bg-gray-50 hover:bg-gray-100"
                  }`}
                >
                  <input
                    type="radio"
                    name="addr-pick"
                    checked={selectedAddress?.id === addr.id}
                    onChange={() => handleSelectAddress(addr)}
                    className="mt-0.5 accent-red-500 shrink-0"
                  />
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-gray-900">
                      {addr.receiverName}
                      <span className="text-gray-500 font-normal ml-2">{addr.receiverPhone}</span>
                    </p>
                    <p className="text-xs text-gray-500 mt-0.5 leading-relaxed">{addr.fullAddress}</p>
                    {addr.isDefault && (
                      <span className="inline-block mt-1 text-[10px] font-bold border border-red-500 text-red-500 px-1.5 py-0.5 rounded">Mặc định</span>
                    )}
                  </div>
                </label>
              ))}
            </div>
            <div className="px-5 py-3 border-t shrink-0">
              <button
                onClick={() => { setShowAddressPicker(false); setShowAddressModal(true); }}
                className="w-full flex items-center justify-center gap-1.5 py-2.5 border-2 border-dashed border-gray-300 rounded-xl text-sm text-gray-500 font-medium hover:border-red-400 hover:text-red-500 transition"
              >
                <Plus className="w-4 h-4" /> Thêm Địa Chỉ Mới
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ═══════════ VOUCHER MODAL ═══════════ */}
      {activeVoucherModal && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center" onClick={() => setActiveVoucherModal(null)}>
          <div className="absolute inset-0 bg-black/40" />
          <div className="relative bg-white sm:rounded-2xl rounded-t-2xl w-full sm:max-w-md sm:mx-4 max-h-[85vh] flex flex-col shadow-xl" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between px-5 py-4 border-b shrink-0">
              <h3 className="font-bold text-gray-900">
                {activeVoucherModal.type === 'PLATFORM' ? 'Chọn Voucher Sàn' : 'Chọn Voucher Shop'}
              </h3>
              <button onClick={() => setActiveVoucherModal(null)} className="p-1 hover:bg-gray-100 rounded-full">
                <X className="w-5 h-5 text-gray-500" />
              </button>
            </div>
            <div className="px-5 pt-4 pb-2 shrink-0">
              <div className="flex gap-2">
                <div className="flex-1 flex items-center gap-2 bg-gray-100 rounded-lg px-3 py-2">
                  <Search className="w-4 h-4 text-gray-400 shrink-0" />
                  <input
                    type="text"
                    value={couponCode}
                    onChange={(e) => { setCouponCode(e.target.value.toUpperCase()); setCouponError(null); }}
                    onKeyDown={(e) => e.key === "Enter" && handleApplyByCode()}
                    placeholder="Nhập mã voucher"
                    className="flex-1 bg-transparent text-sm font-mono uppercase placeholder:normal-case placeholder:font-sans outline-none"
                  />
                </div>
                <button
                  onClick={handleApplyByCode}
                  disabled={applyingCode || !couponCode.trim()}
                  className="px-4 py-2 bg-red-500 text-white text-sm font-semibold rounded-lg hover:bg-red-600 disabled:bg-gray-300 disabled:cursor-not-allowed shrink-0"
                >
                  {applyingCode ? "..." : "Áp dụng"}
                </button>
              </div>
              {couponError && <p className="text-xs text-red-500 mt-1.5">{couponError}</p>}
            </div>
            <div className="overflow-y-auto flex-1 px-5 py-3 space-y-2">
              {loadingCoupons ? (
                <div className="flex items-center justify-center py-10">
                  <Loader2 className="w-6 h-6 animate-spin text-red-500" />
                </div>
              ) : (
                <>
                  {displayUserCoupons.length === 0 && displayAvailableCoupons.length === 0 && (
                    <p className="text-sm text-gray-400 text-center py-8">Không có voucher khả dụng</p>
                  )}
                  {displayUserCoupons.map((c) => {
                    const isSelected = activeVoucherModal.type === 'PLATFORM'
                      ? selectedCoupon?.id === c.id || checkoutSession?.voucherId === c.couponId
                      : sellerVoucherIds[currentSellerId!] === c.couponId;

                    return (
                      <button
                        key={c.id}
                        onClick={() => handleApplyCouponFromModal(c)}
                        className={`w-full text-left p-3.5 rounded-xl border-2 transition ${
                          isSelected ? "border-red-500 bg-red-50" : "border-gray-100 bg-gray-50 hover:border-gray-200"
                        }`}
                      >
                        <div className="flex justify-between items-center">
                          <span className="font-mono font-bold text-sm text-red-600">{c.couponCode}</span>
                          <span className="text-[11px] text-gray-400">HSD: {new Date(c.couponEndDate).toLocaleDateString("vi-VN")}</span>
                        </div>
                        <p className="text-xs text-gray-500 mt-1">
                          Giảm {c.discountType === "PERCENTAGE" ? c.discountValue + "%" : formatPrice(c.discountValue)}
                          {c.minOrderAmount ? " · Đơn tối thiểu " + formatPrice(c.minOrderAmount) : ""}
                        </p>
                      </button>
                    );
                  })}
              {filteredAvailableCoupons.map((ac) => (
                <button
                  key={ac.id}
                  onClick={async () => {
                    try {
                      setCouponError(null);
                      const claimRes = await claimCoupon(ac.id);
                      const claimed = claimRes.data.result;
                      setCoupons((prev) => [...prev, claimed]);
                      await handleApplyCouponFromModal(claimed);
                    } catch { setCouponError("Không thể lấy voucher này"); }
                  }}
                  className="w-full text-left p-3.5 rounded-xl border-2 border-gray-100 bg-gray-50 hover:border-gray-200 transition"
                >
                  <div className="flex justify-between items-center">
                    <div className="flex items-center gap-2">
                      <span className="font-mono font-bold text-sm text-orange-600">{ac.code}</span>
                      <span className="text-[10px] bg-orange-100 text-orange-600 rounded px-1.5 py-0.5 font-medium">Nhận & dùng</span>
                    </div>
                    <span className="text-[11px] text-gray-400">HSD: {new Date(ac.endDate).toLocaleDateString("vi-VN")}</span>
                  </div>
                  <p className="text-xs text-gray-500 mt-1">
                    Giảm {ac.discountType === "PERCENTAGE" ? ac.discountValue + "%" : formatPrice(ac.discountValue)}
                    {ac.minOrderAmount ? " · Đơn tối thiểu " + formatPrice(ac.minOrderAmount) : ""}
                  </p>
                </button>
              ))}
              </>
              )}
              {((activeVoucherModal.type === 'PLATFORM' && (selectedCoupon || checkoutSession?.voucherId)) || 
                (activeVoucherModal.type === 'SHOP' && sellerVoucherIds[currentSellerId!])) && (
                <button
                  onClick={handleRemoveCoupon}
                  className="w-full text-center text-xs text-gray-400 hover:text-red-500 py-2"
                >
                  Bỏ chọn voucher
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ═══════════ ADDRESS FORM MODAL ═══════════ */}
      <AddressFormModal
        open={showAddressModal}
        onClose={() => setShowAddressModal(false)}
        onSave={handleSaveAddress}
      />
    </>
  );
}
