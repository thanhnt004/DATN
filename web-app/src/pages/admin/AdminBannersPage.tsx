import { useEffect, useState, useRef } from "react";
import {
  Loader2,
  Plus,
  Pencil,
  Trash2,
  ImageOff,
  RefreshCw,
  ToggleLeft,
  ToggleRight,
  ChevronLeft,
  ChevronRight,
  X,
  Upload,
  Image as ImageIcon,
} from "lucide-react";
import { uploadFile } from "../../api/fileApi";
import ImageCropper from "../../components/ImageCropper";
import {
  adminListBanners,
  adminCreateBanner,
  adminUpdateBanner,
  adminDeleteBanner,
  adminUpdateBannerStatus,
  adminListBannerPositions,
} from "../../api/adminApi";
import type {
  BannerResponse,
  BannerStatus,
  BannerPositionResponse,
  CreateBannerRequest,
  UpdateBannerRequest,
  PageResponse,
} from "../../types/admin";

// ─── helpers ───────────────────────────────────────────────────────────────────

const STATUSES: { value: BannerStatus; label: string }[] = [
  { value: "ACTIVE",   label: "Hiển thị" },
  { value: "INACTIVE", label: "Ẩn" },
];

function StatusBadge({ status }: { status: BannerStatus }) {
  return (
    <span className={`px-2 py-0.5 rounded-full text-[11px] font-bold ${
      status === "ACTIVE" ? "bg-green-100 text-green-700" : "bg-slate-100 text-slate-500"
    }`}>
      {status === "ACTIVE" ? "Hiển thị" : "Ẩn"}
    </span>
  );
}

function formatDate(iso: string | null) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("vi-VN");
}

// ─── Banner form ───────────────────────────────────────────────────────────────

type FormMode = "create" | "edit";

const EMPTY_FORM: CreateBannerRequest = {
  title: "", imageUrl: "", linkUrl: "", linkType: "NONE",
  linkValue: "", positionCode: "", sortOrder: 0, status: "ACTIVE",
  startDate: "", endDate: "", targetAudience: "ALL",
};

const BANNER_POSITION_ASPECT: Record<string, number> = {
  HOME_HERO: 16 / 9,
  FLASH_SALE: 3 / 1,
  HOME_MID: 4 / 3,
  HOME_BOTTOM: 4 / 3,
  SIDEBAR: 4 / 3,
  CATEGORY_TOP: 3 / 1,
  POPUP: 1 / 1,
};

const getAspectByPositionCode = (positionCode: string) => BANNER_POSITION_ASPECT[positionCode] ?? 16 / 9;

interface BannerFormModalProps {
  mode: FormMode;
  initial?: BannerResponse;
  positions: BannerPositionResponse[];
  onClose: () => void;
  onSave: (data: CreateBannerRequest | UpdateBannerRequest, id?: string) => Promise<void>;
}

