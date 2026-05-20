import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";
import type { ConversationResponse, ChatMessageResponse } from "../types/chat";

const CONVERSATIONS = "/api/v1/conversations";

export async function getMyConversations(userId: string): Promise<ConversationResponse[]> {
  const { data } = await axiosInstance.get<ApiResponse<ConversationResponse[]> | ConversationResponse[]>(CONVERSATIONS, {
    headers: { "X-User-Id": userId },
  });
  // Chat backend currently returns a raw array, while most APIs return wrapped ApiResponse.
  if (Array.isArray(data)) {
    return data;
  }
  return data.result ?? [];
}

export async function resolvePrivateConversation(userId: string, currentUserId: string, userRole?: string, currentUserRole?: string): Promise<ConversationResponse> {
  const { data } = await axiosInstance.get<ApiResponse<ConversationResponse> | ConversationResponse>(
    `${CONVERSATIONS}/resolve`,
    {
      params: { userId, ...(userRole && { userRole }) },
      headers: { 
        "X-User-Id": currentUserId,
        ...(currentUserRole && { "X-User-Role": currentUserRole })
      },
    }
  );
  // Handle both wrapped and unwrapped responses
  if ('id' in data && !('result' in data)) {
    return data as ConversationResponse;
  }
  return (data as ApiResponse<ConversationResponse>).result ?? {};
}

export async function getConversationMessages(
  conversationId: string,
  userId: string,
  page: number = 0,
  size: number = 50,
  before?: number
): Promise<ChatMessageResponse[]> {
  const { data } = await axiosInstance.get<ApiResponse<{ content: ChatMessageResponse[] }>>(
    `${CONVERSATIONS}/${conversationId}/messages`,
    {
      params: { page, size, ...(before && { before }) },
      headers: { "X-User-Id": userId },
    }
  );
  return (data as any).content ?? [];
}
