import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";
import type { NotificationResponse, NotificationSettingResponse,UpdateNotificationSettingRequest } from "../types/notification";

// ─────────────────────────────────────────────────────────────────────────────
// User Notification API
// ─────────────────────────────────────────────────────────────────────────────

/** GET /api/v1/notifications - Get user's notifications */
export const getMyNotifications = (params?: { page?: number; size?: number; status?: string }) =>
  axiosInstance.get<ApiResponse<NotificationResponse[]>>("/api/v1/notifications", { params });

/** PATCH /api/v1/notifications/:id/read - Mark as read */
export const markAsRead = (id: string) =>
  axiosInstance.patch<ApiResponse<void>>(`/api/v1/notifications/${id}/read`);

/** PATCH /api/v1/notifications/read-all - Mark all as read */
export const markAllAsRead = () =>
  axiosInstance.patch<ApiResponse<void>>("/api/v1/notifications/read-all");
/**User setting */
/**GET /api/v1/notifications/settings - get settings */
export const getMyNotificationSetting = () =>
  axiosInstance.get<ApiResponse<NotificationSettingResponse[]>>("/api/v1/notifications/settings");
/**PUT  /api/v1/notifications/settings - put setting */
export const putNotificationSetting = (data: UpdateNotificationSettingRequest) =>
  axiosInstance.put<ApiResponse<NotificationSettingResponse>>("/api/v1/notifications/settings", data);