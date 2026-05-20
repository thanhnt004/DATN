import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Loader2, RefreshCw, ChevronLeft, ChevronRight,
  ClipboardCheck, Truck, XCircle, X, Eye, Package, Printer,
} from "lucide-react";
import {
  getSellerOrders,
  confirmOrder,
  shipOrder,
  deliverOrder,
  completeOrder,
  cancelOrder,
  getSellerOrderById,
  batchShipOrders,
  printShippingLabel as apiPrintShippingLabel,
} from "../../api/sellerDashboardApi";
import type { BatchResult } from "../../api/sellerDashboardApi";
import type { OrderResponse, OrderStatus, ShipOrderRequest } from "../../types/order";

// ─── helpers ─────────────────────────────────────────────────────────────────

const STATUS_CFG: Record<OrderStatus, { label: string; color: string }> = {
  PENDING:          { label: "Chờ xác nhận",    color: "bg-yellow-100 text-yellow-700" },
  CONFIRMED:        { label: "Đã xác nhận",     color: "bg-blue-100 text-blue-700"    },
  SHIPPED:          { label: "Đang giao",       color: "bg-blue-100 text-blue-700" },
  DELIVERED:        { label: "Đã giao",         color: "bg-green-100 text-green-700"   },
  COMPLETED:        { label: "Hoàn thành",      color: "bg-teal-100 text-teal-700"     },
  CANCELLED:        { label: "Đã hủy",          color: "bg-red-100 text-red-600"       },
};

function fmtPrice(n: number | undefined) {
  if (n === undefined) return "—";
  return n.toLocaleString("vi-VN") + "₫";
}

function fmtDate(s: string | undefined) {
  if (!s) return "—";
  return new Date(s).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });
}

function printInvoice(order: OrderResponse) {
  const items = order.items ?? [];
  const paymentLabel: Record<string, string> = { COD: "Thanh toán khi nhận hàng", VNPAY: "Chuyển khoản / VNPay" };

  const html = `<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><title>Hóa đơn #${order.orderNumber}</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:'Segoe UI',Arial,sans-serif;font-size:13px;color:#111;padding:24px;max-width:800px;margin:0 auto}
h1{font-size:22px;text-align:center;margin-bottom:2px}
.sub{text-align:center;font-size:12px;color:#666;margin-bottom:20px}
.section{margin-bottom:16px}
.section-title{font-weight:700;font-size:12px;text-transform:uppercase;letter-spacing:1px;color:#888;border-bottom:1px solid #e5e5e5;padding-bottom:4px;margin-bottom:8px}
.row{display:flex;justify-content:space-between;padding:2px 0;font-size:13px}
.row.bold{font-weight:700}
table{width:100%;border-collapse:collapse;margin-top:4px}
th{text-align:left;font-size:11px;text-transform:uppercase;letter-spacing:0.5px;color:#888;border-bottom:2px solid #e5e5e5;padding:6px 4px}
td{padding:8px 4px;border-bottom:1px solid #f0f0f0;font-size:13px;vertical-align:top}
td.r{text-align:right;white-space:nowrap}
.total-row td{font-weight:700;font-size:15px;border-top:2px solid #111;border-bottom:none}
.footer{margin-top:32px;text-align:center;font-size:11px;color:#999}
@media print{body{padding:16px}button{display:none!important}}
</style></head><body>
<h1>HÓA ĐƠN BÁN HÀNG</h1>
<p class="sub">Mã đơn: <strong>${order.orderNumber}</strong> · Ngày: ${fmtDate(order.createdAt)}</p>

<div class="section">
  <div class="section-title">Người nhận</div>
  <div class="row"><span>${order.recipientName}</span><span>${order.recipientPhone}</span></div>
  <div class="row"><span>${order.shippingAddress}</span></div>
</div>

<div class="section">
  <div class="section-title">Sản phẩm</div>
  <table>
    <thead><tr><th style="width:50%">Sản phẩm</th><th>ĐVT</th><th class="r">Đơn giá</th><th class="r">SL</th><th class="r">Thành tiền</th></tr></thead>
    <tbody>
      ${items.map((item) => `<tr>
        <td>${item.productName}${item.variantInfo ? '<br><span style="font-size:11px;color:#888">' + Object.entries(item.variantInfo).map(([k,v]) => k+': '+v).join(', ') + '</span>' : ''}</td>
        <td>Cái</td>
        <td class="r">${fmtPrice(item.unitPrice)}</td>
        <td class="r">${item.quantity}</td>
        <td class="r">${fmtPrice(item.unitPrice * item.quantity)}</td>
      </tr>`).join('')}
    </tbody>
  </table>
</div>

<div class="section">
  <div class="section-title">Thanh toán</div>
  <div class="row"><span>Tạm tính</span><span>${fmtPrice(order.subtotal)}</span></div>
  ${(order.discountAmount ?? 0) > 0 ? `<div class="row"><span>Voucher Shop A</span><span style="color:green">-${fmtPrice(order.discountAmount)}</span></div>` : ''}
  ${(order.platformVoucherShare ?? 0) > 0 ? `<div class="row"><span>Voucher Sellico (chia)</span><span style="color:green">-${fmtPrice(order.platformVoucherShare)}</span></div>` : ''}
  ${(order.shippingFee ?? 0) > 0 ? `<div class="row"><span>Phí vận chuyển</span><span>${fmtPrice(order.shippingFee)}</span></div>` : ''}
  <div class="row bold" style="font-size:16px;margin-top:6px;padding-top:6px;border-top:2px solid #111"><span>Thanh toán</span><span>${fmtPrice(order.totalAmount)}</span></div>
  <div class="row" style="margin-top:4px"><span>Hình thức</span><span>${paymentLabel[order.paymentMethod] ?? order.paymentMethod}</span></div>
</div>

${order.buyerNote ? `<div class="section"><div class="section-title">Ghi chú</div><p>${order.buyerNote}</p></div>` : ''}

<div class="footer">Cảm ơn quý khách đã mua hàng!</div>
<script>window.onload=()=>window.print()</script>
</body></html>`;

  const w = window.open('', '_blank');
  if (w) { w.document.write(html); w.document.close(); }
}

