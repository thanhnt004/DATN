import { useState, useRef, useCallback } from "react";
import ReactCrop, { type Crop, type PixelCrop } from "react-image-crop";
import "react-image-crop/dist/ReactCrop.css";
import { X, Check, RotateCcw } from "lucide-react";

/* ────────────────────────────────────────────────────────────────────── */
/*  Props                                                                */
/* ────────────────────────────────────────────────────────────────────── */

export interface ImageCropperProps {
  /** The File selected by user */
  file: File;
  /** Locked aspect ratio (e.g. 16/9, 2/1, 4/3). */
  aspect?: number;
  /** Label displayed at the top */
  title?: string;
  /** Called with the cropped Blob + preview URL */
  onConfirm: (croppedBlob: Blob, previewUrl: string) => void;
  /** Called when user cancels */
  onCancel: () => void;
}

/* ────────────────────────────────────────────────────────────────────── */
/*  Helpers                                                              */
/* ────────────────────────────────────────────────────────────────────── */

function getCroppedBlob(
  image: HTMLImageElement,
  crop: PixelCrop,
  fileName: string,
): Promise<Blob> {
  const canvas = document.createElement("canvas");
  const scaleX = image.naturalWidth / image.width;
  const scaleY = image.naturalHeight / image.height;

  canvas.width = Math.floor(crop.width * scaleX);
  canvas.height = Math.floor(crop.height * scaleY);

  const ctx = canvas.getContext("2d");
  if (!ctx) return Promise.reject(new Error("Canvas not supported"));

  ctx.imageSmoothingQuality = "high";
  ctx.drawImage(
    image,
    crop.x * scaleX,
    crop.y * scaleY,
    crop.width * scaleX,
    crop.height * scaleY,
    0,
    0,
    canvas.width,
    canvas.height,
  );

  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (blob) {
          // Preserve original name
          Object.defineProperty(blob, "name", { value: fileName, writable: false });
          resolve(blob);
        } else {
          reject(new Error("Canvas toBlob failed"));
        }
      },
      "image/jpeg",
      0.92,
    );
  });
}

/* ────────────────────────────────────────────────────────────────────── */
/*  Component                                                            */
/* ────────────────────────────────────────────────────────────────────── */

