// ─────────────────────────────────────────────────────────────────────────────
// Cart – Response DTOs
// ─────────────────────────────────────────────────────────────────────────────

export interface CartSummary {
  totalItems: number;
  totalQuantity: number;
  selectedItems: number;
  selectedQuantity: number;
  subtotal: number;
  selectedSubtotal: number;
}

export interface CartItemResponse {
  id: string;
  skuId: string;
  productId: string;
  sellerId: string;
  quantity: number;
  price: number;
  subtotal: number;
  selected: boolean;
  createdAt: string;
  productName: string;
  skuCode: string;
  imageUrl: string | null;
  sellerName: string | null;
  availableStock: number;
  inStock: boolean;
  attributes?: Record<string, string>; // variant attributes, e.g. {"Color": "Red"}
}

export interface CartResponse {
  id: string;
  userId: string;
  items: CartItemResponse[];
  summary: CartSummary;
  updatedAt: string;
}

export interface CartBySellerResponse {
  sellerId: string;
  sellerName: string;
  items: CartItemResponse[];
  subtotal: number;
  itemCount: number;
  allSelected: boolean;
}

export interface SavedItemResponse {
  id: string;
  skuId: string;
  productId: string;
  createdAt: string;
  productName: string;
  imageUrl: string | null;
  price: string;
}

// ─────────────────────────────────────────────────────────────────────────────
// Cart – Request DTOs
// ─────────────────────────────────────────────────────────────────────────────

export interface AddToCartRequest {
  skuId: string;
  productId: string;
  sellerId: string;
  quantity: number;
}

export interface UpdateCartItemRequest {
  quantity: number;
}

export interface UpdateSelectionRequest {
  selected: boolean;
  skuIds?: string[];
  sellerId?: string;
}

export interface SaveForLaterRequest {
  skuId: string;
  productId: string;
}

export interface RemoveItemsRequest {
  skuIds: string[];
}
