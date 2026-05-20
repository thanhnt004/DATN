import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";
import type {
  CartResponse,
  CartBySellerResponse,
  CartItemResponse,
  SavedItemResponse,
  AddToCartRequest,
  UpdateCartItemRequest,
  UpdateSelectionRequest,
  SaveForLaterRequest,
  RemoveItemsRequest,
} from "../types/cart";

// ─────────────────────────────────────────────────────────────────────────────
// Cart Items
// ─────────────────────────────────────────────────────────────────────────────

/** GET /api/v1/cart - Get current user's cart */
export const getCart = () =>
  axiosInstance.get<ApiResponse<CartResponse>>("/api/v1/cart");

/** GET /api/v1/cart/by-seller - Get cart grouped by seller */
export const getCartBySeller = () =>
  axiosInstance.get<ApiResponse<CartBySellerResponse[]>>("/api/v1/cart/by-seller");

/** POST /api/v1/cart/items - Add item to cart */
export const addToCart = (data: AddToCartRequest) =>
  axiosInstance.post<ApiResponse<CartItemResponse>>("/api/v1/cart/items", data);

/** PUT /api/v1/cart/items/:skuId - Update item quantity */
export const updateCartItem = (skuId: string, data: UpdateCartItemRequest) =>
  axiosInstance.put<ApiResponse<CartItemResponse>>(`/api/v1/cart/items/${skuId}`, data);

/** DELETE /api/v1/cart/items/:skuId - Remove single item */
export const removeCartItem = (skuId: string) =>
  axiosInstance.delete<ApiResponse<void>>(`/api/v1/cart/items/${skuId}`);

/** DELETE /api/v1/cart/items - Remove multiple items */
export const removeCartItems = (data: RemoveItemsRequest) =>
  axiosInstance.delete<ApiResponse<void>>("/api/v1/cart/items", { data });

/** DELETE /api/v1/cart - Clear entire cart */
export const clearCart = () =>
  axiosInstance.delete<ApiResponse<void>>("/api/v1/cart");

// ─────────────────────────────────────────────────────────────────────────────
// Selection
// ─────────────────────────────────────────────────────────────────────────────

/** PATCH /api/v1/cart/selection - Update item selection */
export const updateSelection = (data: UpdateSelectionRequest) =>
  axiosInstance.patch<ApiResponse<CartResponse>>("/api/v1/cart/selection", data);

/** POST /api/v1/cart/select-all or deselect-all */
export const selectAll = (selected: boolean) =>
  axiosInstance.post<ApiResponse<CartResponse>>(
    selected ? "/api/v1/cart/select-all" : "/api/v1/cart/deselect-all"
  );

// ─────────────────────────────────────────────────────────────────────────────
// Saved Items (Wishlist)
// ─────────────────────────────────────────────────────────────────────────────

/** GET /api/v1/cart/saved - Get saved items */
export const getSavedItems = () =>
  axiosInstance.get<ApiResponse<SavedItemResponse[]>>("/api/v1/cart/saved");

/** POST /api/v1/cart/saved - Save item for later */
export const saveForLater = (data: SaveForLaterRequest) =>
  axiosInstance.post<ApiResponse<SavedItemResponse>>("/api/v1/cart/saved", data);

/** DELETE /api/v1/cart/saved/:skuId - Remove saved item */
export const removeSavedItem = (skuId: string) =>
  axiosInstance.delete<ApiResponse<void>>(`/api/v1/cart/saved/${skuId}`);

/** POST /api/v1/cart/saved/:skuId/move-to-cart - Move saved back to cart */
export const moveToCart = (skuId: string) =>
  axiosInstance.post<ApiResponse<CartItemResponse>>(`/api/v1/cart/saved/${skuId}/move-to-cart`);

// ─────────────────────────────────────────────────────────────────────────────
// Checkout Preview
// ─────────────────────────────────────────────────────────────────────────────

/** GET /api/v1/cart/checkout-preview/by-seller - Preview checkout grouped by seller */
export const getCheckoutPreview = () =>
  axiosInstance.get<ApiResponse<CartBySellerResponse[]>>("/api/v1/cart/checkout-preview/by-seller");

/** GET /api/v1/cart/count - Get cart item count */
export const getCartCount = () =>
  axiosInstance.get<ApiResponse<number>>("/api/v1/cart/count");