export default function ImageCropper({ file, aspect, title = "Cắt ảnh", onConfirm, onCancel }: ImageCropperProps) {
  const [imgSrc, setImgSrc] = useState<string>("");
  const imgRef = useRef<HTMLImageElement>(null);
  const [crop, setCrop] = useState<Crop>();
  const [completedCrop, setCompletedCrop] = useState<PixelCrop>();
  const [currentAspect] = useState<number | undefined>(aspect);
  const [processing, setProcessing] = useState(false);

  // Read file once
  useState(() => {
    const reader = new FileReader();
    reader.onload = () => setImgSrc(reader.result as string);
    reader.readAsDataURL(file);
  });

  const onImageLoad = useCallback(
    (e: React.SyntheticEvent<HTMLImageElement>) => {
      const { width, height } = e.currentTarget;
      // Default crop: center, 80% of dimension, bound to aspect
      const a = currentAspect;
      let cropW = width * 0.8;
      let cropH = height * 0.8;
      if (a) {
        if (cropW / cropH > a) cropW = cropH * a;
        else cropH = cropW / a;
      }
      setCrop({
        unit: "px",
        x: (width - cropW) / 2,
        y: (height - cropH) / 2,
        width: cropW,
        height: cropH,
      });
    },
    [currentAspect],
  );

  const handleConfirm = async () => {
    if (!completedCrop || !imgRef.current) return;
    setProcessing(true);
    try {
      const blob = await getCroppedBlob(imgRef.current, completedCrop, file.name);
      const previewUrl = URL.createObjectURL(blob);
      onConfirm(blob, previewUrl);
    } catch (err) {
      console.error("Crop failed:", err);
    } finally {
      setProcessing(false);
    }
  };

  const resetCrop = () => {
    setCrop(undefined);
    setCompletedCrop(undefined);
    // Trigger re-crop by invoking onImageLoad logic
    if (imgRef.current) {
      const { width, height } = imgRef.current;
      const a = currentAspect;
      let cropW = width * 0.8;
      let cropH = height * 0.8;
      if (a) {
        if (cropW / cropH > a) cropW = cropH * a;
        else cropH = cropW / a;
      }
      setCrop({ unit: "px", x: (width - cropW) / 2, y: (height - cropH) / 2, width: cropW, height: cropH });
    }
  };

  const ratioText = (() => {
    if (!currentAspect) return "Tự do";
    if (Math.abs(currentAspect - 16 / 9) < 0.01) return "16:9";
    if (Math.abs(currentAspect - 3 / 1) < 0.01) return "3:1";
    if (Math.abs(currentAspect - 4 / 3) < 0.01) return "4:3";
    if (Math.abs(currentAspect - 1) < 0.01) return "1:1";
    return `${Math.round(currentAspect * 100) / 100}:1`;
  })();

  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-full max-w-2xl shadow-2xl max-h-[95vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-3 border-b border-slate-100">
          <h3 className="font-black text-slate-800 text-sm">{title}</h3>
          <button onClick={onCancel} className="p-1 rounded-lg hover:bg-slate-100">
            <X className="w-5 h-5 text-slate-400" />
          </button>
        </div>

        {/* Aspect ratio info */}
        <div className="px-5 py-2 border-b border-slate-50 flex items-center gap-2 flex-wrap">
          <span className="text-xs font-bold text-slate-400 uppercase tracking-wider mr-1">Tỉ lệ:</span>
          <span className="px-3 py-1 rounded-lg text-xs font-bold bg-slate-100 text-slate-600">
            Tự động ({ratioText})
          </span>
        </div>

        {/* Crop area */}
        <div className="flex-1 overflow-auto p-4 flex items-center justify-center bg-slate-50/50 min-h-0">
          {imgSrc ? (
            <ReactCrop
              crop={crop}
              onChange={(c) => setCrop(c)}
              onComplete={(c) => setCompletedCrop(c)}
              aspect={currentAspect}
              className="max-h-[60vh]"
            >
              <img
                ref={imgRef}
                src={imgSrc}
                alt="Crop preview"
                onLoad={onImageLoad}
                className="max-h-[60vh] max-w-full"
                style={{ display: "block" }}
              />
            </ReactCrop>
          ) : (
            <div className="text-slate-400 text-sm">Đang tải ảnh...</div>
          )}
        </div>

        {/* Preview info */}
        {completedCrop && (
          <div className="px-5 py-2 border-t border-slate-100 text-xs text-slate-400">
            Kích thước crop: {Math.round(completedCrop.width * (imgRef.current ? imgRef.current.naturalWidth / imgRef.current.width : 1))}
            {" × "}
            {Math.round(completedCrop.height * (imgRef.current ? imgRef.current.naturalHeight / imgRef.current.height : 1))} px
          </div>
        )}

        {/* Footer */}
        <div className="px-5 py-3 border-t border-slate-100 flex items-center justify-between">
          <button
            onClick={resetCrop}
            className="flex items-center gap-1.5 text-xs font-bold text-slate-500 hover:text-red-600 transition-colors"
          >
            <RotateCcw className="w-3.5 h-3.5" /> Đặt lại
          </button>
          <div className="flex gap-2">
            <button
              onClick={onCancel}
              className="px-4 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50"
            >
              Hủy
            </button>
            <button
              onClick={handleConfirm}
              disabled={!completedCrop || processing}
              className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-50 flex items-center gap-2"
            >
              {processing ? (
                <span className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <Check className="w-3.5 h-3.5" />
              )}
              Xác nhận
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
