// ─────────────────────────────────────────────────────────────────────────────
// Notification – Enums & Response DTOs
// ─────────────────────────────────────────────────────────────────────────────

export type NotificationStatus =
  | "CREATED"
  | "PROCESSING"
  | "SENT"
  | "FAILED"
  | "CANCELLED"
  | "READ";

export type NotificationPriority = "HIGH" | "NORMAL" | "LOW";
export type ChannelType = "EMAIL" | "SMS" | "PUSH";

export interface NotificationResponse {
  id: string;
  userId: string;
  notificationType: string;
  payload: Record<string, unknown>;
  priority: NotificationPriority;
  status: NotificationStatus;
  referenceId: string | null;
  createdAt: string;
}
export interface NotificationSettingResponse {
  notificationType: string;
  channelType: ChannelType;
  enabled: boolean;
  updatedAt: string;
 }
export interface UpdateNotificationSettingRequest {
  notificationType: string;
  channelType: ChannelType;
  enabled: boolean;
}