function BannerFormModal({ mode, initial, positions, onClose, onSave }: BannerFormModalProps) {
  const [form, setForm] = useState<CreateBannerRequest>(() =>
    initial
      ? {
          title: initial.title, imageUrl: initial.imageUrl, linkUrl: initial.linkUrl ?? "",
          linkType: initial.linkType ?? "NONE", linkValue: initial.linkValue ?? "",
          positionCode: initial.positionCode, sortOrder: initial.sortOrder,
          status: initial.status,
          startDate: initial.startDate ? initial.startDate.substring(0, 10) : "",
          endDate: initial.endDate ? initial.endDate.substring(0, 10) : "",
          targetAudience: initial.targetAudience,
        }
      : EMPTY_FORM
  );
  const [saving, setSaving] = useState(false);
  const [error, setError]  = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [cropFile, setCropFile] = useState<File | null>(null);
  const cropAspect = getAspectByPositionCode(form.positionCode);

  const set = (k: keyof CreateBannerRequest, v: string | number) =>
    setForm(f => ({ ...f, [k]: v }));

  const handleFileSelect = (file: File) => {
    if (!file.type.startsWith("image/")) return;
    if (!form.positionCode) {
      setError("Vui lòng chọn vị trí banner trước khi tải ảnh để hệ thống tự áp tỉ lệ phù hợp.");
      return;
    }
    setError(null);
    setCropFile(file);
  };

  const handleCropConfirm = async (croppedBlob: Blob) => {
    setCropFile(null);
    setUploading(true);
    try {
      const fileName = (croppedBlob as File).name || "banner.jpg";
      const croppedFile = new File([croppedBlob], fileName, { type: croppedBlob.type });
      const resp = await uploadFile(croppedFile, "banners");
      const uploaded = resp.data.result;
      set("imageUrl", uploaded.secureUrl || uploaded.url);
    } catch (err) {
      console.error("Upload failed:", err);
      setError("Tải ảnh thất bại. Vui lòng thử lại.");
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async () => {
    if (!form.title.trim() || !form.imageUrl.trim() || !form.positionCode.trim()) {
      setError("Vui lòng điền tiêu đề, tải ảnh lên và chọn vị trí hiển thị"); return;
    }
    setSaving(true); setError(null);
    try {
      await onSave(form, initial?.id);
    } catch {
      setError("Lưu thất bại. Vui lòng thử lại.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-full max-w-lg shadow-2xl max-h-[90vh] flex flex-col">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
          <h3 className="font-black text-slate-800">{mode === "create" ? "Tạo banner mới" : "Chỉnh sửa banner"}</h3>
          <button onClick={onClose} className="p-1 rounded-lg hover:bg-slate-100"><X className="w-5 h-5 text-slate-400" /></button>
        </div>

        <div className="p-6 space-y-4 overflow-y-auto">
          {/* Title */}
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">
              Tiêu đề <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              className="w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400"
              value={form.title}
              onChange={e => set("title", e.target.value)}
              placeholder="VD: Khuyến mãi mùa hè..."
            />
          </div>

          {/* Image Upload */}
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">
              <ImageIcon className="w-3 h-3 inline-block mr-1 -mt-0.5" />
              Hình ảnh banner <span className="text-red-500">*</span>
            </label>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              className="hidden"
              onChange={e => {
                const f = e.target.files?.[0];
                if (f) handleFileSelect(f);
                e.target.value = "";
              }}
            />
            <p className="text-[11px] text-slate-400 mb-2">
              Tỉ lệ cắt ảnh được tự động áp dụng theo vị trí banner đã chọn.
            </p>
            {form.imageUrl ? (
              <div className="space-y-2">
                <div className="h-36 rounded-xl overflow-hidden bg-slate-100 border border-slate-200">
                  <img src={form.imageUrl} alt="preview" className="w-full h-full object-cover" onError={e => e.currentTarget.style.display = "none"} />
                </div>
                <div className="flex items-center gap-3">
                  <button
                    type="button"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={uploading}
                    className="text-xs text-red-600 font-semibold hover:underline disabled:opacity-50 flex items-center gap-1"
                  >
                    {uploading ? <Loader2 className="w-3 h-3 animate-spin" /> : <Upload className="w-3 h-3" />}
                    {uploading ? "Đang tải..." : "Đổi ảnh"}
                  </button>
                  <button
                    type="button"
                    onClick={() => set("imageUrl", "")}
                    className="text-xs text-red-500 font-semibold hover:underline"
                  >
                    Xoá ảnh
                  </button>
                </div>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={uploading}
                className="flex items-center gap-1.5 px-4 py-3 mt-1 w-full justify-center rounded-xl border-2 border-dashed border-slate-200 bg-slate-50 text-xs font-semibold text-slate-400 hover:border-red-300 hover:text-red-500 transition-colors disabled:opacity-50"
              >
                {uploading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Upload className="w-4 h-4" />}
                {uploading ? "Đang tải ảnh lên..." : "Nhấn để tải ảnh banner lên"}
              </button>
            )}
          </div>

          {/* Link fields */}
          {[
            { key: "linkUrl"  as const, label: "URL liên kết", req: false },
            { key: "linkValue"as const, label: "Giá trị liên kết (ID sản phẩm / danh mục...)", req: false },
          ].map(({ key, label, req }) => (
            <div key={key}>
              <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">
                {label} {req && <span className="text-red-500">*</span>}
              </label>
              <input
                type="text"
                className="w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400"
                value={form[key] as string}
                onChange={e => set(key, e.target.value)}
              />
            </div>
          ))}

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Loại liên kết</label>
              <select className="w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400"
                value={form.linkType} onChange={e => set("linkType", e.target.value)}>
                {["NONE","URL","PRODUCT","CATEGORY","CAMPAIGN","SELLER"].map(v => (
                  <option key={v} value={v}>{v}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Vị trí <span className="text-red-500">*</span></label>
              <select className="w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400"
                value={form.positionCode} onChange={e => set("positionCode", e.target.value)}>
                <option value="">-- Chọn --</option>
                {positions.map(p => <option key={p.code} value={p.code}>{p.name}</option>)}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Thứ tự</label>
              <input type="number" className="w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400"
                value={form.sortOrder} onChange={e => { e.target.value = e.target.value.replace(/^0+(?=\d)/, ''); set("sortOrder", +e.target.value); }} />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Đối tượng</label>
              <select className="w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400"
                value={form.targetAudience} onChange={e => set("targetAudience", e.target.value)}>
                {["ALL","NEW_USER","RETURNING_USER","VIP"].map(v => <option key={v} value={v}>{v}</option>)}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Ngày bắt đầu</label>
              <input type="date" className="w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400"
                value={form.startDate ?? ""} onChange={e => set("startDate", e.target.value)} />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Ngày kết thúc</label>
              <input type="date" className="w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400"
                value={form.endDate ?? ""} onChange={e => set("endDate", e.target.value)} />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Trạng thái</label>
            <div className="flex gap-2">
              {STATUSES.map(({ value, label }) => (
                <button key={value} onClick={() => set("status", value)}
                  className={`flex-1 py-2 rounded-xl text-sm font-bold border-2 transition-all ${
                    form.status === value ? "border-red-500 bg-red-50 text-red-700" : "border-slate-100 text-slate-500"
                  }`}>
                  {label}
                </button>
              ))}
            </div>
          </div>

          {error && <p className="text-xs text-red-500 font-semibold">{error}</p>}
        </div>

        {/* Image Cropper Modal */}
        {cropFile && (
          <ImageCropper
            file={cropFile}
            title="Cắt ảnh banner"
            aspect={cropAspect}
            onConfirm={handleCropConfirm}
            onCancel={() => setCropFile(null)}
          />
        )}

        <div className="px-6 pb-6 flex justify-end gap-2">
          <button onClick={onClose} className="px-4 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50">Hủy</button>
          <button onClick={handleSubmit} disabled={saving}
            className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-60 flex items-center gap-2">
            {saving && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
            Lưu
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Delete confirm ────────────────────────────────────────────────────────────

function DeleteModal({ banner, onClose, onDelete }: { banner: BannerResponse; onClose: () => void; onDelete: () => Promise<void> }) {
  const [loading, setLoading] = useState(false);
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-full max-w-sm shadow-2xl p-6">
        <h3 className="font-black text-slate-800 mb-2">Xoá banner</h3>
        <p className="text-sm text-slate-500 mb-6">Bạn có chắc muốn xoá banner <strong>"{banner.title}"</strong>? Thao tác này không thể hoàn tác.</p>
        <div className="flex gap-2 justify-end">
          <button onClick={onClose} className="px-4 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50">Hủy</button>
          <button disabled={loading} onClick={async () => { setLoading(true); await onDelete(); }}
            className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-60 flex items-center gap-2">
            {loading && <Loader2 className="w-3.5 h-3.5 animate-spin" />}Xoá
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Main page ─────────────────────────────────────────────────────────────────

export default function AdminBannersPage() {
  const [page, setPage]             = useState<PageResponse<BannerResponse> | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<BannerStatus | "">("");
  const [positions, setPositions]   = useState<BannerPositionResponse[]>([]);
  const [loading, setLoading]       = useState(false);
  const [modal, setModal]           = useState<"create" | "edit" | "delete" | null>(null);
  const [selected, setSelected]     = useState<BannerResponse | null>(null);
  const [toggling, setToggling]     = useState<string | null>(null);

  const load = async (p = 0, sf = statusFilter) => {
    setLoading(true);
    try {
      const result = await adminListBanners({ status: sf || undefined, page: p, size: 12 });
      setPage(result); setCurrentPage(p);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(0);
    adminListBannerPositions().then(setPositions).catch(() => {});
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSave = async (data: CreateBannerRequest | UpdateBannerRequest, id?: string) => {
    const payload = { ...data };
    if (payload.startDate && !payload.startDate.includes("T")) payload.startDate = payload.startDate + "T00:00:00Z";
    if (payload.endDate && !payload.endDate.includes("T")) payload.endDate = payload.endDate + "T23:59:59Z";
    if (!payload.startDate) delete payload.startDate;
    if (!payload.endDate) delete payload.endDate;
    if (modal === "create") await adminCreateBanner(payload as CreateBannerRequest);
    else if (id)            await adminUpdateBanner(id, payload as UpdateBannerRequest);
    setModal(null); setSelected(null);
    load(currentPage);
  };

  const handleDelete = async () => {
    if (!selected) return;
    await adminDeleteBanner(selected.id);
    setModal(null); setSelected(null);
    load(currentPage);
  };

  const handleToggleStatus = async (banner: BannerResponse) => {
    setToggling(banner.id);
    try {
      const next: BannerStatus = banner.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
      await adminUpdateBannerStatus(banner.id, next);
      load(currentPage);
    } finally {
      setToggling(null);
    }
  };

  return (
    <div className="space-y-5 max-w-6xl">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-xl font-black text-slate-900">Quản lý Banner</h1>
          <p className="text-sm text-slate-400 mt-0.5">
            {page ? `${page.totalElements.toLocaleString()} banner` : "—"}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={() => load(currentPage)} className="flex items-center gap-1.5 px-3 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50">
            <RefreshCw className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={() => { setSelected(null); setModal("create"); }}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold"
          >
            <Plus className="w-3.5 h-3.5" /> Tạo banner
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-2">
        {[{ value: "" as const, label: "Tất cả" }, ...STATUSES].map(({ value, label }) => (
          <button key={value} onClick={() => { setStatusFilter(value); load(0, value); }}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold border transition-all ${
              statusFilter === value ? "bg-red-600 text-white border-red-600" : "bg-white text-slate-600 border-slate-200 hover:border-red-300"
            }`}>
            {label}
          </button>
        ))}
      </div>

      {/* Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-16 gap-2 text-slate-400">
            <Loader2 className="w-5 h-5 animate-spin" />
            <span className="text-sm font-semibold">Đang tải...</span>
          </div>
        ) : !page || page.content.length === 0 ? (
          <div className="text-center py-16 text-slate-400 text-sm font-semibold">Chưa có banner nào</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50">
                  {["Hình ảnh", "Tiêu đề", "Vị trí", "Thứ tự", "Trạng thái", "Bắt đầu", "Kết thúc", "Lượt xem", ""].map(h => (
                    <th key={h} className="px-4 py-3 text-left text-[11px] font-black text-slate-400 uppercase tracking-widest whitespace-nowrap">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {page.content.map(banner => (
                  <tr key={banner.id} className="hover:bg-slate-50 transition-colors">
                    <td className="px-4 py-3">
                      <div className="w-16 h-10 rounded-lg overflow-hidden bg-slate-100 border border-slate-200 flex items-center justify-center">
                        {banner.imageUrl
                          ? <img src={banner.imageUrl} alt={banner.title} className="w-full h-full object-cover" onError={e => { e.currentTarget.style.display = "none"; }} />
                          : <ImageOff className="w-4 h-4 text-slate-300" />}
                      </div>
                    </td>
                    <td className="px-4 py-3 font-semibold text-slate-800 max-w-[160px] truncate">{banner.title}</td>
                    <td className="px-4 py-3 text-slate-500 whitespace-nowrap">{banner.positionCode}</td>
                    <td className="px-4 py-3 text-slate-400">{banner.sortOrder}</td>
                    <td className="px-4 py-3"><StatusBadge status={banner.status} /></td>
                    <td className="px-4 py-3 text-slate-400 whitespace-nowrap">{formatDate(banner.startDate)}</td>
                    <td className="px-4 py-3 text-slate-400 whitespace-nowrap">{formatDate(banner.endDate)}</td>
                    <td className="px-4 py-3 text-slate-400">{banner.viewCount.toLocaleString()}</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-1 justify-end">
                        <button
                          onClick={() => handleToggleStatus(banner)}
                          title="Bật/Tắt"
                          className="p-1.5 rounded-lg hover:bg-slate-100 text-slate-500 hover:text-red-600 transition-colors"
                          disabled={toggling === banner.id}
                        >
                          {toggling === banner.id
                            ? <Loader2 className="w-4 h-4 animate-spin" />
                            : banner.status === "ACTIVE"
                              ? <ToggleRight className="w-4 h-4 text-green-500" />
                              : <ToggleLeft className="w-4 h-4 text-slate-400" />
                          }
                        </button>
                        <button onClick={() => { setSelected(banner); setModal("edit"); }}
                          className="p-1.5 rounded-lg hover:bg-slate-100 text-slate-500 hover:text-red-600 transition-colors">
                          <Pencil className="w-4 h-4" />
                        </button>
                        <button onClick={() => { setSelected(banner); setModal("delete"); }}
                          className="p-1.5 rounded-lg hover:bg-slate-100 text-slate-500 hover:text-red-500 transition-colors">
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {page && page.totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-slate-100">
            <span className="text-xs text-slate-400 font-semibold">Trang {currentPage + 1} / {page.totalPages}</span>
            <div className="flex gap-1">
              <button disabled={page.first} onClick={() => load(currentPage - 1)}
                className="p-1.5 rounded-lg border border-slate-200 disabled:opacity-40 hover:bg-slate-50">
                <ChevronLeft className="w-4 h-4 text-slate-600" />
              </button>
              <button disabled={page.last} onClick={() => load(currentPage + 1)}
                className="p-1.5 rounded-lg border border-slate-200 disabled:opacity-40 hover:bg-slate-50">
                <ChevronRight className="w-4 h-4 text-slate-600" />
              </button>
            </div>
          </div>
        )}
      </div>

      {(modal === "create" || modal === "edit") && (
        <BannerFormModal
          mode={modal}
          initial={modal === "edit" ? selected ?? undefined : undefined}
          positions={positions}
          onClose={() => { setModal(null); setSelected(null); }}
          onSave={handleSave}
        />
      )}

      {modal === "delete" && selected && (
        <DeleteModal
          banner={selected}
          onClose={() => { setModal(null); setSelected(null); }}
          onDelete={handleDelete}
        />
      )}
    </div>
  );
}
