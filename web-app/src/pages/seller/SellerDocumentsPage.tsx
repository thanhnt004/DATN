import { useEffect, useRef, useState } from "react";
import {
  Loader2, Upload, FileText, CheckCircle2, Clock, XCircle,
  RefreshCw, Plus, X,
} from "lucide-react";
import { getMyDocuments, uploadDocument } from "../../api/sellerDashboardApi";
import { uploadFile } from "../../api/fileApi";
import type { SellerDocumentResponse, DocumentType, DocumentStatus } from "../../types/seller";

// ─── helpers ───────────────────────────────────────────────────────────────────

const DOC_TYPE_LABELS: Record<DocumentType, string> = {
  ID_CARD: "CMND / CCCD",
  PASSPORT: "Hộ chiếu",
  BUSINESS_LICENSE: "Giấy phép kinh doanh",
  TAX_CERTIFICATE: "Giấy chứng nhận thuế",
  BANK_STATEMENT: "Sao kê ngân hàng",
};

const STATUS_CONFIG: Record<DocumentStatus, { label: string; color: string; icon: React.ElementType }> = {
  PENDING:  { label: "Chờ duyệt",  color: "bg-yellow-100 text-yellow-700", icon: Clock },
  APPROVED: { label: "Đã duyệt",   color: "bg-green-100 text-green-700",   icon: CheckCircle2 },
  REJECTED: { label: "Bị từ chối", color: "bg-red-100 text-red-700",       icon: XCircle },
};

function formatDate(d: string | null) {
  if (!d) return "—";
  return new Date(d).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
}

// ─── Upload Modal ──────────────────────────────────────────────────────────────

