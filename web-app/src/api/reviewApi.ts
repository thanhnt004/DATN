import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";
import type {
  ReviewResponse,
  ReviewSummaryResponse,
  ReviewPageResponse,
  CreateReviewRequest,
  UpdateReviewRequest,
  CreateReplyRequest,
  ReplyResponse,
} from "../types/review";

// ─────────────────────────────────────────────────────────────────────────────
// Public / Buyer Review API
// ─────────────────────────────────────────────────────────────────────────────

/** GET /api/v1/reviews/product/:productId — Get product reviews (public) */
export const getProductReviews = (
  productId: string,
  params?: { rating?: number; hasComment?: boolean; hasImages?: boolean; page?: number; size?: number }
) =>
  axiosInstance.get<ApiResponse<ReviewPageResponse>>(
    `/api/v1/reviews/product/${productId}`,
    { params }
  );

/** GET /api/v1/reviews/product/:productId/summary — Get review summary (public) */
export const getReviewSummary = (productId: string) =>
  axiosInstance.get<ApiResponse<ReviewSummaryResponse>>(
    `/api/v1/reviews/product/${productId}/summary`
  );

/** POST /api/v1/reviews — Create a review */
export const createReview = (data: CreateReviewRequest) =>
  axiosInstance.post<ApiResponse<ReviewResponse>>("/api/v1/reviews", data);

/** PUT /api/v1/reviews/:id — Update a review */
export const updateReview = (reviewId: string, data: UpdateReviewRequest) =>
  axiosInstance.put<ApiResponse<ReviewResponse>>(`/api/v1/reviews/${reviewId}`, data);

/** DELETE /api/v1/reviews/:id — Delete a review */
export const deleteReview = (reviewId: string) =>
  axiosInstance.delete<ApiResponse<void>>(`/api/v1/reviews/${reviewId}`);

/** GET /api/v1/reviews/my — Get current user's reviews */
export const getMyReviews = (params?: { page?: number; size?: number }) =>
  axiosInstance.get<ApiResponse<ReviewPageResponse>>("/api/v1/reviews/my", { params });

/** GET /api/v1/reviews/order/:orderId — Get reviews for an order */
export const getOrderReviews = (orderId: string) =>
  axiosInstance.get<ApiResponse<ReviewResponse[]>>(`/api/v1/reviews/order/${orderId}`);

/** GET /api/v1/reviews/check — Check if user has reviewed a product for an order */
export const checkHasReviewed = (productId: string, orderId: string) =>
  axiosInstance.get<ApiResponse<boolean>>("/api/v1/reviews/check", {
    params: { productId, orderId },
  });

// ─────────────────────────────────────────────────────────────────────────────
// Seller Review API (replies)
// ─────────────────────────────────────────────────────────────────────────────

/** POST /api/v1/seller/reviews/:reviewId/reply — Reply to a review */
export const createReply = (reviewId: string, data: CreateReplyRequest) =>
  axiosInstance.post<ApiResponse<ReplyResponse>>(
    `/api/v1/seller/reviews/${reviewId}/reply`,
    data
  );

/** PUT /api/v1/seller/reviews/replies/:replyId — Update reply */
export const updateReply = (replyId: string, data: CreateReplyRequest) =>
  axiosInstance.put<ApiResponse<ReplyResponse>>(
    `/api/v1/seller/reviews/replies/${replyId}`,
    data
  );

/** DELETE /api/v1/seller/reviews/replies/:replyId — Delete reply */
export const deleteReply = (replyId: string) =>
  axiosInstance.delete<ApiResponse<void>>(
    `/api/v1/seller/reviews/replies/${replyId}`
  );
