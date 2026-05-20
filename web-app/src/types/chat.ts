export interface ChatAttachment {
  url: string;
  type: string;
  size: number;
}

export interface SendMessageRequest {
  content: string;
  type: string;
  attachments: ChatAttachment[];
  memberIds: string[];
  conversationType: "PRIVATE" | "GROUP";
}

export interface WsMessage<T> {
  traceId: string;
  feature: "CHAT" | "NOTIFICATION" | "PRESENCE";
  action: "SEND" | "SUBSCRIBE" | "UNSUBSCRIBE";
  from: string;
  to: string | null;
  timestamp: number;
  payload: T;
}

export interface ChatMessageResponse {
  messageId?: string;
  conversationId: string;
  senderId: string;
  content: string;
  type?: string;
  createdAt: number;
  editedAt?: number | null;
  deleted?: boolean;
  attachments?: ChatAttachment[];
}

export interface ConversationMember {
  userId: string;
  unreadCount?: number;
  role?: string;
  userRole?: string;
  joinedAt?: string;
  lastReadMessageId?: string;
}

export interface ConversationResponse {
  id: string;
  type: "PRIVATE" | "GROUP";
  createdBy?: string;
  conversationKey?: string;
  createdAt?: string;
  members: ConversationMember[];
  unreadCount?: number;
}