// ─── Ship modal ───────────────────────────────────────────────────────────────

function ShipModal({ orderId, onClose, onDone }: { orderId: string; onClose: () => void; onDone: () => void }) {
  const [provider, setProvider] = useState("");
  const [tracking, setTracking] = useState("");
  const [note,     setNote]     = useState("");
  const [saving,   setSaving]   = useState(false);
  const [error,    setError]    = useState<string | null>(null);

  const cls = "w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400";

  const handleSave = async () => {
    if (!provider.trim()) { setError("Vui lòng nhập đơn vị vận chuyển"); return; }
    if (!tracking.trim()) { setError("Vui lòng nhập mã vận đơn"); return; }
    setSaving(true); setError(null);
    try {
      const payload: ShipOrderRequest = { shippingProvider: provider.trim(), trackingNumber: tracking.trim(), note: note.trim() || undefined };
      await shipOrder(orderId, payload);
      onDone();
    } catch {
      setError("Giao hàng thất bại. Vui lòng thử lại.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-full max-w-sm shadow-2xl">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
          <h3 className="font-black text-slate-800">Thông tin giao hàng</h3>
          <button onClick={onClose} className="p-1 rounded-lg hover:bg-slate-100"><X className="w-5 h-5 text-slate-400" /></button>
        </div>
        <div className="p-5 space-y-3">
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Đơn vị vận chuyển *</label>
            <input autoFocus className={cls} value={provider} onChange={e => setProvider(e.target.value)} placeholder="VD: GHN, GHTK, VNPost..." />
          </div>
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Mã vận đơn *</label>
            <input className={cls} value={tracking} onChange={e => setTracking(e.target.value)} placeholder="VD: GHN123456789" />
          </div>
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Ghi chú</label>
            <textarea rows={2} className={`${cls} resize-none`} value={note} onChange={e => setNote(e.target.value)} placeholder="Ghi chú thêm..." />
          </div>
          {error && <p className="text-xs text-red-500 font-semibold">{error}</p>}
        </div>
        <div className="px-5 pb-5 flex justify-end gap-2">
          <button onClick={onClose} className="px-4 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50">Hủy</button>
          <button onClick={handleSave} disabled={saving}
            className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-60 flex items-center gap-2">
            {saving && <Loader2 className="w-3.5 h-3.5 animate-spin" />}<Truck className="w-3.5 h-3.5" />Giao hàng
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Cancel modal ─────────────────────────────────────────────────────────────

function CancelModal({ orderId, onClose, onDone }: { orderId: string; onClose: () => void; onDone: () => void }) {
  const [reason,  setReason]  = useState("");
  const [saving,  setSaving]  = useState(false);
  const [error,   setError]   = useState<string | null>(null);

  const handleSave = async () => {
    if (!reason.trim()) { setError("Vui lòng nhập lý do hủy"); return; }
    setSaving(true); setError(null);
    try {
      await cancelOrder(orderId, { reason: reason.trim() });
      onDone();
    } catch {
      setError("Hủy đơn thất bại. Vui lòng thử lại.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-full max-w-sm shadow-2xl">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
          <h3 className="font-black text-slate-800">Hủy đơn hàng</h3>
          <button onClick={onClose} className="p-1 rounded-lg hover:bg-slate-100"><X className="w-5 h-5 text-slate-400" /></button>
        </div>
        <div className="p-5 space-y-3">
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Lý do hủy *</label>
            <textarea autoFocus rows={3} className="w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400 resize-none"
              value={reason} onChange={e => setReason(e.target.value)} placeholder="Nhập lý do hủy đơn..." />
          </div>
          {error && <p className="text-xs text-red-500 font-semibold">{error}</p>}
        </div>
        <div className="px-5 pb-5 flex justify-end gap-2">
          <button onClick={onClose} className="px-4 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50">Không</button>
          <button onClick={handleSave} disabled={saving}
            className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-60 flex items-center gap-2">
            {saving && <Loader2 className="w-3.5 h-3.5 animate-spin" />}Hủy đơn
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Order detail slide-in ────────────────────────────────────────────────────

function OrderDetailPanel({ orderId, onClose }: { orderId: string; onClose: () => void }) {
  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [printing, setPrinting] = useState(false);

  useEffect(() => {
    getSellerOrderById(orderId).then(setOrder).finally(() => setLoading(false));
  }, [orderId]);

  const handlePrintShippingLabel = async () => {
    if (!order) return;
    setPrinting(true);
    try {
      let tracking = order.trackingNumber;
      if (!tracking) {
        // Create shipping order
        const res = await batchShipOrders([order.id]);
        if (res.failedItems?.length > 0) {
          alert("Lỗi khi tạo đơn giao hàng qua GHN: " + res.failedItems[0].reason);
          return;
        }
        // Fetch updated order to get tracking number
        const updated = await getSellerOrderById(orderId);
        tracking = updated.trackingNumber;
        setOrder(updated);
      }

      if (!tracking) {
        alert("Đơn hàng chưa có mã vận đơn!");
        return;
      }

      const url = await apiPrintShippingLabel([tracking]);
      window.open(url, "_blank");
    } catch (error) {
      alert("Không thể in phiếu giao hàng. Vui lòng thử lại sau.");
    } finally {
      setPrinting(false);
    }
  };

  const cfg = order ? STATUS_CFG[order.status] : null;

  return (
    <>
      <div className="fixed inset-0 z-40 bg-black/30 backdrop-blur-sm" onClick={onClose} />
      <div className="fixed top-0 right-0 h-full z-50 w-full max-w-md bg-white shadow-2xl flex flex-col">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
          <h3 className="font-black text-slate-800">Chi tiết đơn hàng</h3>
          <button onClick={onClose} className="p-1 rounded-lg hover:bg-slate-100"><X className="w-5 h-5 text-slate-400" /></button>
        </div>
        <div className="flex-1 overflow-y-auto p-5">
          {loading ? (
            <div className="flex items-center justify-center py-12 gap-2 text-slate-400">
              <Loader2 className="w-5 h-5 animate-spin" /><span className="text-sm font-semibold">Đang tải...</span>
            </div>
          ) : !order ? (
            <div className="text-center py-12 text-slate-400 text-sm font-semibold">Không tìm thấy đơn hàng</div>
          ) : (
            <div className="space-y-5">
              {/* Header */}
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-mono text-xs text-slate-400">{order.id}</p>
                  <p className="font-black text-slate-800">#{order.orderNumber}</p>
                </div>
                {cfg && <span className={`px-2.5 py-1 rounded-full text-xs font-bold ${cfg.color}`}>{cfg.label}</span>}
              </div>

              {/* Recipient */}
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 space-y-1">
                <p className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-2">Người nhận</p>
                <p className="text-sm font-bold text-slate-800">{order.recipientName}</p>
                <p className="text-xs text-slate-500">{order.recipientPhone}</p>
                <p className="text-xs text-slate-500">{order.shippingAddress}</p>
              </div>

              {/* Items */}
              <div>
                <p className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-2">Sản phẩm ({order.items?.length ?? 0})</p>
                <div className="space-y-2">
                  {(order.items ?? []).map((item, i) => (
                    <div key={item.id ?? i} className="flex gap-3 p-2 rounded-xl border border-slate-100">
                      <div className="w-12 h-12 rounded-lg bg-slate-100 flex items-center justify-center overflow-hidden shrink-0">
                        {item.imageUrl
                          ? <img src={item.imageUrl} alt={item.productName} className="w-full h-full object-cover" />
                          : <Package className="w-4 h-4 text-slate-300" />}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-bold text-slate-800 truncate">{item.productName}</p>
                        {item.variantInfo && <p className="text-xs text-slate-400">{Object.entries(item.variantInfo).map(([k,v]) => `${k}: ${v}`).join(', ')}</p>}
                        <div className="flex items-center justify-between mt-1">
                          <p className="text-xs text-slate-400">x{item.quantity}</p>
                          <p className="text-sm font-black text-red-600">{fmtPrice(item.unitPrice)}</p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Pricing */}
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 space-y-1.5">
                <p className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-2">Thanh toán</p>
                <div className="flex justify-between text-sm"><span className="text-slate-500">Tạm tính</span><span className="font-semibold">{fmtPrice(order.subtotal)}</span></div>
                {(order.discountAmount ?? 0) > 0 && <div className="flex justify-between text-sm"><span className="text-slate-500">Voucher Shop A</span><span className="font-semibold text-green-600">-{fmtPrice(order.discountAmount)}</span></div>}
                {(order.platformVoucherShare ?? 0) > 0 && <div className="flex justify-between text-sm"><span className="text-slate-500">Voucher Sellico (chia)</span><span className="font-semibold text-green-600">-{fmtPrice(order.platformVoucherShare)}</span></div>}
                {(order.shippingFee ?? 0) > 0 && <div className="flex justify-between text-sm"><span className="text-slate-500">Phí ship</span><span className="font-semibold">{fmtPrice(order.shippingFee)}</span></div>}
                <div className="border-t border-slate-200 pt-1.5 flex justify-between text-sm font-black"><span>Thanh toán</span><span className="text-red-600">{fmtPrice(order.totalAmount)}</span></div>
                <p className="text-xs text-slate-400 pt-1">Thanh toán: {order.paymentMethod} · {order.paymentStatus}</p>
              </div>

              {/* Shipping info */}
              {(order.shippingProvider || order.trackingNumber) && (
                <div className="p-3 rounded-xl bg-blue-50 border border-blue-100 space-y-1">
                  <p className="text-xs font-bold text-blue-400 uppercase tracking-widest mb-1">Vận chuyển</p>
                  {order.shippingProvider && <p className="text-sm text-blue-800 font-bold">{order.shippingProvider}</p>}
                  {order.trackingNumber && <p className="font-mono text-xs text-blue-600">{order.trackingNumber}</p>}
                </div>
              )}

              {/* Dates */}
              <div className="space-y-1 text-xs text-slate-400">
                <p>Đặt hàng: {fmtDate(order.createdAt)}</p>
                {order.confirmedAt && <p>Xác nhận: {fmtDate(order.confirmedAt)}</p>}
                {order.shippedAt && <p>Giao hàng: {fmtDate(order.shippedAt)}</p>}
                {order.deliveredAt && <p>Nhận hàng: {fmtDate(order.deliveredAt)}</p>}
                {order.cancelledAt && <p>Hủy: {fmtDate(order.cancelledAt)}{order.cancelReason ? ` — ${order.cancelReason}` : ''}</p>}
              </div>

              {/* Print buttons */}
              <div className="flex gap-2 mt-4">
                <button
                  onClick={() => printInvoice(order)}
                  className="flex-1 flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl border-2 border-slate-200 hover:bg-slate-50 text-slate-700 text-sm font-bold transition"
                >
                  <Printer className="w-4 h-4" /> In hóa đơn
                </button>
                {order.status === "SHIPPED" && (
                  <button
                    onClick={handlePrintShippingLabel}
                    disabled={printing}
                    className="flex-1 flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-900 text-white text-sm font-bold transition disabled:opacity-60"
                  >
                    {printing ? <Loader2 className="w-4 h-4 animate-spin" /> : <Printer className="w-4 h-4" />} In phiếu ship
                  </button>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </>
  );
}

// ─── Main page ─────────────────────────────────────────────────────────────────

type Modal = { type: "ship"; orderId: string } | { type: "cancel"; orderId: string } | null;

export default function SellerOrdersPage() {
  const navigate = useNavigate();
  const [orders,           setOrders]           = useState<OrderResponse[]>([]);
  const [loading,          setLoading]          = useState(false);
  const [statusFilter,     setStatusFilter]     = useState<OrderStatus | "">("");
  const [orderNumberFilter, setOrderNumberFilter] = useState("");
  const [startDateFilter,  setStartDateFilter]  = useState("");
  const [endDateFilter,    setEndDateFilter]    = useState("");
  const [currentPage,      setCurrentPage]      = useState(0);
  const [hasMore,          setHasMore]          = useState(false);
  const [modal,            setModal]            = useState<Modal>(null);
  const [processingId,     setProcessingId]     = useState<string | null>(null);
  const [detailId,         setDetailId]         = useState<string | null>(null);
  const [selectedIds,      setSelectedIds]      = useState<string[]>([]);
  const [batchRes,         setBatchRes]         = useState<BatchResult<string> | null>(null);
  const PAGE_SIZE = 15;
  const totalRef = useRef(0);

  const load = async (p = 0, sf = statusFilter) => {
    setLoading(true);
    try {
      const result = await getSellerOrders({
        status: sf || undefined,
        orderNumber: orderNumberFilter.trim() || undefined,
        startDate: startDateFilter || undefined,
        endDate: endDateFilter || undefined,
        page: p,
        size: PAGE_SIZE,
      });
      setOrders(result.content ?? []);
      setCurrentPage(p);
      totalRef.current = result.totalElements ?? 0;
      setHasMore((p + 1) * PAGE_SIZE < totalRef.current);
      setSelectedIds([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(0); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleConfirm = async (orderId: string) => {
    setProcessingId(orderId);
    try { await confirmOrder(orderId); load(currentPage); }
    finally { setProcessingId(null); }
  };

  const handleDeliver = async (orderId: string) => {
    setProcessingId(orderId);
    try { await deliverOrder(orderId); load(currentPage); }
    finally { setProcessingId(null); }
  };

  const handleComplete = async (orderId: string) => {
    setProcessingId(orderId);
    try { await completeOrder(orderId); load(currentPage); }
    finally { setProcessingId(null); }
  };

  const confirmedOrders = orders.filter(o => o.status === "CONFIRMED");

  const toggleSelectAll = () => {
    if (selectedIds.length === orders.length && orders.length > 0) {
      setSelectedIds([]);
    } else {
      setSelectedIds(orders.map(o => o.id));
    }
  };
  
  const toggleSelect = (id: string) => {
    setSelectedIds(prev => prev.includes(id) ? prev.filter(i => i !== id) : [...prev, id]);
  };

  const handleBatchShip = async () => {
    if (selectedIds.length === 0) return;
    setLoading(true);
    try {
      const res = await batchShipOrders(selectedIds);
      setBatchRes(res);
      setSelectedIds([]);
      load(currentPage);
    } catch {
      alert("Lỗi khi giao hàng hàng loạt!");
    } finally {
      setLoading(false);
    }
  };

  const STATUS_FILTERS: { value: OrderStatus | ""; label: string }[] = [
    { value: "", label: "Tất cả" },
    { value: "PENDING",   label: "Chờ xác nhận" },
    { value: "CONFIRMED", label: "Đã xác nhận"  },
    { value: "SHIPPED",   label: "Đang giao"    },
    { value: "DELIVERED", label: "Đã giao"      },
    { value: "COMPLETED", label: "Hoàn thành"   },
    { value: "CANCELLED", label: "Đã hủy"       },
  ];

  return (
    <div className="space-y-5">
      {batchRes && (
        <div className="p-4 rounded-xl border mb-4 bg-white shadow-sm">
          <div className="flex justify-between items-start mb-2">
            <h3 className="font-bold text-slate-800">Kết quả Giao Hàng</h3>
            <button onClick={() => setBatchRes(null)} className="p-1 hover:bg-slate-100 rounded-lg"><X className="w-4 h-4" /></button>
          </div>
          {batchRes.failedItems?.length === 0 ? (
            <p className="text-green-600 font-semibold text-sm">Thành công! Đã gửi {batchRes.successItems.length} đơn hàng sang GHN.</p>
          ) : (
            <div>
              <p className="text-amber-600 font-semibold text-sm mb-2">Thành công: {batchRes.successItems.length} đơn. Thất bại: {batchRes.failedItems.length} đơn.</p>
              <ul className="text-xs text-red-600 list-disc list-inside ml-4 space-y-1">
                {batchRes.failedItems.map((fail, i) => {
                  const o = orders.find(ord => ord.id === fail.id);
                  const displayId = o ? `#${o.orderNumber}` : fail.id;
                  let reason = fail.reason;
                  try {
                    const jsonMatch = reason.match(/\[(\{.*\})\]/);
                    if (jsonMatch) {
                      const parsed = JSON.parse(jsonMatch[1]);
                      if (parsed.message) reason = parsed.message;
                    }
                  } catch {}
                  return (
                    <li key={i}>Đơn {displayId}: {reason}</li>
                  );
                })}
              </ul>
            </div>
          )}
        </div>
      )}

      <div className="flex flex-col gap-4">
        <div className="flex items-center justify-between flex-wrap gap-3">
          <h1 className="text-xl font-black text-slate-900">Đơn hàng</h1>
          <div className="flex items-center gap-2">
            <button onClick={() => load(currentPage)} className="p-2 rounded-xl border border-slate-200 hover:bg-slate-50">
              <RefreshCw className="w-4 h-4 text-slate-500" />
            </button>
          </div>
        </div>

        <div className="grid gap-3 md:grid-cols-[1fr_auto]">
          <div className="grid gap-3 md:grid-cols-[1.5fr_1fr_1fr] bg-white rounded-2xl border border-slate-200 p-4 shadow-sm">
            <input
              type="text"
              value={orderNumberFilter}
              onChange={e => setOrderNumberFilter(e.target.value)}
              placeholder="Tìm theo mã đơn hàng"
              className="w-full px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 text-sm outline-none focus:border-red-400"
            />
            <input
              type="date"
              value={startDateFilter}
              onChange={e => setStartDateFilter(e.target.value)}
              className="w-full px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 text-sm outline-none focus:border-red-400"
            />
            <input
              type="date"
              value={endDateFilter}
              onChange={e => setEndDateFilter(e.target.value)}
              className="w-full px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 text-sm outline-none focus:border-red-400"
            />
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => load(0)}
              disabled={loading}
              className="flex-1 px-4 py-2 rounded-xl bg-red-600 text-white text-sm font-bold hover:bg-red-700 disabled:opacity-50"
            >
              Áp dụng
            </button>
            <button
              onClick={() => {
                setOrderNumberFilter("");
                setStartDateFilter("");
                setEndDateFilter("");
                setStatusFilter("");
                load(0, "");
              }}
              disabled={loading}
              className="flex-1 px-4 py-2 rounded-xl border border-slate-200 bg-white text-sm font-bold text-slate-700 hover:bg-slate-50 disabled:opacity-50"
            >
              Xóa
            </button>
          </div>
        </div>
      </div>

      {/* Status filter */}
      <div className="flex flex-wrap gap-2">
        {STATUS_FILTERS.map(({ value, label }) => (
          <button key={value} onClick={() => { setStatusFilter(value); setSelectedIds([]); load(0, value); }}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold border transition-all ${
              statusFilter === value ? "bg-red-600 text-white border-red-600" : "bg-white text-slate-600 border-slate-200 hover:border-red-300"
            }`}>
            {label}
          </button>
        ))}
      </div>

      {/* Batch Ship Button - only show in CONFIRMED tab when items selected */}
      {statusFilter === "CONFIRMED" && selectedIds.length > 0 && (
        <div className="flex items-center gap-2 p-3 bg-red-50 rounded-xl border border-red-200">
          <p className="text-sm font-semibold text-red-700 flex-1">
            Đã chọn {selectedIds.length} đơn hàng
          </p>
          <button
            onClick={handleBatchShip}
            disabled={loading}
            className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-60 flex items-center gap-2"
          >
            {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Truck className="w-4 h-4" />}
            Giao hàng ({selectedIds.length})
          </button>
        </div>
      )}

      {/* Table */}
      <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
        {loading ? (
          <div className="flex items-center justify-center py-16 gap-2 text-slate-400">
            <Loader2 className="w-5 h-5 animate-spin" /><span className="text-sm font-semibold">Đang tải...</span>
          </div>
        ) : orders.length === 0 ? (
          <div className="text-center py-16 text-slate-400 text-sm font-semibold">Không có đơn hàng nào</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm min-w-[700px]">
              <thead>
                <tr className="bg-slate-50 text-left">
                  {statusFilter === "CONFIRMED" && (
                    <th className="px-4 py-3 w-10">
                      <input type="checkbox" className="rounded border-slate-300 text-red-600 focus:ring-red-500 cursor-pointer"
                        checked={orders.length > 0 && selectedIds.length === orders.length}
                        onChange={toggleSelectAll} />
                    </th>
                  )}
                  <th className="px-4 py-3 text-xs font-black text-slate-500 uppercase tracking-wider">Đơn hàng</th>
                  <th className="px-4 py-3 text-xs font-black text-slate-500 uppercase tracking-wider">Người nhận</th>
                  <th className="px-4 py-3 text-xs font-black text-slate-500 uppercase tracking-wider">Sản phẩm</th>
                  <th className="px-4 py-3 text-xs font-black text-slate-500 uppercase tracking-wider">Tổng tiền</th>
                  <th className="px-4 py-3 text-xs font-black text-slate-500 uppercase tracking-wider">Trạng thái</th>
                  <th className="px-4 py-3 text-xs font-black text-slate-500 uppercase tracking-wider">Ngày đặt</th>
                  <th className="px-4 py-3 text-xs font-black text-slate-500 uppercase tracking-wider">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {orders.map(order => {
                  const cfg = STATUS_CFG[order.status];
                  const itemCount = order.items?.length ?? 0;
                  return (
                    <tr key={order.id} className="hover:bg-slate-50/60 transition-colors">
                      {statusFilter === "CONFIRMED" && (
                        <td className="px-4 py-3">
                          <input type="checkbox" className="rounded border-slate-300 text-red-600 focus:ring-red-500 cursor-pointer"
                            checked={selectedIds.includes(order.id)}
                            onChange={() => toggleSelect(order.id)} />
                        </td>
                      )}
                      <td className="px-4 py-3">
                        <p className="font-mono font-bold text-slate-800 text-xs">#{order.orderNumber}</p>
                      </td>
                      <td className="px-4 py-3">
                        <p className="font-semibold text-slate-800 text-xs">{order.recipientName}</p>
                        <p className="text-xs text-slate-400">{order.recipientPhone}</p>
                      </td>
                      <td className="px-4 py-3">
                        <p className="text-xs text-slate-600">{itemCount} sản phẩm</p>
                        {order.items?.[0] && (
                          <p className="text-xs text-slate-400 max-w-32 truncate">{order.items[0].productName}</p>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        <p className="font-black text-red-600 text-sm">{fmtPrice(order.totalAmount)}</p>
                        <p className="text-[10px] text-slate-400">{order.paymentMethod}</p>
                      </td>
                      <td className="px-4 py-3">
                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${cfg.color}`}>{cfg.label}</span>
                      </td>
                      <td className="px-4 py-3">
                        <p className="text-xs text-slate-500">{fmtDate(order.createdAt)}</p>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-1">
                          {/* Detail */}
                          <button onClick={() => setDetailId(order.id)}
                            className="p-1.5 rounded-lg hover:bg-red-50 text-slate-400 hover:text-red-600" title="Chi tiết">
                            <Eye className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => navigate(`/seller/messages?peerId=${encodeURIComponent(order.userId)}`)}
                            className="px-2 py-1 rounded-lg border border-slate-200 text-[10px] font-bold text-slate-600 hover:border-red-300 hover:text-red-600"
                            title="Chat với người mua"
                          >
                            Chat
                          </button>
                          {/* Confirm */}
                          {order.status === "PENDING" && (
                            <button onClick={() => handleConfirm(order.id)} disabled={processingId === order.id}
                              className="p-1.5 rounded-lg hover:bg-green-50 text-slate-400 hover:text-green-600 disabled:opacity-50" title="Xác nhận">
                              {processingId === order.id ? <Loader2 className="w-4 h-4 animate-spin" /> : <ClipboardCheck className="w-4 h-4" />}
                            </button>
                          )}
                          {/* Ship */}
                          {order.status === "CONFIRMED" && statusFilter !== "CONFIRMED" && (
                            <button onClick={() => setModal({ type: "ship", orderId: order.id })}
                              className="p-1.5 rounded-lg hover:bg-blue-50 text-slate-400 hover:text-blue-600" title="Giao hàng">
                              <Truck className="w-4 h-4" />
                            </button>
                          )}
                          {/* Mark delivered */}
                          {order.status === "SHIPPED" && (
                            <button onClick={() => handleDeliver(order.id)}
                              disabled={processingId === order.id}
                              className="p-1.5 rounded-lg hover:bg-green-50 text-slate-400 hover:text-green-600 disabled:opacity-50"
                              title="Đã giao">
                              {processingId === order.id ? <Loader2 className="w-4 h-4 animate-spin" /> : <ClipboardCheck className="w-4 h-4" />}
                            </button>
                          )}
                          {/* Mark completed */}
                          {order.status === "DELIVERED" && (
                            <button onClick={() => handleComplete(order.id)}
                              disabled={processingId === order.id}
                              className="p-1.5 rounded-lg hover:bg-teal-50 text-slate-400 hover:text-teal-600 disabled:opacity-50"
                              title="Hoàn thành">
                              {processingId === order.id ? <Loader2 className="w-4 h-4 animate-spin" /> : <ClipboardCheck className="w-4 h-4" />}
                            </button>
                          )}
                          {/* Cancel */}
                          {(order.status === "PENDING" || order.status === "CONFIRMED") && (
                            <button onClick={() => setModal({ type: "cancel", orderId: order.id })}
                              className="p-1.5 rounded-lg hover:bg-red-50 text-slate-400 hover:text-red-500" title="Hủy đơn">
                              <XCircle className="w-4 h-4" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Pagination */}
      {(currentPage > 0 || hasMore) && (
        <div className="flex items-center justify-between">
          <span className="text-xs text-slate-400 font-semibold">Trang {currentPage + 1}</span>
          <div className="flex gap-1">
            <button disabled={currentPage === 0} onClick={() => load(currentPage - 1)}
              className="p-1.5 rounded-lg border border-slate-200 disabled:opacity-40 hover:bg-slate-50">
              <ChevronLeft className="w-4 h-4 text-slate-600" />
            </button>
            <button disabled={!hasMore} onClick={() => load(currentPage + 1)}
              className="p-1.5 rounded-lg border border-slate-200 disabled:opacity-40 hover:bg-slate-50">
              <ChevronRight className="w-4 h-4 text-slate-600" />
            </button>
          </div>
        </div>
      )}

      {/* Modals */}
      {modal?.type === "ship" && (
        <ShipModal orderId={modal.orderId} onClose={() => setModal(null)} onDone={() => { setModal(null); load(currentPage); }} />
      )}
      {modal?.type === "cancel" && (
        <CancelModal orderId={modal.orderId} onClose={() => setModal(null)} onDone={() => { setModal(null); load(currentPage); }} />
      )}
      {detailId && (
        <OrderDetailPanel orderId={detailId} onClose={() => setDetailId(null)} />
      )}
    </div>
  );
}