function UploadDocModal({
  onClose,
  onDone,
}: {
  onClose: () => void;
  onDone: () => void;
}) {
  const [docType, setDocType] = useState<DocumentType>("ID_CARD");
  const [docNumber, setDocNumber] = useState("");
  const [docUrl, setDocUrl] = useState("");
  const [uploading, setUploading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  const handleFileChange = async (file: File | undefined) => {
    if (!file) return;
    setUploading(true);
    setError(null);
    try {
      const resp = await uploadFile(file, "documents");
      const uploaded = resp.data.result;
      setDocUrl(uploaded.secureUrl || uploaded.url);
    } catch {
      setError("Tải file thất bại. Vui lòng thử lại.");
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async () => {
    if (!docUrl.trim()) {
      setError("Vui lòng tải file lên trước");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await uploadDocument({
        documentType: docType,
        documentUrl: docUrl,
        documentNumber: docNumber.trim() || undefined,
      });
      onDone();
    } catch {
      setError("Gửi thất bại. Vui lòng thử lại.");
    } finally {
      setSaving(false);
    }
  };

  const cls = "w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400";

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-full max-w-md shadow-2xl">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
          <h3 className="font-black text-slate-800">Tải lên giấy tờ</h3>
          <button onClick={onClose} className="p-1 rounded-lg hover:bg-slate-100">
            <X className="w-5 h-5 text-slate-400" />
          </button>
        </div>

        <div className="p-5 space-y-4">
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Loại giấy tờ *</label>
            <select className={cls} value={docType} onChange={e => setDocType(e.target.value as DocumentType)}>
              {(Object.entries(DOC_TYPE_LABELS) as [DocumentType, string][]).map(([k, v]) => (
                <option key={k} value={k}>{v}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Số giấy tờ</label>
            <input className={cls} value={docNumber} onChange={e => setDocNumber(e.target.value)} placeholder="VD: 001234567890" />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">File / Hình ảnh *</label>
            <input ref={fileRef} type="file" accept="image/*,.pdf" className="hidden"
              onChange={e => handleFileChange(e.target.files?.[0])} />
            {docUrl ? (
              <div className="flex items-center gap-3 p-3 rounded-xl border border-green-200 bg-green-50">
                <CheckCircle2 className="w-5 h-5 text-green-600 shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-semibold text-green-700 truncate">Đã tải lên thành công</p>
                  <p className="text-xs text-green-500 truncate">{docUrl}</p>
                </div>
                <button onClick={() => { setDocUrl(""); if (fileRef.current) fileRef.current.value = ""; }}
                  className="text-xs text-red-500 font-bold hover:underline shrink-0">Xóa</button>
              </div>
            ) : (
              <button
                onClick={() => fileRef.current?.click()}
                disabled={uploading}
                className="w-full flex items-center justify-center gap-2 py-8 rounded-xl border-2 border-dashed border-slate-200 hover:border-red-300 text-slate-400 hover:text-red-500 transition-colors"
              >
                {uploading ? (
                  <><Loader2 className="w-5 h-5 animate-spin" /><span className="text-sm font-semibold">Đang tải...</span></>
                ) : (
                  <><Upload className="w-5 h-5" /><span className="text-sm font-semibold">Nhấn để chọn file</span></>
                )}
              </button>
            )}
          </div>

          {error && <p className="text-xs text-red-500 font-semibold">{error}</p>}
        </div>

        <div className="px-5 pb-5 flex justify-end gap-2">
          <button onClick={onClose} className="px-4 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50">Hủy</button>
          <button onClick={handleSubmit} disabled={saving || !docUrl}
            className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-60 flex items-center gap-2">
            {saving && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
            Gửi xác minh
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Main page ─────────────────────────────────────────────────────────────────

export default function SellerDocumentsPage() {
  const [docs, setDocs] = useState<SellerDocumentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showUpload, setShowUpload] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const result = await getMyDocuments();
      setDocs(result);
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  return (
    <div className="space-y-5 max-w-3xl mx-auto">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-xl font-black text-slate-900">Giấy tờ xác minh</h1>
          <p className="text-sm text-slate-400 mt-0.5">Tải lên giấy tờ để xác minh tài khoản bán hàng</p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={load} className="p-2 rounded-xl border border-slate-200 hover:bg-slate-50">
            <RefreshCw className="w-4 h-4 text-slate-500" />
          </button>
          <button onClick={() => setShowUpload(true)}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold">
            <Plus className="w-3.5 h-3.5" /> Tải lên giấy tờ
          </button>
        </div>
      </div>

      {/* Info */}
      <div className="bg-red-50 border border-red-200 rounded-2xl p-4">
        <p className="text-sm text-red-700 font-semibold">
          Vui lòng tải lên các giấy tờ cần thiết để hoàn tất xác minh. Giấy tờ sẽ được đội ngũ quản trị viên kiểm duyệt.
        </p>
        <div className="mt-2 flex flex-wrap gap-2">
          {(Object.entries(DOC_TYPE_LABELS) as [DocumentType, string][]).map(([k, v]) => (
            <span key={k} className="px-2 py-0.5 rounded-lg bg-white text-xs font-semibold text-red-600 border border-red-100">{v}</span>
          ))}
        </div>
      </div>

      {/* Document list */}
      {loading ? (
        <div className="flex items-center justify-center py-16 gap-2 text-slate-400">
          <Loader2 className="w-5 h-5 animate-spin" /><span className="text-sm font-semibold">Đang tải...</span>
        </div>
      ) : docs.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-2xl border border-slate-200">
          <FileText className="w-10 h-10 text-slate-300 mx-auto mb-3" />
          <p className="text-sm text-slate-400 font-semibold">Chưa có giấy tờ nào</p>
          <p className="text-xs text-slate-300 mt-1">Nhấn "Tải lên giấy tờ" để bắt đầu</p>
        </div>
      ) : (
        <div className="space-y-3">
          {docs.map(doc => {
            const cfg = STATUS_CONFIG[doc.status];
            const StatusIcon = cfg.icon;
            return (
              <div key={doc.id} className="bg-white rounded-2xl border border-slate-200 shadow-sm p-4 flex items-start gap-4">
                {/* Thumbnail / icon */}
                <div className="w-16 h-16 rounded-xl bg-slate-100 border border-slate-200 flex items-center justify-center overflow-hidden shrink-0">
                  {doc.documentUrl.match(/\.(jpg|jpeg|png|gif|webp)$/i) ? (
                    <img src={doc.documentUrl} alt={doc.documentType} className="w-full h-full object-cover" />
                  ) : (
                    <FileText className="w-6 h-6 text-slate-400" />
                  )}
                </div>

                {/* Info */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <p className="font-bold text-sm text-slate-800">{DOC_TYPE_LABELS[doc.documentType]}</p>
                    <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold flex items-center gap-1 ${cfg.color}`}>
                      <StatusIcon className="w-3 h-3" />
                      {cfg.label}
                    </span>
                  </div>
                  {doc.documentNumber && (
                    <p className="text-xs text-slate-500">Số: <span className="font-mono">{doc.documentNumber}</span></p>
                  )}
                  <div className="flex gap-4 mt-1 text-[11px] text-slate-400">
                    <span>Tải lên: {formatDate(doc.createdAt)}</span>
                    {doc.verifiedAt && <span>Duyệt: {formatDate(doc.verifiedAt)}</span>}
                  </div>
                  {doc.status === "REJECTED" && doc.rejectionReason && (
                    <div className="mt-2 p-2 rounded-lg bg-red-50 border border-red-100">
                      <p className="text-xs text-red-600 font-semibold">Lý do từ chối: {doc.rejectionReason}</p>
                    </div>
                  )}
                </div>

                {/* Link */}
                <a href={doc.documentUrl} target="_blank" rel="noopener noreferrer"
                  className="shrink-0 px-3 py-1.5 rounded-xl border border-slate-200 text-xs font-bold text-slate-500 hover:text-red-600 hover:border-red-300 transition-colors">
                  Xem file
                </a>
              </div>
            );
          })}
        </div>
      )}

      {showUpload && (
        <UploadDocModal
          onClose={() => setShowUpload(false)}
          onDone={() => { setShowUpload(false); load(); }}
        />
      )}
    </div>
  );
}
