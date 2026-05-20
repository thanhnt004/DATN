import { useEffect, useState, useRef, useCallback } from "react";
import {
  ChevronRight,
  ChevronDown,
  Plus,
  Pencil,
  Trash2,
  Loader2,
  RefreshCw,
  FolderOpen,
  Folder,
  X,
  GripVertical,
  Image as ImageIcon,
  Search,
  Eye,
  EyeOff,
  ArrowRightLeft,
  Upload,
  Tag,
} from "lucide-react";
import {
  adminGetCategoryTree,
  adminCreateCategory,
  adminUpdateCategory,
  adminDeleteCategory,
  adminMoveCategory,
  adminGetCategoryAttributes,
  adminCreateCategoryAttribute,
  adminUpdateCategoryAttribute,
  adminDeleteCategoryAttribute,
  adminNormalizeCategorySortOrder,
} from "../../api/adminApi";
import { uploadFile } from "../../api/fileApi";
import type {
  CategoryTreeResponse,
  CategoryStatus,
  CreateCategoryRequest,
  UpdateCategoryRequest,
  CategoryAttributeRequest,
} from "../../types/admin";

// ─── Helpers ─────────────────────────────────────────────────────────────────

function flatCount(nodes: CategoryTreeResponse[]): number {
  return nodes.reduce((acc, n) => acc + 1 + flatCount(n.children ?? []), 0);
}

function filterTree(nodes: CategoryTreeResponse[], query: string): CategoryTreeResponse[] {
  if (!query) return nodes;
  const q = query.toLowerCase();
  return nodes.reduce<CategoryTreeResponse[]>((acc, n) => {
    const childMatches = filterTree(n.children ?? [], query);
    if (n.name.toLowerCase().includes(q) || n.slug.toLowerCase().includes(q) || childMatches.length > 0) {
      acc.push({ ...n, children: childMatches.length > 0 ? childMatches : n.name.toLowerCase().includes(q) ? n.children : [] });
    }
    return acc;
  }, []);
}

const STATUS_LABELS: Record<CategoryStatus, { label: string; cls: string }> = {
  ACTIVE: { label: "Hiển thị", cls: "bg-green-100 text-green-700" },
  INACTIVE: { label: "Ẩn", cls: "bg-slate-100 text-slate-500" },
  DELETED: { label: "Đã xoá", cls: "bg-red-100 text-red-500" },
};

// ─── Drag-and-Drop Context ──────────────────────────────────────────────────

interface DragState {
  dragId: string;
  dragNode: CategoryTreeResponse;
  overId: string | null;
  dropPosition: "before" | "inside" | "after" | null;
}

// ─── Image Upload Component ─────────────────────────────────────────────────

function ImageUploader({
  label,
  url,
  onUploaded,
  onRemove,
  folder,
  size = "sm",
}: {
  label: string;
  url: string;
  onUploaded: (url: string) => void;
  onRemove: () => void;
  folder: string;
  size?: "sm" | "md";
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);

  const handleFile = async (file: File) => {
    if (!file.type.startsWith("image/")) return;
    setUploading(true);
    try {
      const resp = await uploadFile(file, folder);
      const uploaded = resp.data.result;
      onUploaded(uploaded.secureUrl || uploaded.url);
    } catch (err) {
      console.error("Upload failed:", err);
    } finally {
      setUploading(false);
    }
  };

  const imgCls = size === "md" ? "w-20 h-20" : "w-10 h-10";

  return (
    <div>
      <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest mb-1">
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
      {url ? (
        <div className="flex items-center gap-2 mt-1">
          <img
            src={url}
            alt=""
            className={`${imgCls} rounded-xl object-cover border border-slate-100`}
            onError={(e) => { (e.currentTarget as HTMLImageElement).style.display = "none"; }}
          />
          <div className="flex flex-col gap-1">
            <button
              type="button"
              onClick={() => inputRef.current?.click()}
              disabled={uploading}
              className="text-xs text-red-600 font-semibold hover:underline disabled:opacity-50"
            >
              {uploading ? "Đang tải..." : "Đổi ảnh"}
            </button>
            <button
              type="button"
              onClick={onRemove}
              className="text-xs text-red-500 font-semibold hover:underline"
            >
              Xoá
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
          {uploading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Upload className="w-3.5 h-3.5" />}
          {uploading ? "Đang tải..." : "Tải ảnh lên"}
        </button>
      )}
    </div>
  );
}

// ─── Attribute Row Component ────────────────────────────────────────────────

interface AttrFormRow {
  _key: string;              // local key for React
  existingId?: string;       // if editing existing attribute
  name: string;
  isRequired: boolean;
  isFilterable: boolean;
  dataType: "string" | "number" | "boolean" | "enum";
  predefinedValues: string[];
  _deleted?: boolean;
}

function newAttrRow(): AttrFormRow {
  return {
    _key: crypto.randomUUID(),
    name: "",
    isRequired: false,
    isFilterable: true,
    dataType: "string",
    predefinedValues: [],
  };
}

