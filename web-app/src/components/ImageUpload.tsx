import { useRef, useState } from "react";
import { Loader2, Upload, Image as ImageIcon, X } from "lucide-react";
import { uploadFile } from "../api/fileApi";

interface ImageUploadProps {
  /** Label hiển thị phía trên */
  label: string;
  /** URL ảnh hiện tại (hoặc rỗng) */
  value: string;
  /** Callback khi upload xong → trả về URL */
  onChange: (url: string) => void;
  /** Thư mục lưu trên Cloudinary */
  folder?: string;
  /** Kích thước preview */
  size?: "sm" | "md" | "lg";
  /** CSS class bọc ngoài */
  className?: string;
}

export default function ImageUpload({
  label,
  value,
  onChange,
  folder = "general",
  size = "md",
  className = "",
}: ImageUploadProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);

  const handleFile = async (file: File) => {
    if (!file.type.startsWith("image/")) return;
    setUploading(true);
    try {
      const resp = await uploadFile(file, folder);
      const uploaded = resp.data.result;
      onChange(uploaded.secureUrl || uploaded.url);
    } catch (err) {
      console.error("Upload failed:", err);
    } finally {
      setUploading(false);
    }
  };

  const imgSize =
    size === "lg" ? "w-32 h-32" : size === "md" ? "w-20 h-20" : "w-10 h-10";

  return (
    <div className={className}>
      <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">
        <ImageIcon className="w-3 h-3 inline-block mr-1 -mt-0.5" />
        {label}
      </label>

      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={(e) => {
          const f = e.target.files?.[0];
          if (f) handleFile(f);
          e.target.value = "";
        }}
      />

      {value ? (
        <div className="flex items-center gap-3 mt-1">
          <img
            src={value}
            alt=""
            className={`${imgSize} rounded-xl object-cover border border-slate-100`}
            onError={(e) => {
              (e.currentTarget as HTMLImageElement).style.display = "none";
            }}
          />
          <div className="flex flex-col gap-1">
            <button
              type="button"
              onClick={() => inputRef.current?.click()}
              disabled={uploading}
              className="text-xs text-red-600 font-semibold hover:underline disabled:opacity-50 flex items-center gap-1"
            >
              {uploading ? (
                <>
                  <Loader2 className="w-3 h-3 animate-spin" /> Đang tải...
                </>
              ) : (
                "Đổi ảnh"
              )}
            </button>
            <button
              type="button"
              onClick={() => onChange("")}
              className="text-xs text-red-500 font-semibold hover:underline flex items-center gap-1"
            >
              <X className="w-3 h-3" /> Xoá
            </button>
          </div>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          disabled={uploading}
          className="flex items-center gap-1.5 px-3 py-2 mt-1 rounded-xl border-2 border-dashed border-slate-200 bg-slate-50 text-xs font-semibold text-slate-400 hover:border-red-300 hover:text-red-500 transition-colors disabled:opacity-50"
        >
          {uploading ? (
            <Loader2 className="w-3.5 h-3.5 animate-spin" />
          ) : (
            <Upload className="w-3.5 h-3.5" />
          )}
          {uploading ? "Đang tải..." : "Tải ảnh lên"}
        </button>
      )}
    </div>
  );
}
