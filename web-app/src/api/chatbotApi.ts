/**
 * chatbotApi.ts
 *
 * Chatbot endpoints:
 *   POST /api/v1/chatbot/chat         (JWT required)
 *   DELETE /api/v1/chatbot/sessions/{sessionId}
 */
import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";

// ── Types ────────────────────────────────────────────────────────────────────

export interface ChatRequest {
  message: string;
  sessionId?: string;
}

export interface ProductInfo {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  categoryName: string | null;
  imageUrl: string | null;
  price: number;
  originalPrice: number | null;
  ratingAvg: number | null;
  ratingCount: number | null;
  soldCount: number | null;
}

export interface ChatResponse {
  sessionId: string;
  reply: string;
  suggestedProducts: ProductInfo[] | null;
}

// ── API calls ────────────────────────────────────────────────────────────────

export async function sendChatMessage(req: ChatRequest): Promise<ChatResponse> {
  const { data } = await axiosInstance.post<ApiResponse<ChatResponse>>(
    "/api/v1/chatbot/chat",
    req,
  );
  return data.result;
}

export async function deleteChatSession(sessionId: string): Promise<void> {
  await axiosInstance.delete(`/api/v1/chatbot/sessions/${sessionId}`);
}
