// ─────────────────────────────────────────────────────────────────────────────
// File Upload – Response DTOs
// ─────────────────────────────────────────────────────────────────────────────

export interface FileUploadResponse {
  publicId: string;
  url: string;
  secureUrl: string;
  format: string;
  resourceType: string;
  bytes: number;
  width: number | null;
  height: number | null;
  folder: string;
  originalFilename: string;
}
