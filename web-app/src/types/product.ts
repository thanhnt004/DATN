// ─────────────────────────────────────────────────────────────────────────────
// Product – Enums & Response DTOs
// ─────────────────────────────────────────────────────────────────────────────

export type ProductStatus = "DRAFT" | "PENDING" | "ACTIVE" | "BANNED" | "DELETED";

export interface SpecAttribute {
  name: string;
  value: string;
}

export interface ImageResponse {
  id: string;
  url: string;
  isPrimary: boolean;
  sortOrder: number;
}

export interface SkuAttributeResponse {
  optionName: string;
  valueName: string;
}

export interface SkuResponse {
  id: string;
  skuCode: string;
  price: number;
  originalPrice: number;
  costPrice: number | null;
  status: string;
  weightGram: number | null;
  lengthCm: number | null;
  widthCm: number | null;
  heightCm: number | null;
  attributes: SkuAttributeResponse[];
}

export interface OptionValueResponse {
  id: string;
  value: string;
  imageUrl: string | null;
}

export interface OptionResponse {
  id: string;
  name: string;
  values: OptionValueResponse[];
}

export interface ProductResponse {
  id: string;
  sellerId: string;
  categoryId: string;
  name: string;
  slug: string;
  description: string | null;
  status: ProductStatus;
  ratingAvg: number | null;
  ratingCount: number | null;
  soldCount: number | null;
  minPrice: number;
  maxPrice: number;
  createdAt: string;
  updatedAt: string;
  isDeleted: boolean;
  images: ImageResponse[];
  skus: SkuResponse[];
  options: OptionResponse[];
  specifications: SpecAttribute[];
}

/** Lightweight product summary for listings */
export interface ProductSummaryResponse {
  id: string;
  sellerId: string;
  categoryId: string;
  name: string;
  slug: string;
  status: string;
  minPrice: number;
  maxPrice: number;
  ratingAvg: number | null;
  ratingCount: number | null;
  soldCount: number | null;
  primaryImageUrl: string | null;
}

// ─────────────────────────────────────────────────────────────────────────────
// Product – Request DTOs
// ─────────────────────────────────────────────────────────────────────────────

export interface ImageRequest {
  url: string;
  isPrimary: boolean;
  sortOrder: number;
}

export interface OptionValueRequest {
  value: string;
  imageUrl?: string;
}

export interface OptionRequest {
  name: string;
  values: OptionValueRequest[];
}

export interface SkuRequest {
  skuCode: string;
  price: number;
  originalPrice?: number;
  costPrice?: number;
  weightGram?: number;
  lengthCm?: number;
  widthCm?: number;
  heightCm?: number;
  selectionAttributes: Record<string, string>;
  // Inventory fields (synced to inventory-service on creation)
  totalStock?: number;
  lowStockThreshold?: number;
  locationCode?: string;
}

export interface CreateProductRequest {
  name: string;
  sellerId: string;
  categoryId: string;
  description?: string;
  images: ImageRequest[];
  options: OptionRequest[];
  skus: SkuRequest[];
  specifications: SpecAttribute[];
}

export interface UpdateProductBasicInfoRequest {
  name: string;
  slug?: string;
  description?: string;
  categoryId: string;
}

export interface UpdateProductStatusRequest {
  status: ProductStatus;
}

export interface UpdateSingleSkuRequest {
  price?: number;
  originalPrice?: number;
  costPrice?: number;
  weightGram?: number;
  dimensions?: {
    lengthCm?: number;
    widthCm?: number;
    heightCm?: number;
  };
  status?: string;
}

export interface ProductPageParams {
  sellerId?: string;
  status?: ProductStatus | "";
  keyword?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: "ASC" | "DESC";
}
