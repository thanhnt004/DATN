// ─────────────────────────────────────────────────────────────────────────────
// Search – Response & Request DTOs
// ─────────────────────────────────────────────────────────────────────────────

export interface SearchProductResponse {
  id: string;
  name: string;
  slug: string;
  thumbnailUrl: string | null;
  minPrice: number;
  maxPrice: number;
  ratingAvg: number | null;
  ratingCount: number | null;
  soldCount: number | null;
  categoryId: string | null;
  sellerId: string | null;
  status: string;
  createdAt: string;
}

export interface SearchParams {
  keyword?: string;
  categoryId?: string;
  categoryIds?: string;
  sellerId?: string;
  minPrice?: number;
  maxPrice?: number;
  minRating?: number;
  status?: string;
  sortBy?: string; // "relevance" | "price_asc" | "price_desc" | "newest" | "best_selling"
  page?: number;
  size?: number;
}

export interface RelatedCategoryResponse {
  categoryId: string;
  count: number;
}
