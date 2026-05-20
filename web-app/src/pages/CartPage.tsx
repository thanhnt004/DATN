import { useState, useEffect, useCallback } from "react";
import { Link, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import {
  Trash2, Minus, Plus, ShoppingBag, ArrowLeft, Heart, Store,
  X, ChevronDown, Loader2,
} from "lucide-react";
import {
  getCartBySeller,
  updateCartItem,
  removeCartItem,
  removeCartItems,
  updateSelection,
  selectAll,
  saveForLater,
  addToCart,
} from "../api/cartApi";
import { getProductById } from "../api/productApi";
import { getSellerPublic } from "../api/sellerPublicApi";
import { formatPrice } from "../utils/helpers";
import type { CartBySellerResponse, CartItemResponse } from "../types/cart";
import type { ProductResponse } from "../types/product";
import type { SellerResponse } from "../types/seller";

/* ═══════════════════════ Variant-change modal ═══════════════════════ */

interface VariantModalProps {
  product: ProductResponse;
  item: CartItemResponse;
  currentSkuId: string;
  currentQty: number;
  onConfirm: (newSkuId: string, qty: number) => void;
  onClose: () => void;
}

function VariantModal({ product, item, currentSkuId, currentQty, onConfirm, onClose }: VariantModalProps) {
  const currentSku = product.skus.find(s => s.id === currentSkuId);

  // Build initial selection from current SKU attributes, or fallback to cart item attributes
  const buildInitialSelection = (): Record<string, string> => {
    const sel: Record<string, string> = {};
    
    // First priority: use current SKU attributes
    if (currentSku && currentSku.attributes && currentSku.attributes.length > 0) {
      currentSku.attributes.forEach(a => { sel[a.optionName] = a.valueName; });
    }
    
    // Second priority: use attributes from cart item (exists even if SKU data unavailable)
    if (Object.keys(sel).length === 0 && item.attributes && typeof item.attributes === 'object' && Object.keys(item.attributes).length > 0) {
      Object.assign(sel, item.attributes);
    }

    // Third priority: reverse-calculate Cartesian index if attributes are missing
    if (Object.keys(sel).length === 0 && product.options.length > 0 && product.skus.length > 0) {
      const isMissingAttrs = product.skus.every(s => !s.attributes || s.attributes.length === 0);
      if (isMissingAttrs) {
        const skuIndex = product.skus.findIndex(s => s.id === currentSkuId);
        if (skuIndex !== -1) {
          let expectedCount = 1;
          product.options.forEach(opt => expectedCount *= opt.values.length);
          if (expectedCount === product.skus.length) {
            let remainder = skuIndex;
            let multiplier = 1;
            product.options.forEach(opt => multiplier *= opt.values.length);
            for (let i = 0; i < product.options.length; i++) {
              const opt = product.options[i];
              multiplier = Math.floor(multiplier / opt.values.length);
              const valIndex = Math.floor(remainder / multiplier);
              remainder %= multiplier;
              if (opt.values[valIndex]) {
                sel[opt.name] = opt.values[valIndex].value;
              }
            }
          }
        }
      }
    }
    
    // Fill any missing options with first available value
    for (const opt of product.options) {
      if (!sel[opt.name] && opt.values.length > 0) {
        sel[opt.name] = opt.values[0].value;
      }
    }
    return sel;
  };

  const [selected, setSelected] = useState<Record<string, string>>(buildInitialSelection);
  const [qty, setQty] = useState(currentQty);

  // Match SKU: find SKU where all its attributes match the selected values (strict matching)
  let matchedSku = product.skus.find(s => {
    const attrs = s.attributes || [];
    if (product.options.length > 0 && attrs.length === 0) return false;
    return attrs.length === Object.keys(selected).length &&
           attrs.every(a => selected[a.optionName] === a.valueName);
  });

  // Fallback: if backend returns empty attributes, calculate cartesian index
  if (!matchedSku && product.options.length > 0 && product.skus.length > 0) {
    const isMissingAttrs = product.skus.every(s => !s.attributes || s.attributes.length === 0);
    if (isMissingAttrs) {
      let expectedCount = 1;
      product.options.forEach(opt => expectedCount *= opt.values.length);
      if (expectedCount === product.skus.length) {
        let index = 0;
        let multiplier = 1;
        for (let i = product.options.length - 1; i >= 0; i--) {
          const opt = product.options[i];
          const valIndex = opt.values.findIndex(v => v.value === selected[opt.name]);
          if (valIndex !== -1) index += valIndex * multiplier;
          multiplier *= opt.values.length;
        }
        matchedSku = product.skus[index];
      }
    }
  }

  // Fallback: if product has single SKU with no attributes (no options), auto-match it
  const resolvedSku = matchedSku || (product.skus.length === 1 && product.options.length === 0 ? product.skus[0] : null);

  const previewImg = (() => {
    for (const opt of product.options) {
      const val = opt.values.find(v => v.value === selected[opt.name]);
      if (val?.imageUrl) return val.imageUrl;
    }
    const primary = product.images.find(i => i.isPrimary);
    return primary?.url || product.images[0]?.url || null;
  })();

  return (
    <div className="fixed inset-0 z-[60] flex items-end sm:items-center justify-center bg-black/40 backdrop-blur-sm" onClick={onClose}>
      <div className="bg-white w-full sm:max-w-md sm:rounded-2xl rounded-t-2xl max-h-[85vh] flex flex-col" onClick={e => e.stopPropagation()}>
        {/* Header */}
        <div className="flex items-start gap-3 p-4 border-b border-slate-100">
          {previewImg && (
            <img src={previewImg} alt="" className="w-20 h-20 rounded-xl object-cover border" />
          )}
          <div className="flex-1 min-w-0">
            <p className="text-sm font-semibold text-slate-900 line-clamp-2">{product.name}</p>
            {resolvedSku ? (
              <>
                <p className="text-lg font-bold text-red-600 mt-1">{formatPrice(resolvedSku.price)}</p>
                <p className="text-xs text-slate-400">Kho: {resolvedSku.status === "ACTIVE" ? "Còn hàng" : "Hết hàng"}</p>
              </>
            ) : (
              <p className="text-xs text-orange-500 mt-1 font-medium">Vui lòng chọn phân loại hàng</p>
            )}
          </div>
          <button onClick={onClose} className="p-1 rounded-lg hover:bg-slate-100">
            <X className="w-5 h-5 text-slate-400" />
          </button>
        </div>

        {/* Options */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {product.options.map(opt => (
            <div key={opt.id}>
              <p className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">{opt.name}</p>
              <div className="flex flex-wrap gap-2">
                {opt.values.map(val => {
                  const isSelected = selected[opt.name] === val.value;
                  return (
                    <button
                      key={val.id}
                      onClick={() => setSelected(prev => ({ ...prev, [opt.name]: val.value }))}
                      className={`flex items-center gap-2 px-3 py-2 rounded-xl border-2 text-sm font-medium transition-all ${
                        isSelected
                          ? "border-red-500 bg-red-50 text-red-700"
                          : "border-slate-100 bg-slate-50 text-slate-600 hover:border-red-200"
                      }`}
                    >
                      {val.imageUrl && (
                        <img src={val.imageUrl} alt={val.value} className="w-6 h-6 rounded object-cover" />
                      )}
                      {val.value}
                    </button>
                  );
                })}
              </div>
            </div>
          ))}

          {/* Quantity */}
          <div>
            <p className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Số lượng</p>
            <div className="flex items-center border rounded-xl w-fit">
              <button onClick={() => setQty(q => Math.max(1, q - 1))} className="px-3 py-2 hover:bg-slate-50">
                <Minus className="w-3.5 h-3.5" />
              </button>
              <span className="px-4 py-2 text-sm font-semibold border-x min-w-[48px] text-center">{qty}</span>
              <button onClick={() => setQty(q => q + 1)} className="px-3 py-2 hover:bg-slate-50">
                <Plus className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-slate-100 space-y-2">
          {!resolvedSku && (
            <p className="text-xs text-center text-orange-500 font-medium">Phân loại này không tồn tại, vui lòng chọn lại</p>
          )}
          <button
            onClick={() => resolvedSku && onConfirm(resolvedSku.id, qty)}
            disabled={!resolvedSku}
            className="w-full py-3 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-sm disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
          >
            Xác nhận
          </button>
        </div>
      </div>
    </div>
  );
}

/* ═══════════════════════ Main CartPage ═══════════════════════ */

export default function CartPage() {
  const navigate = useNavigate();
  const [sellerGroups, setSellerGroups] = useState<CartBySellerResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState<string | null>(null);

  // Enrichment caches
  const [sellerInfoMap, setSellerInfoMap] = useState<Record<string, SellerResponse>>({});
  const [productCache, setProductCache] = useState<Record<string, ProductResponse>>({});
  const [productImageMap, setProductImageMap] = useState<Record<string, string>>({});

  // Variant modal
  const [variantModal, setVariantModal] = useState<{ item: CartItemResponse; product: ProductResponse } | null>(null);

  const fetchCart = useCallback(async () => {
    try {
      const res = await getCartBySeller();
      const groups = res.data.result ?? [];
      setSellerGroups(groups);

      // Fetch seller info for each unique seller
      const sellerIds = [...new Set(groups.map(g => g.sellerId))];
      sellerIds.forEach(async (sid) => {
        try {
          const sRes = await getSellerPublic(sid);
          const seller = sRes.data.result;
          if (seller) setSellerInfoMap(prev => ({ ...prev, [sid]: seller }));
        } catch { /* ignore */ }
      });

      // Fetch product primary images for items with null imageUrl
      const itemsNeedingImage = groups.flatMap(g => g.items).filter(i => !i.imageUrl);
      const productIds = [...new Set(itemsNeedingImage.map(i => i.productId))];
      productIds.forEach(async (pid) => {
        try {
          const pRes = await getProductById(pid);
          const product = pRes.data.result;
          if (product) {
            const primary = product.images.find(img => img.isPrimary);
            const url = primary?.url || product.images[0]?.url || null;
            if (url) setProductImageMap(prev => ({ ...prev, [pid]: url }));
          }
        } catch { /* ignore */ }
      });
    } catch {
      setSellerGroups([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchCart(); }, [fetchCart]);

  const allItems = sellerGroups.flatMap(g => g.items);
  const selectedItems = allItems.filter(i => i.selected);
  const totalSelected = selectedItems.reduce((sum, i) => sum + i.subtotal, 0);
  const totalSelectedCount = selectedItems.reduce((sum, i) => sum + i.quantity, 0);
  const allSelected = allItems.length > 0 && selectedItems.length === allItems.length;

  const handleQuantityChange = async (item: CartItemResponse, newQty: number) => {
    if (newQty < 1 || newQty > item.availableStock) return;
    setUpdating(item.skuId);
    try {
      await updateCartItem(item.skuId, { quantity: newQty });
      fetchCart();
    } catch {
      toast.error("Cập nhật số lượng thất bại");
    } finally {
      setUpdating(null);
    }
  };

  const handleRemoveItem = async (skuId: string) => {
    setUpdating(skuId);
    try {
      await removeCartItem(skuId);
      toast.success("Đã xóa sản phẩm khỏi giỏ hàng");
      fetchCart();
    } catch {
      toast.error("Xóa sản phẩm thất bại");
    } finally {
      setUpdating(null);
    }
  };

  const handleRemoveSelected = async () => {
    if (selectedItems.length === 0) return;
    if (!confirm(`Xóa ${selectedItems.length} sản phẩm đã chọn?`)) return;
    try {
      await removeCartItems({ skuIds: selectedItems.map(i => i.skuId) });
      toast.success(`Đã xóa ${selectedItems.length} sản phẩm`);
      fetchCart();
    } catch {
      toast.error("Xóa sản phẩm thất bại");
    }
  };

  const handleToggleItem = async (item: CartItemResponse) => {
    try {
      await updateSelection({ selected: !item.selected, skuIds: [item.skuId] });
      fetchCart();
    } catch { /* ignore */ }
  };

  const handleToggleSeller = async (group: CartBySellerResponse) => {
    try {
      await updateSelection({ selected: !group.allSelected, sellerId: group.sellerId });
      fetchCart();
    } catch { /* ignore */ }
  };

  const handleSelectAll = async () => {
    try {
      await selectAll(!allSelected);
      fetchCart();
    } catch { /* ignore */ }
  };

  const handleSaveForLater = async (item: CartItemResponse) => {
    try {
      await saveForLater({ skuId: item.skuId, productId: item.productId });
      await removeCartItem(item.skuId);
      toast.success("Đã lưu sản phẩm");
      fetchCart();
    } catch {
      toast.error("Lưu sản phẩm thất bại");
    }
  };

  const handleCheckout = () => {
    if (selectedItems.length === 0) {
      toast.error("Vui lòng chọn sản phẩm để thanh toán");
      return;
    }
    navigate("/checkout");
  };

  // Open variant modal — fetch product if not cached
  const handleOpenVariant = async (item: CartItemResponse) => {
    if (productCache[item.productId]) {
      setVariantModal({ item, product: productCache[item.productId] });
      return;
    }
    setUpdating(item.skuId);
    try {
      const res = await getProductById(item.productId);
      const product = res.data.result;
      if (product) {
        setProductCache(prev => ({ ...prev, [item.productId]: product }));
        setVariantModal({ item, product });
      }
    } catch {
      toast.error("Không thể tải thông tin sản phẩm");
    } finally {
      setUpdating(null);
    }
  };

  // Variant change: remove old SKU, add new one
  const handleVariantChange = async (newSkuId: string, qty: number) => {
    if (!variantModal) return;
    const { item } = variantModal;
    setVariantModal(null);
    setUpdating(item.skuId);
    try {
      if (newSkuId === item.skuId) {
        if (qty !== item.quantity) await updateCartItem(item.skuId, { quantity: qty });
      } else {
        await removeCartItem(item.skuId);
        await addToCart({ skuId: newSkuId, productId: item.productId, sellerId: item.sellerId, quantity: qty });
      }
      toast.success("Đã cập nhật phân loại");
      fetchCart();
    } catch {
      toast.error("Cập nhật phân loại thất bại");
    } finally {
      setUpdating(null);
    }
  };

  // Resolve image for an item
  const getItemImage = (item: CartItemResponse) =>
    item.imageUrl || productImageMap[item.productId] || "https://placehold.co/80x80?text=N/A";

  // Resolve seller info
  const getSellerInfo = (group: CartBySellerResponse) => {
    const info = sellerInfoMap[group.sellerId];
    return {
      name: info?.shopName || group.sellerName || `Shop #${group.sellerId.slice(0, 8)}`,
      logo: info?.logoUrl || null,
    };
  };

  // Resolve variant display name
  const getVariantDisplay = (item: CartItemResponse) => {
    if (item.attributes && Object.keys(item.attributes).length > 0) {
      return Object.values(item.attributes).join(", ");
    }
    const prod = productCache[item.productId];
    if (prod && prod.options && prod.options.length > 0 && prod.skus && prod.skus.length > 0) {
      const isMissingAttrs = prod.skus.every(s => !s.attributes || s.attributes.length === 0);
      if (isMissingAttrs) {
        const skuIndex = prod.skus.findIndex(s => s.id === item.skuId);
        if (skuIndex !== -1) {
          let expectedCount = 1;
          prod.options.forEach(opt => expectedCount *= opt.values.length);
          if (expectedCount === prod.skus.length) {
            const vals: string[] = [];
            let remainder = skuIndex;
            let multiplier = 1;
            prod.options.forEach(opt => multiplier *= opt.values.length);
            for (let i = 0; i < prod.options.length; i++) {
              const opt = prod.options[i];
              multiplier = Math.floor(multiplier / opt.values.length);
              const valIndex = Math.floor(remainder / multiplier);
              remainder %= multiplier;
              if (opt.values[valIndex]) {
                vals.push(opt.values[valIndex].value);
              }
            }
            if (vals.length > 0) return vals.join(", ");
          }
        }
      }
    }
    return item.skuCode || "Phân loại";
  };

  /* ── Loading ── */
  if (loading) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-12 flex justify-center">
        <Loader2 className="w-8 h-8 animate-spin text-slate-400" />
      </div>
    );
  }

  /* ── Empty ── */
  if (allItems.length === 0) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-20 text-center">
        <ShoppingBag className="w-20 h-20 text-gray-300 mx-auto mb-4" />
        <h2 className="text-2xl font-bold text-gray-700 mb-2">Giỏ hàng trống</h2>
        <p className="text-gray-500 mb-6">Hãy thêm sản phẩm vào giỏ hàng của bạn</p>
        <Link
          to="/"
          className="inline-flex items-center gap-2 bg-red-500 text-white px-6 py-3 rounded-lg font-semibold hover:bg-red-600 transition"
        >
          <ArrowLeft className="w-4 h-4" /> Tiếp tục mua sắm
        </Link>
      </div>
    );
  }

  /* ── Main cart ── */
  return (
    <>
      <div className="max-w-6xl mx-auto px-2 sm:px-4 py-4 sm:py-6 pb-36 sm:pb-6">
        <h1 className="text-xl sm:text-2xl font-bold text-gray-900 mb-4 sm:mb-6 px-2 sm:px-0">
          Giỏ hàng <span className="text-gray-500 font-normal text-base sm:text-lg">({allItems.length} sản phẩm)</span>
        </h1>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 sm:gap-6">
          {/* Cart Items Column */}
          <div className="lg:col-span-2 space-y-3 sm:space-y-4">
            {/* Select All */}
            <div className="bg-white rounded-xl border p-3 sm:p-4 flex items-center justify-between">
              <label className="flex items-center gap-3 cursor-pointer">
                <input type="checkbox" checked={allSelected} onChange={handleSelectAll}
                  className="w-4 h-4 rounded border-gray-300 text-red-500 focus:ring-red-500" />
                <span className="font-medium text-sm">Chọn tất cả ({allItems.length})</span>
              </label>
              {selectedItems.length > 0 && (
                <button onClick={handleRemoveSelected} className="text-sm text-red-500 hover:text-red-600 font-medium">
                  Xóa đã chọn ({selectedItems.length})
                </button>
              )}
            </div>

            {/* Seller Groups */}
            {sellerGroups.map(group => {
              const seller = getSellerInfo(group);
              return (
                <div key={group.sellerId} className="bg-white rounded-xl border overflow-hidden">
                  {/* Seller Header */}
                  <div className="flex items-center gap-3 p-3 sm:p-4 border-b bg-slate-50/80">
                    <input type="checkbox" checked={group.allSelected} onChange={() => handleToggleSeller(group)}
                      className="w-4 h-4 rounded border-gray-300 text-red-500 focus:ring-red-500" />
                    <Link to={`/shop/${group.sellerId}`} className="flex items-center gap-2 hover:opacity-80 transition-opacity min-w-0">
                      {seller.logo ? (
                        <img src={seller.logo} alt={seller.name} className="w-6 h-6 rounded-full object-cover border" />
                      ) : (
                        <div className="w-6 h-6 rounded-full bg-red-100 flex items-center justify-center">
                          <Store className="w-3.5 h-3.5 text-red-500" />
                        </div>
                      )}
                      <span className="font-semibold text-sm text-slate-800 truncate">{seller.name}</span>
                      <ChevronDown className="w-3.5 h-3.5 text-slate-400 -rotate-90 shrink-0" />
                    </Link>
                  </div>

                  {/* Items */}
                  <div className="divide-y divide-slate-50">
                    {group.items.map(item => (
                      <div key={item.skuId} className={`p-3 sm:p-4 ${!item.inStock ? "opacity-50" : ""}`}>
                        <div className="flex gap-3">
                          <input type="checkbox" checked={item.selected} onChange={() => handleToggleItem(item)}
                            disabled={!item.inStock}
                            className="w-4 h-4 rounded border-gray-300 text-red-500 focus:ring-red-500 mt-1 shrink-0" />

                          <Link to={`/product/${item.productId}`} className="shrink-0">
                            <img src={getItemImage(item)} alt={item.productName || "Sản phẩm"}
                              className="w-20 h-20 sm:w-24 sm:h-24 object-cover rounded-xl border" />
                          </Link>

                          <div className="flex-1 min-w-0">
                            {item.productName ? (
                              <Link to={`/product/${item.productId}`}
                                className="text-sm font-medium text-gray-900 hover:text-red-500 line-clamp-2 leading-snug">
                                {item.productName}
                              </Link>
                            ) : (
                              <p className="text-sm font-medium text-red-500">Sản phẩm không còn tồn tại</p>
                            )}

                            {/* Variant — clickable to change */}
                            {((item.attributes && Object.keys(item.attributes).length > 0) || item.skuCode) && (
                              <button onClick={() => handleOpenVariant(item)} disabled={updating === item.skuId}
                                className="mt-1 flex items-center gap-1 text-xs bg-slate-100 text-slate-600 px-2 py-1 rounded-lg hover:bg-red-50 hover:text-red-600 transition-colors group">
                                <span className="font-medium">Phân loại: </span>
                                {getVariantDisplay(item)}
                                {updating === item.skuId ? (
                                  <Loader2 className="w-3 h-3 animate-spin ml-1" />
                                ) : (
                                  <ChevronDown className="w-3 h-3 ml-0.5 group-hover:text-red-500" />
                                )}
                              </button>
                            )}

                            {!item.inStock && <p className="text-xs text-red-500 font-medium mt-1">Hết hàng</p>}

                            {/* Price + quantity + actions */}
                            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mt-2 gap-2">
                              <p className="text-red-600 font-bold text-sm sm:text-base">{formatPrice(item.price)}</p>
                              <div className="flex items-center gap-2">
                                <div className="flex items-center border rounded-lg">
                                  <button onClick={() => handleQuantityChange(item, item.quantity - 1)}
                                    disabled={updating === item.skuId || item.quantity <= 1}
                                    className="px-2 py-1 hover:bg-gray-100 disabled:opacity-40 rounded-l-lg">
                                    <Minus className="w-3 h-3" />
                                  </button>
                                  <span className="px-3 py-1 text-sm min-w-[36px] text-center border-x font-medium">{item.quantity}</span>
                                  <button onClick={() => handleQuantityChange(item, item.quantity + 1)}
                                    disabled={updating === item.skuId || item.quantity >= item.availableStock}
                                    className="px-2 py-1 hover:bg-gray-100 disabled:opacity-40 rounded-r-lg">
                                    <Plus className="w-3 h-3" />
                                  </button>
                                </div>
                                <button onClick={() => handleSaveForLater(item)}
                                  className="p-1.5 text-gray-400 hover:text-red-500 transition" title="Lưu lại sau">
                                  <Heart className="w-4 h-4" />
                                </button>
                                <button onClick={() => handleRemoveItem(item.skuId)}
                                  className="p-1.5 text-gray-400 hover:text-red-500 transition" title="Xóa">
                                  <Trash2 className="w-4 h-4" />
                                </button>
                              </div>
                            </div>

                            {item.quantity > 1 && (
                              <p className="text-xs text-slate-400 mt-1">
                                Tổng: <span className="font-semibold text-slate-600">{formatPrice(item.subtotal)}</span>
                              </p>
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              );
            })}
          </div>

          {/* Order Summary — Desktop sidebar */}
          <div className="hidden lg:block lg:col-span-1">
            <div className="bg-white rounded-xl border p-6 sticky top-24">
              <h3 className="font-bold text-gray-900 mb-4">Tóm tắt đơn hàng</h3>
              <div className="space-y-3 text-sm">
                <div className="flex justify-between text-gray-600">
                  <span>Số sản phẩm đã chọn</span>
                  <span>{totalSelectedCount}</span>
                </div>
                <div className="flex justify-between text-gray-600">
                  <span>Tạm tính</span>
                  <span>{formatPrice(totalSelected)}</span>
                </div>
                <div className="border-t pt-3 flex justify-between">
                  <span className="font-bold text-gray-900">Tổng cộng</span>
                  <span className="font-bold text-xl text-red-600">{formatPrice(totalSelected)}</span>
                </div>
              </div>
              <button onClick={handleCheckout} disabled={selectedItems.length === 0}
                className="w-full mt-6 bg-red-500 text-white py-3 rounded-xl font-semibold hover:bg-red-600 transition disabled:bg-gray-300 disabled:cursor-not-allowed">
                Thanh toán ({selectedItems.length})
              </button>
              <Link to="/" className="block text-center text-sm text-red-500 hover:text-red-600 mt-3 font-medium">
                Tiếp tục mua sắm
              </Link>
            </div>
          </div>
        </div>
      </div>

      {/* Order Summary — Mobile horizontal bottom bar */}
      <div className="lg:hidden fixed bottom-0 inset-x-0 z-40 bg-white border-t shadow-[0_-4px_20px_rgba(0,0,0,0.08)]">
        <div className="px-4 py-2 flex items-center justify-between gap-3">
          <label className="flex items-center gap-2 cursor-pointer shrink-0">
            <input type="checkbox" checked={allSelected} onChange={handleSelectAll}
              className="w-4 h-4 rounded border-gray-300 text-red-500 focus:ring-red-500" />
            <span className="text-xs font-medium text-slate-600">Tất cả</span>
          </label>
          <div className="flex items-center gap-3">
            <div className="text-right">
              <p className="text-[10px] text-slate-400">Tổng cộng</p>
              <p className="text-base font-bold text-red-600 leading-tight">{formatPrice(totalSelected)}</p>
              <p className="text-[10px] text-slate-400">{totalSelectedCount} sản phẩm</p>
            </div>
            <button onClick={handleCheckout} disabled={selectedItems.length === 0}
              className="bg-red-500 text-white px-5 py-2.5 rounded-xl font-bold text-sm hover:bg-red-600 transition disabled:bg-gray-300 disabled:cursor-not-allowed whitespace-nowrap">
              Thanh toán ({selectedItems.length})
            </button>
          </div>
        </div>
      </div>

      {/* Variant Change Modal */}
      {variantModal && (
        <VariantModal
          product={variantModal.product}
          item={variantModal.item}
          currentSkuId={variantModal.item.skuId}
          currentQty={variantModal.item.quantity}
          onConfirm={handleVariantChange}
          onClose={() => setVariantModal(null)}
        />
      )}
    </>
  );
}
