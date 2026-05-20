export interface CheckoutItemRequest {
  skuId: string;
  quantity: number;
}

export interface UpdateAddressRequest {
  recipientName: string;
  recipientPhone: string;
  address: string;
  ward?: string;
  district?: string;
  city?: string;
}

export interface UpdateQuantityRequest {
  skuId: string;
  quantity: number;
}

export interface UpdateVoucherRequest {
  voucherId?: string;
}

export interface UpdateBuyerNoteRequest {
  buyerNote: string;
}

export interface CreateCheckoutSessionRequest {
  items: CheckoutItemRequest[];
  recipientName: string;
  recipientPhone: string;
  shippingAddress: string;
  shippingWard?: string;
  shippingDistrict?: string;
  shippingCity?: string;
  cartId?: string;
  buyerNotes?: Record<string, string>;
  sellerVoucherID?: Record<string, string>;
  voucherId?: string;
}

export interface CheckoutItemResponse {
  sellerId: string;
  skuId: string;
  productId: string;
  productName: string;
  skuCode?: string;
  imageUrl?: string | null;
  unitPrice: number;
  quantity: number;
  variantInfo?: Record<string, string>;
}

export interface SellerSessionResponse {
  sellerId: string;
  voucherId?: string;
  buyerNote?: string;
  totalAmount: number;
  shippingFee: number;
  discount: number;
  finalAmount: number;
}

export interface ShippingAddressResponse {
  recipientName: string;
  recipientPhone: string;
  address: string;
  ward?: string;
  district?: string;
  city?: string;
  fullAddress?: string;
}

export interface CheckoutSessionResponse {
  sessionId: string;
  items: CheckoutItemResponse[];
  shippingAddress: ShippingAddressResponse;
  sellerSessions: SellerSessionResponse[];
  voucherId?: string;
  cartId?: string;
  totalAmount: number;
  shippingFee: number;
  discount: number;
  finalAmount: number;
  expiredAt: string;
}
