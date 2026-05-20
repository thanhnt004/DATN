// ═══════════════════════════════════════════════════════════════════
// Review Types
// ═══════════════════════════════════════════════════════════════════

export interface ReviewResponse {
  id: string;
  productId: string;
  userId: string;
  orderId: string;
  skuId: string | null;
  rating: number;
  comment: string | null;
  images: string[];
  anonymous: boolean;
  createdAt: string;
  updatedAt: string;
  userName: string | null;
  userAvatar: string | null;
  reply: ReplyResponse | null;
}

export interface ReplyResponse {
  id: string;
  sellerId: string;
  comment: string;
  createdAt: string;
  updatedAt: string;
}

export interface ReviewSummaryResponse {
  ratingAvg: number;
  totalCount: number;
  ratingDistribution: Record<number, number>; // { 5: 10, 4: 5, 3: 2, 2: 1, 1: 0 }
  withCommentCount: number;
  withImagesCount: number;
}

export interface ReviewPageResponse {
  content: ReviewResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateReviewRequest {
  productId: string;
  orderId: string;
  skuId?: string;
  rating: number;
  comment?: string;
  images?: string[];
  anonymous?: boolean;
}

export interface UpdateReviewRequest {
  rating?: number;
  comment?: string;
  images?: string[];
  isAnonymous?: boolean;
}

export interface CreateReplyRequest {
  comment: string;
}
