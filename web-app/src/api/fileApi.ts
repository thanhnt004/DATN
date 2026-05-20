import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";
import type { FileUploadResponse } from "../types/file";

// ─────────────────────────────────────────────────────────────────────────────
// File Upload API
// ─────────────────────────────────────────────────────────────────────────────

/** POST /api/v1/files/upload - Upload single file */
export const uploadFile = (file: File, folder = "general") => {
  const formData = new FormData();
  formData.append("file", file);
  return axiosInstance.post<ApiResponse<FileUploadResponse>>(
    `/api/v1/files/upload?folder=${encodeURIComponent(folder)}`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } }
  );
};

/** POST /api/v1/files/upload/batch - Upload multiple files */
export const uploadFiles = (files: File[], folder = "general") => {
  const formData = new FormData();
  files.forEach((f) => formData.append("files", f));
  return axiosInstance.post<ApiResponse<FileUploadResponse[]>>(
    `/api/v1/files/upload/batch?folder=${encodeURIComponent(folder)}`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } }
  );
};

/** DELETE /api/v1/files?publicId= - Delete single file */
export const deleteFile = (publicId: string) =>
  axiosInstance.delete<ApiResponse<void>>(`/api/v1/files`, { params: { publicId } });

/** DELETE /api/v1/files/batch - Delete multiple files */
export const deleteFiles = (publicIds: string[]) =>
  axiosInstance.delete<ApiResponse<void>>("/api/v1/files/batch", { data: publicIds });