function AttributeEditor({
  attrs,
  onChange,
}: {
  attrs: AttrFormRow[];
  onChange: (attrs: AttrFormRow[]) => void;
}) {
  const update = (idx: number, patch: Partial<AttrFormRow>) => {
    const next = [...attrs];
    next[idx] = { ...next[idx], ...patch };
    onChange(next);
  };

  const remove = (idx: number) => {
    const row = attrs[idx];
    if (row.existingId) {
      // Mark for deletion
      const next = [...attrs];
      next[idx] = { ...row, _deleted: true };
      onChange(next);
    } else {
      onChange(attrs.filter((_, i) => i !== idx));
    }
  };

  const addValue = (idx: number) => {
    const next = [...attrs];
    next[idx] = { ...next[idx], predefinedValues: [...next[idx].predefinedValues, ""] };
    onChange(next);
  };

  const updateValue = (attrIdx: number, valIdx: number, val: string) => {
    const next = [...attrs];
    const vals = [...next[attrIdx].predefinedValues];
    vals[valIdx] = val;
    next[attrIdx] = { ...next[attrIdx], predefinedValues: vals };
    onChange(next);
  };

  const removeValue = (attrIdx: number, valIdx: number) => {
    const next = [...attrs];
    next[attrIdx] = {
      ...next[attrIdx],
      predefinedValues: next[attrIdx].predefinedValues.filter((_, i) => i !== valIdx),
    };
    onChange(next);
  };

  const visibleAttrs = attrs.filter((a) => !a._deleted);

  return (
    <div>
      <div className="flex items-center justify-between mb-2">
        <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest">
          <Tag className="w-3 h-3 inline-block mr-1 -mt-0.5" />
          Thuộc tính ({visibleAttrs.length})
        </label>
        <button
          type="button"
          onClick={() => onChange([...attrs, newAttrRow()])}
          className="text-xs font-bold text-red-600 hover:text-red-700 flex items-center gap-0.5"
        >
          <Plus className="w-3 h-3" /> Thêm
        </button>
      </div>

      {visibleAttrs.length === 0 && (
        <p className="text-xs text-slate-400 italic">Chưa có thuộc tính nào.</p>
      )}

      <div className="space-y-3">
        {attrs.map((attr, idx) => {
          if (attr._deleted) return null;
          return (
            <div key={attr._key} className="rounded-xl border border-slate-200 bg-slate-50/50 p-3 space-y-2">
              <div className="flex items-start gap-2">
                {/* Name */}
                <input
                  type="text"
                  className="flex-1 px-2.5 py-1.5 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                  value={attr.name}
                  onChange={(e) => update(idx, { name: e.target.value })}
                  placeholder="Tên thuộc tính (VD: Màu sắc)"
                />
                <button
                  type="button"
                  onClick={() => remove(idx)}
                  className="p-1 rounded-lg hover:bg-red-100 text-slate-400 hover:text-red-500 shrink-0"
                  title="Xoá thuộc tính"
                >
                  <Trash2 className="w-3 h-3" />
                </button>
              </div>

              <div className="flex flex-wrap gap-2 items-center">
                {/* Data Type */}
                <select
                  className="px-2 py-1 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                  value={attr.dataType}
                  onChange={(e) =>
                    update(idx, { dataType: e.target.value as AttrFormRow["dataType"] })
                  }
                >
                  <option value="string">Chuỗi</option>
                  <option value="number">Số</option>
                  <option value="boolean">Boolean</option>
                  <option value="enum">Enum</option>
                </select>

                {/* Toggles */}
                <label className="flex items-center gap-1 text-xs text-slate-500 cursor-pointer">
                  <input
                    type="checkbox"
                    className="w-3.5 h-3.5 rounded accent-red-600"
                    checked={attr.isRequired}
                    onChange={(e) => update(idx, { isRequired: e.target.checked })}
                  />
                  Bắt buộc
                </label>
                <label className="flex items-center gap-1 text-xs text-slate-500 cursor-pointer">
                  <input
                    type="checkbox"
                    className="w-3.5 h-3.5 rounded accent-red-600"
                    checked={attr.isFilterable}
                    onChange={(e) => update(idx, { isFilterable: e.target.checked })}
                  />
                  Bộ lọc
                </label>
              </div>

              {/* Predefined values for enum */}
              {attr.dataType === "enum" && (
                <div className="pl-2 space-y-1.5">
                  <span className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">
                    Giá trị
                  </span>
                  {attr.predefinedValues.map((v, vi) => (
                    <div key={vi} className="flex items-center gap-1.5">
                      <input
                        type="text"
                        value={v}
                        onChange={(e) => updateValue(idx, vi, e.target.value)}
                        className="flex-1 px-2 py-1 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                        placeholder={`Giá trị ${vi + 1}`}
                      />
                      <button
                        type="button"
                        onClick={() => removeValue(idx, vi)}
                        className="p-0.5 rounded hover:bg-red-100 text-slate-400 hover:text-red-500"
                      >
                        <X className="w-3 h-3" />
                      </button>
                    </div>
                  ))}
                  <button
                    type="button"
                    onClick={() => addValue(idx)}
                    className="text-[10px] font-bold text-red-500 hover:text-red-700 flex items-center gap-0.5"
                  >
                    <Plus className="w-2.5 h-2.5" /> Thêm giá trị
                  </button>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ─── Category Form Modal (file upload + attributes) ─────────────────────────

interface CategoryFormModalProps {
  mode: "create" | "edit";
  parentId?: string;
  parentName?: string;
  initial?: CategoryTreeResponse;
  onClose: () => void;
  onSave: (data: CreateCategoryRequest | UpdateCategoryRequest, id?: string) => Promise<void>;
}

const EMPTY_FORM = {
  name: "",
  description: "",
  status: "ACTIVE" as CategoryStatus,
  sortOrder: 0,
  iconUrl: "",
  imageUrl: "",
};

function CategoryFormModal({ mode, parentId, parentName, initial, onClose, onSave }: CategoryFormModalProps) {
  const [form, setForm] = useState(() =>
    initial
      ? {
          name: initial.name,
          description: initial.description ?? "",
          status: initial.status ?? "ACTIVE",
          sortOrder: initial.sortOrder ?? 0,
          iconUrl: initial.iconUrl ?? "",
          imageUrl: initial.imageUrl ?? "",
        }
      : EMPTY_FORM
  );
  const [attrs, setAttrs] = useState<AttrFormRow[]>([]);
  const [loadingAttrs, setLoadingAttrs] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Load existing attributes when editing
  useEffect(() => {
    if (mode === "edit" && initial?.id) {
      setLoadingAttrs(true);
      adminGetCategoryAttributes(initial.id)
        .then((list) => {
          setAttrs(
            list.map((a) => ({
              _key: a.id,
              existingId: a.id,
              name: a.value,                     // backend "value" = attribute name
              isRequired: false,                 // not returned in response, default false
              isFilterable: a.filterable,
              dataType: (a.dataType || "string") as AttrFormRow["dataType"],
              predefinedValues: a.predefinedValues?.map((pv) => pv.value) ?? [],
            }))
          );
        })
        .catch(() => {})
        .finally(() => setLoadingAttrs(false));
    }
  }, [mode, initial?.id]);

  const set = <K extends keyof typeof EMPTY_FORM>(k: K, v: (typeof EMPTY_FORM)[K]) =>
    setForm((f) => ({ ...f, [k]: v }));

  const handleSave = async () => {
    if (!form.name.trim()) {
      setError("Tên danh mục không được để trống");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const base = {
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        iconUrl: form.iconUrl.trim() || undefined,
        imageUrl: form.imageUrl.trim() || undefined,
        status: form.status,
        sortOrder: form.sortOrder,
      };

      if (mode === "create") {
        // Build attributes array for create
        const attrPayload: CategoryAttributeRequest[] = attrs
          .filter((a) => !a._deleted && a.name.trim())
          .map((a) => ({
            name: a.name.trim(),
            isRequired: a.isRequired,
            isFilterable: a.isFilterable,
            dataType: a.dataType,
            predefinedValues:
              a.dataType === "enum"
                ? a.predefinedValues.filter((v) => v.trim()).map((v) => ({ value: v.trim() }))
                : undefined,
          }));

        const payload: CreateCategoryRequest = {
          ...base,
          sortOrder: form.sortOrder > 0 ? form.sortOrder : undefined, // Let backend auto-assign if default 0
          parentId: parentId ?? undefined,
          attributes: attrPayload.length > 0 ? attrPayload : undefined,
        };
        await onSave(payload);
      } else {
        // Update category (no inline attributes)
        await onSave(base as UpdateCategoryRequest, initial?.id);

        // Handle attribute changes separately
        for (const attr of attrs) {
          if (attr._deleted && attr.existingId) {
            await adminDeleteCategoryAttribute(attr.existingId);
          } else if (attr._deleted) {
            // New attribute marked for deletion, skip
            continue;
          } else if (attr.existingId && attr.name.trim()) {
            await adminUpdateCategoryAttribute(attr.existingId, {
              name: attr.name.trim(),
              isRequired: attr.isRequired,
              isFilterable: attr.isFilterable,
              dataType: attr.dataType,
              predefinedValues:
                attr.dataType === "enum"
                  ? attr.predefinedValues.filter((v) => v.trim()).map((v) => ({ value: v.trim() }))
                  : undefined,
            });
          } else if (!attr.existingId && attr.name.trim()) {
            // New attribute added in edit mode
            await adminCreateCategoryAttribute(initial!.id, {
              name: attr.name.trim(),
              isRequired: attr.isRequired,
              isFilterable: attr.isFilterable,
              dataType: attr.dataType,
              predefinedValues:
                attr.dataType === "enum"
                  ? attr.predefinedValues.filter((v) => v.trim()).map((v) => ({ value: v.trim() }))
                  : undefined,
            });
          }
        }
      }
    } catch {
      setError("Lưu thất bại. Vui lòng thử lại.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm" onClick={onClose}>
      <div className="bg-white rounded-2xl w-full max-w-lg shadow-2xl" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
          <div>
            <h3 className="font-black text-slate-800">
              {mode === "create" ? "Thêm danh mục" : "Chỉnh sửa danh mục"}
            </h3>
            {parentName && <p className="text-xs text-slate-400 mt-0.5">Danh mục cha: <strong>{parentName}</strong></p>}
          </div>
          <button onClick={onClose} className="p-1 rounded-lg hover:bg-slate-100">
            <X className="w-5 h-5 text-slate-400" />
          </button>
        </div>

        <div className="p-5 space-y-4 max-h-[70vh] overflow-y-auto">
          {/* Name */}
          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest mb-1">
              Tên danh mục <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              autoFocus
              className="w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400 transition-colors"
              value={form.name}
              onChange={(e) => set("name", e.target.value)}
              placeholder="VD: Điện thoại, Thời trang..."
            />
          </div>

          {/* Description */}
          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest mb-1">Mô tả</label>
            <textarea
              rows={2}
              className="w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400 resize-none transition-colors"
              value={form.description}
              onChange={(e) => set("description", e.target.value)}
              placeholder="Mô tả ngắn..."
            />
          </div>

          {/* Image Uploads */}
          <div className="grid grid-cols-2 gap-3">
            <ImageUploader
              label="Icon"
              url={form.iconUrl}
              onUploaded={(url) => set("iconUrl", url)}
              onRemove={() => set("iconUrl", "")}
              folder="categories/icons"
              size="sm"
            />
            <ImageUploader
              label="Ảnh danh mục"
              url={form.imageUrl}
              onUploaded={(url) => set("imageUrl", url)}
              onRemove={() => set("imageUrl", "")}
              folder="categories/images"
              size="md"
            />
          </div>

          {/* Status + Sort */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest mb-1">Trạng thái</label>
              <select
                className="w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400"
                value={form.status}
                onChange={(e) => set("status", e.target.value as CategoryStatus)}
              >
                <option value="ACTIVE">Hiển thị</option>
                <option value="INACTIVE">Ẩn</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest mb-1">Thứ tự</label>
              <input
                type="number"
                className="w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400"
                value={form.sortOrder}
                onChange={(e) => { e.target.value = e.target.value.replace(/^0+(?=\d)/, ''); set("sortOrder", +e.target.value); }}
              />
            </div>
          </div>

          {/* Attributes */}
          <div className="border-t border-slate-100 pt-4">
            {loadingAttrs ? (
              <div className="flex items-center gap-2 text-slate-400 text-xs">
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                Đang tải thuộc tính...
              </div>
            ) : (
              <AttributeEditor attrs={attrs} onChange={setAttrs} />
            )}
          </div>

          {error && <p className="text-xs text-red-500 font-semibold">{error}</p>}
        </div>

        <div className="px-5 pb-5 flex justify-end gap-2">
          <button onClick={onClose} className="px-4 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50">
            Hủy
          </button>
          <button
            onClick={handleSave}
            disabled={saving}
            className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-60 flex items-center gap-2"
          >
            {saving && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
            Lưu
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Delete Confirm Modal ───────────────────────────────────────────────────

function DeleteModal({
  category,
  onClose,
  onDelete,
}: {
  category: CategoryTreeResponse;
  onClose: () => void;
  onDelete: () => Promise<void>;
}) {
  const [loading, setLoading] = useState(false);
  const childCount = flatCount(category.children ?? []);
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm" onClick={onClose}>
      <div className="bg-white rounded-2xl w-full max-w-sm shadow-2xl p-6" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-black text-slate-800 mb-2">Xoá danh mục</h3>
        <p className="text-sm text-slate-500 mb-2">
          Bạn có chắc muốn xoá <strong>"{category.name}"</strong>?
        </p>
        {childCount > 0 && (
          <p className="text-xs text-orange-600 font-semibold bg-orange-50 border border-orange-100 rounded-xl px-3 py-2 mb-4">
            ⚠️ Danh mục này có {childCount} danh mục con. Tất cả sẽ bị xoá.
          </p>
        )}
        <div className="flex gap-2 justify-end mt-4">
          <button onClick={onClose} className="px-4 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50">
            Hủy
          </button>
          <button
            disabled={loading}
            onClick={async () => {
              setLoading(true);
              await onDelete();
            }}
            className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-60 flex items-center gap-2"
          >
            {loading && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
            Xoá
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Detail Panel ───────────────────────────────────────────────────────────

function DetailPanel({ node, onClose }: { node: CategoryTreeResponse; onClose: () => void }) {
  const st = STATUS_LABELS[node.status] ?? STATUS_LABELS.ACTIVE;
  return (
    <div className="border-l border-slate-200 bg-white w-80 shrink-0 overflow-y-auto">
      <div className="flex items-center justify-between px-4 py-3 border-b border-slate-100">
        <h3 className="font-black text-sm text-slate-800 truncate">Chi tiết danh mục</h3>
        <button onClick={onClose} className="p-1 rounded-lg hover:bg-slate-100"><X className="w-4 h-4 text-slate-400" /></button>
      </div>
      <div className="p-4 space-y-4">
        {/* Images */}
        <div className="flex items-center gap-3">
          {node.iconUrl ? (
            <img src={node.iconUrl} alt="" className="w-10 h-10 rounded-xl object-cover border border-slate-100" />
          ) : (
            <div className="w-10 h-10 rounded-xl bg-slate-100 flex items-center justify-center">
              <Folder className="w-5 h-5 text-slate-300" />
            </div>
          )}
          {node.imageUrl && (
            <img src={node.imageUrl} alt="" className="w-16 h-10 rounded-xl object-cover border border-slate-100" />
          )}
        </div>

        {/* Name */}
        <div>
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Tên</span>
          <p className="text-sm font-bold text-slate-800 mt-0.5">{node.name}</p>
        </div>

        {/* Slug */}
        <div>
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Slug</span>
          <p className="text-xs text-slate-500 mt-0.5 font-mono bg-slate-50 rounded-lg px-2 py-1">{node.slug}</p>
        </div>

        {/* Description */}
        {node.description && (
          <div>
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Mô tả</span>
            <p className="text-xs text-slate-600 mt-0.5">{node.description}</p>
          </div>
        )}

        {/* Meta grid */}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Cấp</span>
            <p className="text-sm font-bold text-slate-700 mt-0.5">Level {node.level}</p>
          </div>
          <div>
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Thứ tự</span>
            <p className="text-sm font-bold text-slate-700 mt-0.5">{node.sortOrder}</p>
          </div>
          <div>
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Trạng thái</span>
            <span className={`inline-block mt-0.5 text-[10px] font-bold px-2 py-0.5 rounded-full ${st.cls}`}>{st.label}</span>
          </div>
          <div>
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Danh mục con</span>
            <p className="text-sm font-bold text-slate-700 mt-0.5">{node.children?.length ?? 0}</p>
          </div>
        </div>

        {/* Path */}
        <div>
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Path</span>
          <p className="text-[10px] text-slate-500 mt-0.5 font-mono break-all bg-slate-50 rounded-lg px-2 py-1">{node.path}</p>
        </div>

        {/* ID */}
        <div>
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">ID</span>
          <p className="text-[10px] text-slate-400 mt-0.5 font-mono break-all">{node.id}</p>
        </div>
      </div>
    </div>
  );
}

// ─── Drag-and-Drop Tree Node ────────────────────────────────────────────────

interface TreeNodeProps {
  node: CategoryTreeResponse;
  depth: number;
  drag: DragState | null;
  expandedIds: Set<string>;
  selectedId: string | null;
  highlightId: string | null;
  onToggleExpand: (id: string) => void;
  onSelect: (node: CategoryTreeResponse) => void;
  onAddChild: (node: CategoryTreeResponse) => void;
  onEdit: (node: CategoryTreeResponse) => void;
  onDelete: (node: CategoryTreeResponse) => void;
  onToggleStatus: (node: CategoryTreeResponse) => void;
  onDragStart: (e: React.DragEvent, node: CategoryTreeResponse) => void;
  onDragOver: (e: React.DragEvent, node: CategoryTreeResponse) => void;
  onDragLeave: () => void;
  onDrop: (e: React.DragEvent, node: CategoryTreeResponse) => void;
}

function TreeNode({
  node,
  depth,
  drag,
  expandedIds,
  selectedId,
  highlightId,
  onToggleExpand,
  onSelect,
  onAddChild,
  onEdit,
  onDelete,
  onToggleStatus,
  onDragStart,
  onDragOver,
  onDragLeave,
  onDrop,
}: TreeNodeProps) {
  const expanded = expandedIds.has(node.id);
  const hasChildren = (node.children?.length ?? 0) > 0;
  const isSelected = selectedId === node.id;
  const isDragging = drag?.dragId === node.id;
  const isOver = drag?.overId === node.id;
  const isHighlighted = highlightId === node.id;

  const st = STATUS_LABELS[node.status] ?? STATUS_LABELS.ACTIVE;

  // Drop indicator classes
  let dropIndicator = "";
  if (isOver && drag?.dropPosition === "before") dropIndicator = "border-t-2 border-red-500";
  if (isOver && drag?.dropPosition === "after") dropIndicator = "border-b-2 border-red-500";
  if (isOver && drag?.dropPosition === "inside") dropIndicator = "ring-2 ring-red-400 ring-inset bg-red-50/50";

  return (
    <div className={isDragging ? "opacity-30" : ""}>
      <div
        data-category-id={node.id}
        draggable
        onDragStart={(e) => onDragStart(e, node)}
        onDragOver={(e) => onDragOver(e, node)}
        onDragLeave={onDragLeave}
        onDrop={(e) => onDrop(e, node)}
        onClick={() => onSelect(node)}
        className={`group flex items-center gap-1.5 px-2 py-1.5 rounded-xl transition-all cursor-default select-none
          ${isHighlighted ? "bg-amber-50 border border-amber-300 ring-2 ring-amber-200" : isSelected ? "bg-red-50 border border-red-200" : "hover:bg-slate-50 border border-transparent"}
          ${dropIndicator}`}
        style={{ paddingLeft: `${8 + depth * 20}px` }}
      >
        {/* Drag handle */}
        <span className="cursor-grab active:cursor-grabbing text-slate-300 hover:text-slate-500 shrink-0">
          <GripVertical className="w-3.5 h-3.5" />
        </span>

        {/* Expand toggle */}
        <button
          className="w-5 h-5 flex items-center justify-center shrink-0 text-slate-400 hover:text-slate-600"
          onClick={(e) => {
            e.stopPropagation();
            onToggleExpand(node.id);
          }}
        >
          {hasChildren ? (
            expanded ? (
              <ChevronDown className="w-3.5 h-3.5" />
            ) : (
              <ChevronRight className="w-3.5 h-3.5" />
            )
          ) : (
            <span className="w-3.5" />
          )}
        </button>

        {/* Icon */}
        {node.iconUrl ? (
          <img src={node.iconUrl} alt="" className="w-5 h-5 rounded-md object-cover shrink-0" />
        ) : hasChildren && expanded ? (
          <FolderOpen className="w-4 h-4 text-red-400 shrink-0" />
        ) : (
          <Folder className="w-4 h-4 text-slate-300 shrink-0" />
        )}

        {/* Name */}
        <span className="font-semibold text-sm text-slate-800 flex-1 min-w-0 truncate">{node.name}</span>

        {/* Badges */}
        <span className={`text-[9px] font-bold px-1.5 py-0.5 rounded-full shrink-0 ${st.cls}`}>{st.label}</span>
        {hasChildren && (
          <span className="text-[9px] font-bold bg-red-50 text-red-400 px-1.5 py-0.5 rounded-full shrink-0 tabular-nums">
            {node.children?.length}
          </span>
        )}
        <span className="text-[9px] font-mono text-slate-300 shrink-0 tabular-nums w-5 text-right mr-0.5">{node.sortOrder}</span>

        {/* Actions */}
        <div className="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity shrink-0">
          <button
            onClick={(e) => {
              e.stopPropagation();
              onToggleStatus(node);
            }}
            title={node.status === "ACTIVE" ? "Ẩn" : "Hiển thị"}
            className="p-1 rounded-lg hover:bg-slate-200 text-slate-400 hover:text-slate-700"
          >
            {node.status === "ACTIVE" ? <EyeOff className="w-3 h-3" /> : <Eye className="w-3 h-3" />}
          </button>
          <button
            onClick={(e) => {
              e.stopPropagation();
              onAddChild(node);
            }}
            title="Thêm danh mục con"
            className="p-1 rounded-lg hover:bg-red-100 text-slate-400 hover:text-red-600"
          >
            <Plus className="w-3 h-3" />
          </button>
          <button
            onClick={(e) => {
              e.stopPropagation();
              onEdit(node);
            }}
            title="Chỉnh sửa"
            className="p-1 rounded-lg hover:bg-slate-200 text-slate-400 hover:text-slate-700"
          >
            <Pencil className="w-3 h-3" />
          </button>
          <button
            onClick={(e) => {
              e.stopPropagation();
              onDelete(node);
            }}
            title="Xoá"
            className="p-1 rounded-lg hover:bg-red-100 text-slate-400 hover:text-red-500"
          >
            <Trash2 className="w-3 h-3" />
          </button>
        </div>
      </div>

      {/* Children */}
      {hasChildren && expanded && (
        <div>
          {[...(node.children ?? [])].sort((a, b) => a.sortOrder - b.sortOrder).map((child) => (
            <TreeNode
              key={child.id}
              node={child}
              depth={depth + 1}
              drag={drag}
              expandedIds={expandedIds}
              selectedId={selectedId}
              highlightId={highlightId}
              onToggleExpand={onToggleExpand}
              onSelect={onSelect}
              onAddChild={onAddChild}
              onEdit={onEdit}
              onDelete={onDelete}
              onToggleStatus={onToggleStatus}
              onDragStart={onDragStart}
              onDragOver={onDragOver}
              onDragLeave={onDragLeave}
              onDrop={onDrop}
            />
          ))}
        </div>
      )}
    </div>
  );
}

// ─── Root-level Drop Zone (drop to make root) ──────────────────────────────

function RootDropZone({
  drag,
  onDragOver,
  onDragLeave,
  onDrop,
}: {
  drag: DragState | null;
  onDragOver: (e: React.DragEvent) => void;
  onDragLeave: () => void;
  onDrop: (e: React.DragEvent) => void;
}) {
  const isActive = drag && drag.overId === "__root__";
  return (
    <div
      onDragOver={onDragOver}
      onDragLeave={onDragLeave}
      onDrop={onDrop}
      className={`h-8 mx-3 mb-1 rounded-xl border-2 border-dashed flex items-center justify-center text-xs font-semibold transition-all ${
        isActive ? "border-red-400 bg-red-50 text-red-500" : "border-slate-200 text-slate-300"
      }`}
    >
      <ArrowRightLeft className="w-3 h-3 mr-1" />
      Thả vào đây để thành danh mục gốc
    </div>
  );
}

// ─── Main Page ──────────────────────────────────────────────────────────────

type ModalState =
  | { type: "create-root" }
  | { type: "create-child"; parent: CategoryTreeResponse }
  | { type: "edit"; node: CategoryTreeResponse }
  | { type: "delete"; node: CategoryTreeResponse }
  | null;

// ─── Helper: find a node by id in tree ─────────────────────────────────────
function findNodeById(nodes: CategoryTreeResponse[], id: string): CategoryTreeResponse | null {
  for (const n of nodes) {
    if (n.id === id) return n;
    if (n.children?.length) {
      const found = findNodeById(n.children, id);
      if (found) return found;
    }
  }
  return null;
}

// ─── Helper: get ancestor ids for a node (from path) ───────────────────────
function getAncestorIds(node: CategoryTreeResponse): string[] {
  if (!node.path) return [];
  return node.path.split('/').filter((s) => s && s !== node.id);
}

export default function AdminCategoriesPage() {
  const [tree, setTree] = useState<CategoryTreeResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [modal, setModal] = useState<ModalState>(null);
  const [search, setSearch] = useState("");
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());
  const [selectedNode, setSelectedNode] = useState<CategoryTreeResponse | null>(null);
  const [drag, setDrag] = useState<DragState | null>(null);
  const [moving, setMoving] = useState(false);
  const lastChangedIdRef = useRef<string | null>(null);
  const [highlightId, setHighlightId] = useState<string | null>(null);
  const treeContainerRef = useRef<HTMLDivElement>(null);
  const selectedNodeRef = useRef<CategoryTreeResponse | null>(null);
  selectedNodeRef.current = selectedNode;

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await adminGetCategoryTree();
      setTree(data);

      // Refresh selectedNode with latest data
      if (selectedNodeRef.current) {
        const updated = findNodeById(data, selectedNodeRef.current.id);
        setSelectedNode(updated);
      }

      // Auto-expand first 2 levels
      const ids = new Set<string>();
      const expand = (nodes: CategoryTreeResponse[], d: number) => {
        for (const n of nodes) {
          if (d < 2) ids.add(n.id);
          if (n.children?.length) expand(n.children, d + 1);
        }
      };
      expand(data, 0);

      // Also expand ancestors of the last-changed node so it's visible
      const changedId = lastChangedIdRef.current;
      if (changedId) {
        const changedNode = findNodeById(data, changedId);
        if (changedNode) {
          getAncestorIds(changedNode).forEach((aid) => ids.add(aid));
        }
      }

      setExpandedIds((prev) => {
        const merged = new Set(prev);
        ids.forEach((id) => merged.add(id));
        return merged;
      });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  // Scroll to last changed category after tree re-renders
  useEffect(() => {
    const changedId = lastChangedIdRef.current;
    if (!changedId || loading) return;
    // Wait for DOM update
    const timer = setTimeout(() => {
      const el = treeContainerRef.current?.querySelector(`[data-category-id="${changedId}"]`);
      if (el) {
        el.scrollIntoView({ behavior: "smooth", block: "center" });
        setHighlightId(changedId);
        setTimeout(() => setHighlightId(null), 1500);
      }
      lastChangedIdRef.current = null;
    }, 100);
    return () => clearTimeout(timer);
  }, [tree, loading]);

  // ── Tree interaction handlers ──

  const toggleExpand = (id: string) => {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  };

  const expandAll = () => {
    const ids = new Set<string>();
    const walk = (nodes: CategoryTreeResponse[]) => {
      for (const n of nodes) {
        if (n.children?.length) {
          ids.add(n.id);
          walk(n.children);
        }
      }
    };
    walk(tree);
    setExpandedIds(ids);
  };

  const collapseAll = () => setExpandedIds(new Set());

  // ── CRUD handlers ──

  const handleSave = async (data: CreateCategoryRequest | UpdateCategoryRequest, id?: string) => {
    let changedId: string | undefined;
    if (modal?.type === "edit" && id) {
      await adminUpdateCategory(id, data as UpdateCategoryRequest);
      changedId = id;
    } else {
      const resp = await adminCreateCategory(data as CreateCategoryRequest);
      changedId = resp?.id;
    }
    setModal(null);
    if (changedId) lastChangedIdRef.current = changedId;
    await load();
  };

  const handleDelete = async () => {
    if (modal?.type !== "delete") return;
    const parentId = modal.node.parentId;
    await adminDeleteCategory(modal.node.id);
    setModal(null);
    if (selectedNode?.id === modal.node.id) setSelectedNode(null);
    if (parentId) lastChangedIdRef.current = parentId;
    await load();
  };

  const handleToggleStatus = async (node: CategoryTreeResponse) => {
    const newStatus = node.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
    await adminUpdateCategory(node.id, { status: newStatus });
    lastChangedIdRef.current = node.id;
    await load();
  };

  // ── Drag-and-Drop handlers ──

  const dragRef = useRef<DragState | null>(null);

  const isDescendant = (parentId: string, childId: string, nodes: CategoryTreeResponse[]): boolean => {
    const find = (list: CategoryTreeResponse[]): CategoryTreeResponse | undefined => {
      for (const n of list) {
        if (n.id === parentId) return n;
        const found = find(n.children ?? []);
        if (found) return found;
      }
      return undefined;
    };
    const parent = find(nodes);
    if (!parent) return false;
    const check = (list: CategoryTreeResponse[]): boolean => {
      for (const n of list) {
        if (n.id === childId) return true;
        if (check(n.children ?? [])) return true;
      }
      return false;
    };
    return check(parent.children ?? []);
  };

  const handleDragStart = (e: React.DragEvent, node: CategoryTreeResponse) => {
    e.dataTransfer.effectAllowed = "move";
    e.dataTransfer.setData("text/plain", node.id);
    const state: DragState = { dragId: node.id, dragNode: node, overId: null, dropPosition: null };
    dragRef.current = state;
    // Delay to allow browser to capture drag image
    requestAnimationFrame(() => setDrag(state));
  };

  const handleDragOver = (e: React.DragEvent, node: CategoryTreeResponse) => {
    e.preventDefault();
    if (!dragRef.current || dragRef.current.dragId === node.id) return;
    if (isDescendant(dragRef.current.dragId, node.id, tree)) return;

    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
    const y = e.clientY - rect.top;
    const h = rect.height;
    let position: "before" | "inside" | "after";
    if (y < h * 0.25) position = "before";
    else if (y > h * 0.75) position = "after";
    else position = "inside";

    const next: DragState = { ...dragRef.current, overId: node.id, dropPosition: position };
    dragRef.current = next;
    setDrag(next);
    e.dataTransfer.dropEffect = "move";
  };

  const handleDragLeave = () => {
    if (dragRef.current) {
      const next = { ...dragRef.current, overId: null, dropPosition: null };
      dragRef.current = next;
      setDrag(next);
    }
  };

  const handleRootDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    if (!dragRef.current) return;
    const next: DragState = { ...dragRef.current, overId: "__root__", dropPosition: "inside" };
    dragRef.current = next;
    setDrag(next);
  };

  const handleRootDragLeave = () => {
    if (dragRef.current) {
      const next = { ...dragRef.current, overId: null, dropPosition: null };
      dragRef.current = next;
      setDrag(next);
    }
  };

  const executeDrop = async (targetNode: CategoryTreeResponse | null) => {
    if (!dragRef.current) return;
    const { dragNode, dropPosition } = dragRef.current;
    setDrag(null);
    dragRef.current = null;

    if (!targetNode && !dropPosition) return;

    setMoving(true);
    try {
      let newParentId: string | null | undefined;
      let newSortOrder: number | undefined;

      if (!targetNode) {
        // Dropped on root zone
        newParentId = null;
        newSortOrder = tree.length > 0 ? Math.max(...tree.map((n) => n.sortOrder)) + 1 : 0;
      } else if (dropPosition === "inside") {
        // Move as child of target
        newParentId = targetNode.id;
        const children = targetNode.children ?? [];
        newSortOrder = children.length > 0 ? Math.max(...children.map((c) => c.sortOrder)) + 1 : 0;
        // Auto-expand target
        setExpandedIds((prev) => new Set([...prev, targetNode.id]));
      } else if (dropPosition === "before") {
        // Place before target, same parent
        newParentId = targetNode.parentId;
        newSortOrder = Math.max(0, targetNode.sortOrder - 1);
      } else if (dropPosition === "after") {
        newParentId = targetNode.parentId;
        newSortOrder = targetNode.sortOrder + 1;
      }

      await adminMoveCategory(dragNode.id, {
        parentId: newParentId,
        sortOrder: newSortOrder,
      });
      lastChangedIdRef.current = dragNode.id;
      await load();
    } catch (err) {
      console.error("Move failed:", err);
    } finally {
      setMoving(false);
    }
  };

  const handleDrop = (e: React.DragEvent, node: CategoryTreeResponse) => {
    e.preventDefault();
    e.stopPropagation();
    executeDrop(node);
  };

  const handleRootDrop = (e: React.DragEvent) => {
    e.preventDefault();
    executeDrop(null);
  };

  const handleDragEnd = () => {
    dragRef.current = null;
    setDrag(null);
  };

  const handleNormalizeSortOrder = async () => {
    try {
      await adminNormalizeCategorySortOrder();
      await load();
    } catch (err) {
      console.error("Normalize sort order failed:", err);
    }
  };

  // ── Filtered tree ──
  const displayTree = search ? filterTree(tree, search) : tree;
  const total = flatCount(tree);
  const rootSorted = [...displayTree].sort((a, b) => a.sortOrder - b.sortOrder);

  return (
    <div className="flex gap-0 h-[calc(100vh-5rem)]" onDragEnd={handleDragEnd}>
      {/* ── Left: Tree panel ── */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Header */}
        <div className="flex items-center justify-between flex-wrap gap-3 px-1 pb-4">
          <div>
            <h1 className="text-xl font-black text-slate-900">Quản lý danh mục</h1>
            <p className="text-sm text-slate-400 mt-0.5">
              {loading ? "..." : `${total} danh mục · ${tree.length} gốc`}
              {moving && <span className="ml-2 text-red-500 font-semibold">Di chuyển...</span>}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button onClick={load} title="Làm mới" className="p-2 rounded-xl border border-slate-200 text-slate-500 hover:bg-slate-50">
              <RefreshCw className="w-3.5 h-3.5" />
            </button>
            <button onClick={expandAll} title="Mở tất cả" className="p-2 rounded-xl border border-slate-200 text-slate-500 hover:bg-slate-50 text-xs font-bold">
              <FolderOpen className="w-3.5 h-3.5" />
            </button>
            <button onClick={collapseAll} title="Thu gọn" className="p-2 rounded-xl border border-slate-200 text-slate-500 hover:bg-slate-50 text-xs font-bold">
              <Folder className="w-3.5 h-3.5" />
            </button>
            <button
              onClick={handleNormalizeSortOrder}
              title="Sắp xếp lại thứ tự tất cả danh mục"
              className="flex items-center gap-1 px-3 py-2 rounded-xl border border-slate-200 text-slate-500 hover:bg-slate-50 text-xs font-bold"
            >
              <ArrowRightLeft className="w-3.5 h-3.5" /> Chuẩn hoá thứ tự
            </button>
            <button
              onClick={() => setModal({ type: "create-root" })}
              className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold"
            >
              <Plus className="w-3.5 h-3.5" /> Thêm gốc
            </button>
          </div>
        </div>

        {/* Search */}
        <div className="relative px-1 pb-3">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-300 pointer-events-none" />
          <input
            type="text"
            placeholder="Tìm danh mục..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-9 pr-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-300 transition-colors"
          />
          {search && (
            <button onClick={() => setSearch("")} className="absolute right-4 top-1/2 -translate-y-1/2 p-0.5 rounded hover:bg-slate-200">
              <X className="w-3.5 h-3.5 text-slate-400" />
            </button>
          )}
        </div>

        {/* Tree */}
        <div ref={treeContainerRef} className="flex-1 bg-white rounded-2xl border border-slate-200 shadow-sm overflow-y-auto">
          {loading ? (
            <div className="flex items-center justify-center py-16 gap-2 text-slate-400">
              <Loader2 className="w-5 h-5 animate-spin" />
              <span className="text-sm font-semibold">Đang tải...</span>
            </div>
          ) : displayTree.length === 0 ? (
            <div className="text-center py-16 text-slate-400 text-sm font-semibold">
              {search ? (
                <>Không tìm thấy danh mục nào cho "<strong>{search}</strong>"</>
              ) : (
                <>Chưa có danh mục nào. Nhấn "Thêm gốc" để bắt đầu.</>
              )}
            </div>
          ) : (
            <div className="py-2">
              {rootSorted.map((node) => (
                <TreeNode
                  key={node.id}
                  node={node}
                  depth={0}
                  drag={drag}
                  expandedIds={expandedIds}
                  selectedId={selectedNode?.id ?? null}
                  highlightId={highlightId}
                  onToggleExpand={toggleExpand}
                  onSelect={setSelectedNode}
                  onAddChild={(n) => setModal({ type: "create-child", parent: n })}
                  onEdit={(n) => setModal({ type: "edit", node: n })}
                  onDelete={(n) => setModal({ type: "delete", node: n })}
                  onToggleStatus={handleToggleStatus}
                  onDragStart={handleDragStart}
                  onDragOver={handleDragOver}
                  onDragLeave={handleDragLeave}
                  onDrop={handleDrop}
                />
              ))}

              {/* Root drop zone */}
              {drag && (
                <RootDropZone drag={drag} onDragOver={handleRootDragOver} onDragLeave={handleRootDragLeave} onDrop={handleRootDrop} />
              )}
            </div>
          )}
        </div>
      </div>

      {/* ── Right: Detail panel ── */}
      {selectedNode && <DetailPanel node={selectedNode} onClose={() => setSelectedNode(null)} />}

      {/* ── Modals ── */}
      {modal?.type === "create-root" && <CategoryFormModal mode="create" onClose={() => setModal(null)} onSave={handleSave} />}
      {modal?.type === "create-child" && (
        <CategoryFormModal mode="create" parentId={modal.parent.id} parentName={modal.parent.name} onClose={() => setModal(null)} onSave={handleSave} />
      )}
      {modal?.type === "edit" && <CategoryFormModal mode="edit" initial={modal.node} onClose={() => setModal(null)} onSave={handleSave} />}
      {modal?.type === "delete" && <DeleteModal category={modal.node} onClose={() => setModal(null)} onDelete={handleDelete} />}
    </div>
  );
}
