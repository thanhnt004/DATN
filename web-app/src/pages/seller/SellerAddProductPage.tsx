import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import {
  ArrowLeft, ChevronRight, Loader2, Plus, Trash2, Upload, X,
  Image as ImageIcon, Info, AlertCircle, Package, ListFilter, Eye,
} from "lucide-react";
import { getMySellerProfile, createProduct, getAvailableOptionTemplates } from "../../api/sellerDashboardApi";
import { adminGetCategoryTree, adminGetCategoryAttributes } from "../../api/adminApi";
import { uploadFile } from "../../api/fileApi";
import type { CategoryTreeResponse, CategoryAttributeResponse, OptionTemplateResponse } from "../../types/admin";
import type { CreateProductRequest, SkuRequest } from "../../types/product";

// ─── helpers ───────────────────────────────────────────────────────────────────

/** Generate all combinations from arrays of option values */
function cartesian<T>(...arrays: T[][]): T[][] {
  return arrays.reduce<T[][]>(
    (acc, arr) => acc.flatMap((combo) => arr.map((val) => [...combo, val])),
    [[]]
  );
}

/** Auto‑generate a unique SKU code from product name + option values + random suffix */
function buildSkuCode(productName: string, attrs: Record<string, string>, index: number): string {
  const prefix = productName
    .replace(/[^a-zA-Z0-9\u00C0-\u1EF9]/g, "")
    .slice(0, 6)
    .toUpperCase();
  const suffix = Object.values(attrs)
    .map((v) => v.replace(/[^a-zA-Z0-9]/g, "").slice(0, 3).toUpperCase())
    .join("-");
  const rand = Math.random().toString(36).slice(2, 6).toUpperCase();
  return `${prefix || "SKU"}-${suffix || String(index + 1).padStart(3, "0")}-${rand}`;
}

// ─── Reusable label ────────────────────────────────────────────────────────────

function SectionCard({ title, subtitle, children }: { title: string; subtitle?: string; children: React.ReactNode }) {
  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-sm">
      <div className="px-5 py-4 border-b border-slate-100">
        <h2 className="font-black text-slate-800">{title}</h2>
        {subtitle && <p className="text-xs text-slate-400 mt-0.5">{subtitle}</p>}
      </div>
      <div className="p-5">{children}</div>
    </div>
  );
}

const inputBase =
  "px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400 transition-colors";
const inputCls = `w-full ${inputBase}`;

// ─── Category Picker (tree drill‑down) ────────────────────────────────────────

function CategoryPicker({
  tree,
  selected,
  onSelect,
}: {
  tree: CategoryTreeResponse[];
  selected: CategoryTreeResponse | null;
  onSelect: (cat: CategoryTreeResponse, namePath?: string) => void;
}) {
  const [breadcrumb, setBreadcrumb] = useState<CategoryTreeResponse[]>([]);
  const currentList = breadcrumb.length === 0 ? tree : breadcrumb[breadcrumb.length - 1].children;

  const handleClick = (cat: CategoryTreeResponse) => {
    if (cat.children && cat.children.length > 0) {
      setBreadcrumb((prev) => [...prev, cat]);
    } else {
      // Leaf category – select it, build name path from breadcrumb
      const namePath = [...breadcrumb.map((b) => b.name), cat.name].join(" > ");
      onSelect(cat, namePath);
    }
  };

  const goBack = (index: number) => {
    setBreadcrumb((prev) => prev.slice(0, index));
  };

  return (
    <div>
      {/* Breadcrumb */}
      {breadcrumb.length > 0 && (
        <div className="flex items-center gap-1 text-xs mb-2 flex-wrap">
          <button
            type="button"
            onClick={() => goBack(0)}
            className="text-red-600 hover:underline font-semibold"
          >
            Tất cả
          </button>
          {breadcrumb.map((bc, i) => (
            <span key={bc.id} className="flex items-center gap-1">
              <ChevronRight className="w-3 h-3 text-slate-300" />
              <button
                type="button"
                onClick={() => goBack(i + 1)}
                className="text-red-600 hover:underline font-semibold"
              >
                {bc.name}
              </button>
            </span>
          ))}
        </div>
      )}

      {/* List */}
      <div className="max-h-60 overflow-y-auto border border-slate-100 rounded-xl">
        {currentList.length === 0 ? (
          <p className="text-xs text-slate-400 text-center py-4">Không có danh mục con</p>
        ) : (
          currentList
            .filter((c) => c.status === "ACTIVE")
            .map((cat) => {
              const isLeaf = !cat.children || cat.children.length === 0;
              return (
                <button
                  key={cat.id}
                  type="button"
                  onClick={() => handleClick(cat)}
                  className={`flex items-center justify-between w-full px-4 py-2.5 text-sm hover:bg-slate-50 transition-colors border-b border-slate-50 last:border-0 ${
                    selected?.id === cat.id ? "bg-red-50 text-red-700 font-bold" : "text-slate-700"
                  }`}
                >
                  <span className="flex items-center gap-2">
                    {cat.iconUrl && (
                      <img src={cat.iconUrl} alt="" className="w-5 h-5 rounded object-cover" />
                    )}
                    {cat.name}
                    {isLeaf && (
                      <span className="text-[10px] bg-green-100 text-green-700 px-1.5 py-0.5 rounded-full font-bold">
                        Chọn được
                      </span>
                    )}
                  </span>
                  {!isLeaf && <ChevronRight className="w-4 h-4 text-slate-300" />}
                </button>
              );
            })
        )}
      </div>

      {/* Selected */}
      {selected && (
        <div className="mt-2 flex items-center gap-2 px-3 py-2 rounded-xl bg-red-50 border border-red-200">
          <Package className="w-4 h-4 text-red-500 shrink-0" />
          <span className="text-sm font-bold text-red-700 truncate">{selected.path || selected.name}</span>
          <button
            type="button"
            onClick={() => onSelect(null as unknown as CategoryTreeResponse)}
            className="ml-auto p-0.5 rounded hover:bg-red-100"
          >
            <X className="w-3.5 h-3.5 text-red-400" />
          </button>
        </div>
      )}
    </div>
  );
}

// ─── Multi-image gallery upload ────────────────────────────────────────────────

function GalleryUpload({
  images,
  onChange,
  folder,
  maxImages,
}: {
  images: string[];
  onChange: (urls: string[]) => void;
  folder: string;
  maxImages?: number;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);

  const handleFiles = async (files: FileList) => {
    const imageFiles = Array.from(files).filter((f) => f.type.startsWith("image/"));
    if (imageFiles.length === 0) return;
    setUploading(true);
    try {
      const uploaded: string[] = [];
      for (const file of imageFiles) {
        if (maxImages && images.length + uploaded.length >= maxImages) break;
        const resp = await uploadFile(file, folder);
        const result = resp.data.result;
        uploaded.push(result.secureUrl || result.url);
      }
      onChange([...images, ...uploaded]);
    } catch (err) {
      console.error("Gallery upload failed:", err);
    } finally {
      setUploading(false);
    }
  };

  const remove = (index: number) => {
    onChange(images.filter((_, i) => i !== index));
  };

  return (
    <div>
      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        multiple
        className="hidden"
        onChange={(e) => {
          if (e.target.files) handleFiles(e.target.files);
          e.target.value = "";
        }}
      />
      <div className="flex flex-wrap gap-3">
        {images.map((url, i) => (
          <div key={i} className="relative group">
            <img
              src={url}
              alt=""
              className="w-24 h-24 rounded-xl object-cover border border-slate-100"
            />
            <button
              type="button"
              onClick={() => remove(i)}
              className="absolute -top-1.5 -right-1.5 p-0.5 bg-red-500 text-white rounded-full opacity-0 group-hover:opacity-100 transition-opacity shadow"
            >
              <X className="w-3 h-3" />
            </button>
          </div>
        ))}
        {(!maxImages || images.length < maxImages) && (
          <button
            type="button"
            onClick={() => inputRef.current?.click()}
            disabled={uploading}
            className="w-24 h-24 rounded-xl border-2 border-dashed border-slate-200 bg-slate-50 flex flex-col items-center justify-center gap-1 text-slate-400 hover:border-red-300 hover:text-red-500 transition-colors disabled:opacity-50"
          >
            {uploading ? (
              <Loader2 className="w-5 h-5 animate-spin" />
            ) : (
              <>
                <Upload className="w-5 h-5" />
                <span className="text-[10px] font-bold">Thêm ảnh</span>
              </>
            )}
          </button>
        )}
      </div>
    </div>
  );
}

// ─── Primary image upload ──────────────────────────────────────────────────────

function PrimaryImageUpload({
  value,
  onChange,
  folder,
}: {
  value: string;
  onChange: (url: string) => void;
  folder: string;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);

  const handleFile = async (file: File) => {
    if (!file.type.startsWith("image/")) return;
    setUploading(true);
    try {
      const resp = await uploadFile(file, folder);
      const result = resp.data.result;
      onChange(result.secureUrl || result.url);
    } catch (err) {
      console.error("Upload failed:", err);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div>
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
        <div className="relative inline-block group">
          <img
            src={value}
            alt="Primary"
            className="w-40 h-40 rounded-xl object-cover border-2 border-red-200"
          />
          <div className="absolute inset-0 bg-black/40 rounded-xl opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
            <button
              type="button"
              onClick={() => inputRef.current?.click()}
              disabled={uploading}
              className="px-2 py-1 bg-white rounded-lg text-xs font-bold text-slate-700 hover:bg-slate-100"
            >
              {uploading ? "Đang tải..." : "Đổi ảnh"}
            </button>
            <button
              type="button"
              onClick={() => onChange("")}
              className="px-2 py-1 bg-red-500 rounded-lg text-xs font-bold text-white hover:bg-red-600"
            >
              Xoá
            </button>
          </div>
          <span className="absolute top-1.5 left-1.5 px-1.5 py-0.5 bg-red-600 text-white text-[10px] font-bold rounded-md">
            Chính
          </span>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          disabled={uploading}
          className="w-40 h-40 rounded-xl border-2 border-dashed border-red-300 bg-red-50 flex flex-col items-center justify-center gap-2 text-red-400 hover:bg-red-100 transition-colors disabled:opacity-50"
        >
          {uploading ? (
            <Loader2 className="w-6 h-6 animate-spin" />
          ) : (
            <>
              <ImageIcon className="w-8 h-8" />
              <span className="text-xs font-bold">Ảnh chính *</span>
            </>
          )}
        </button>
      )}
    </div>
  );
}

// ─── Option row ────────────────────────────────────────────────────────────────

interface OptionRow {
  name: string;
  values: { value: string; imageUrl: string }[];
}

function OptionsEditor({
  options,
  onChange,
  folder,
}: {
  options: OptionRow[];
  onChange: (opts: OptionRow[]) => void;
  folder: string;
}) {
  const [newValueInputs, setNewValueInputs] = useState<Record<number, string>>({});
  const [templates, setTemplates] = useState<OptionTemplateResponse[]>([]);
  const [showTemplatePicker, setShowTemplatePicker] = useState(false);
  const [templateSearch, setTemplateSearch] = useState("");

  useEffect(() => {
    getAvailableOptionTemplates()
      .then(setTemplates)
      .catch(() => {});
  }, []);

  const filteredTemplates = templates.filter(
    (t) =>
      t.name.toLowerCase().includes(templateSearch.toLowerCase()) &&
      !options.some((o) => o.name.toLowerCase() === t.name.toLowerCase())
  );

  const applyTemplate = (tpl: OptionTemplateResponse) => {
    if (options.length >= 3) return;
    onChange([
      ...options,
      { name: tpl.name, values: tpl.values.map((v) => ({ value: v.value, imageUrl: "" })) },
    ]);
    setShowTemplatePicker(false);
    setTemplateSearch("");
  };

  const updateName = (i: number, name: string) => {
    const next = [...options];
    next[i] = { ...next[i], name };
    onChange(next);
  };

  const addValue = (i: number) => {
    const val = (newValueInputs[i] || "").trim();
    if (!val) return;
    if (options[i].values.some((v) => v.value === val)) return;
    const next = [...options];
    next[i] = { ...next[i], values: [...next[i].values, { value: val, imageUrl: "" }] };
    onChange(next);
    setNewValueInputs((prev) => ({ ...prev, [i]: "" }));
  };

  const removeValue = (optIdx: number, valIdx: number) => {
    const next = [...options];
    next[optIdx] = { ...next[optIdx], values: next[optIdx].values.filter((_, j) => j !== valIdx) };
    onChange(next);
  };

  const updateValueImage = async (optIdx: number, valIdx: number, file: File) => {
    if (!file.type.startsWith("image/")) return;
    try {
      const resp = await uploadFile(file, folder);
      const result = resp.data.result;
      const url = result.secureUrl || result.url;
      const next = [...options];
      const vals = [...next[optIdx].values];
      vals[valIdx] = { ...vals[valIdx], imageUrl: url };
      next[optIdx] = { ...next[optIdx], values: vals };
      onChange(next);
    } catch (err) {
      console.error("Option value image upload failed:", err);
    }
  };

  const removeValueImage = (optIdx: number, valIdx: number) => {
    const next = [...options];
    const vals = [...next[optIdx].values];
    vals[valIdx] = { ...vals[valIdx], imageUrl: "" };
    next[optIdx] = { ...next[optIdx], values: vals };
    onChange(next);
  };

  const addOption = () => {
    if (options.length >= 3) return;
    onChange([...options, { name: "", values: [] }]);
  };

  const removeOption = (i: number) => {
    onChange(options.filter((_, j) => j !== i));
  };

  return (
    <div className="space-y-4">
      {/* Template picker button */}
      {options.length < 3 && templates.length > 0 && (
        <div className="relative">
          <button
            type="button"
            onClick={() => setShowTemplatePicker(!showTemplatePicker)}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-blue-50 text-blue-600 hover:bg-blue-100 text-sm font-bold transition-colors"
          >
            <ListFilter className="w-4 h-4" />
            Chọn từ mẫu có sẵn
          </button>

          {showTemplatePicker && (
            <div className="absolute z-30 top-full left-0 mt-1.5 w-80 bg-white border border-slate-200 rounded-xl shadow-lg overflow-hidden">
              <div className="p-2 border-b border-slate-100">
                <input
                  className={inputCls}
                  placeholder="Tìm mẫu..."
                  value={templateSearch}
                  onChange={(e) => setTemplateSearch(e.target.value)}
                  autoFocus
                />
              </div>
              <div className="max-h-56 overflow-y-auto">
                {filteredTemplates.length === 0 ? (
                  <p className="text-xs text-slate-400 text-center py-4">Không có mẫu phù hợp</p>
                ) : (
                  filteredTemplates.map((tpl) => (
                    <button
                      key={tpl.id}
                      type="button"
                      onClick={() => applyTemplate(tpl)}
                      className="w-full text-left px-3 py-2.5 hover:bg-slate-50 border-b border-slate-50 last:border-b-0 transition-colors"
                    >
                      <span className="text-sm font-bold text-slate-700">{tpl.name}</span>
                      <span className="ml-2 text-[10px] font-medium text-slate-400 uppercase">
                        {tpl.source === "ADMIN" ? "Hệ thống" : "Của bạn"}
                      </span>
                      <div className="flex flex-wrap gap-1 mt-1">
                        {tpl.values.slice(0, 8).map((v) => (
                          <span
                            key={v.id}
                            className="px-1.5 py-0.5 rounded bg-slate-100 text-[10px] font-medium text-slate-500"
                          >
                            {v.value}
                          </span>
                        ))}
                        {tpl.values.length > 8 && (
                          <span className="text-[10px] text-slate-400">+{tpl.values.length - 8}</span>
                        )}
                      </div>
                    </button>
                  ))
                )}
              </div>
            </div>
          )}
        </div>
      )}

      {options.map((opt, i) => (
        <div key={i} className="p-4 rounded-xl border border-slate-100 bg-slate-50/50 space-y-3">
          <div className="flex items-center gap-2">
            <span className="text-xs font-black text-slate-400 uppercase tracking-widest w-24 shrink-0">
              Phân loại {i + 1}
            </span>
            <input
              className={inputCls}
              placeholder="VD: Màu sắc, Kích thước..."
              value={opt.name}
              onChange={(e) => updateName(i, e.target.value)}
            />
            <button
              type="button"
              onClick={() => removeOption(i)}
              className="p-1.5 rounded-lg hover:bg-red-50 text-slate-400 hover:text-red-500"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>

          {/* Values */}
          <div className="flex flex-wrap gap-2">
            {opt.values.map((val, vi) => (
              <div key={vi} className="flex flex-col items-center gap-1">
                {/* Image upload for first option only */}
                {i === 0 && (
                  <div className="relative group">
                    {val.imageUrl ? (
                      <div className="relative">
                        <img src={val.imageUrl} alt="" className="w-14 h-14 rounded-lg object-cover border border-slate-200" />
                        <button
                          type="button"
                          onClick={() => removeValueImage(i, vi)}
                          className="absolute -top-1 -right-1 p-0.5 bg-red-500 text-white rounded-full opacity-0 group-hover:opacity-100 transition-opacity shadow"
                        >
                          <X className="w-2.5 h-2.5" />
                        </button>
                      </div>
                    ) : (
                      <label className="w-14 h-14 rounded-lg border-2 border-dashed border-slate-200 bg-white flex flex-col items-center justify-center gap-0.5 text-slate-400 hover:border-red-300 hover:text-red-500 transition-colors cursor-pointer">
                        <ImageIcon className="w-3.5 h-3.5" />
                        <span className="text-[8px] font-bold">Ảnh</span>
                        <input
                          type="file"
                          accept="image/*"
                          className="hidden"
                          onChange={(e) => {
                            const f = e.target.files?.[0];
                            if (f) updateValueImage(i, vi, f);
                            e.target.value = "";
                          }}
                        />
                      </label>
                    )}
                  </div>
                )}
                <span className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-white border border-slate-200 text-xs font-semibold text-slate-600">
                  {val.value}
                  <button
                    type="button"
                    onClick={() => removeValue(i, vi)}
                    className="text-slate-300 hover:text-red-500"
                  >
                    <X className="w-3 h-3" />
                  </button>
                </span>
              </div>
            ))}
          </div>

          {/* Add value input */}
          <div className="flex items-center gap-2">
            <input
              className={`${inputCls} max-w-xs`}
              placeholder="Thêm giá trị..."
              value={newValueInputs[i] || ""}
              onChange={(e) => setNewValueInputs((prev) => ({ ...prev, [i]: e.target.value }))}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  addValue(i);
                }
              }}
            />
            <button
              type="button"
              onClick={() => addValue(i)}
              className="px-3 py-2 rounded-xl bg-slate-200 hover:bg-slate-300 text-xs font-bold text-slate-600"
            >
              Thêm
            </button>
          </div>
        </div>
      ))}

      {options.length < 3 && (
        <button
          type="button"
          onClick={addOption}
          className="flex items-center gap-1.5 px-4 py-2 rounded-xl border-2 border-dashed border-slate-200 text-sm font-bold text-slate-400 hover:border-red-300 hover:text-red-500 transition-colors"
        >
          <Plus className="w-4 h-4" />
          Thêm phân loại (tối đa 3)
        </button>
      )}
    </div>
  );
}

// ─── SKU table ─────────────────────────────────────────────────────────────────

interface SkuRow extends SkuRequest {
  _key: string; // synthetic key for React
  // inventory fields
  invTotalStock?: number;
  invLowStockThreshold?: number;
  invLocationCode?: string;
}

function SkuTable({
  skus,
  onChange,
}: {
  skus: SkuRow[];
  onChange: (skus: SkuRow[]) => void;
}) {
  const [collapsed, setCollapsed] = useState<Record<string, boolean>>({});

  const toggleCollapse = (key: string) =>
    setCollapsed((prev) => ({ ...prev, [key]: !prev[key] }));
  const collapseAll = () => {
    const map: Record<string, boolean> = {};
    skus.forEach((s) => { map[s._key] = true; });
    setCollapsed(map);
  };
  const expandAll = () => setCollapsed({});
  const update = (index: number, field: keyof SkuRow, value: string | number) => {
    const next = [...skus];
    next[index] = { ...next[index], [field]: value };
    onChange(next);
  };

  return (
    <div className="overflow-x-auto">
      {skus.length === 0 ? (
        <p className="text-sm text-slate-400 text-center py-6">
          Thêm phân loại hàng ở trên để tự động tạo các biến thể SKU
        </p>
      ) : (
        <div className="space-y-3">
          {/* Collapse/Expand controls */}
          {skus.length > 1 && (
            <div className="flex justify-end gap-2 mb-1">
              <button type="button" onClick={expandAll} className="text-[11px] text-blue-600 hover:underline">Mở tất cả</button>
              <span className="text-slate-300">|</span>
              <button type="button" onClick={collapseAll} className="text-[11px] text-blue-600 hover:underline">Thu gọn tất cả</button>
            </div>
          )}
          {skus.map((sku, i) => (
            <div key={sku._key} className="rounded-xl border border-slate-100 bg-slate-50/50 hover:border-slate-200 transition-colors">
              {/* Collapsible header */}
              <button
                type="button"
                onClick={() => toggleCollapse(sku._key)}
                className="w-full flex items-center justify-between p-3 text-left"
              >
                <div className="flex items-center gap-2">
                  <span className="text-xs font-black text-slate-400 uppercase tracking-wider">#{i + 1}</span>
                  <div className="flex flex-wrap gap-1">
                    {Object.entries(sku.selectionAttributes).map(([k, v]) => (
                      <span key={k} className="px-2 py-0.5 rounded-lg bg-red-50 text-[11px] font-bold text-red-600 border border-red-100">
                        {k}: {v}
                      </span>
                    ))}
                  </div>
                  {sku.price > 0 && (
                    <span className="text-[11px] text-slate-500 ml-2">
                      {sku.price.toLocaleString()}đ • Kho: {sku.invTotalStock ?? 0}
                    </span>
                  )}
                </div>
                <ChevronRight className={`w-4 h-4 text-slate-400 transition-transform ${!collapsed[sku._key] ? "rotate-90" : ""}`} />
              </button>

              {!collapsed[sku._key] && (
              <div className="px-4 pb-4">
              <div className="grid grid-cols-1 gap-4">
                {/* Fields grid */}
                <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-2">
                  <div>
                    <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">SKU</label>
                    <input
                      className="w-full px-2 py-1.5 rounded-lg border border-slate-200 bg-white text-xs font-mono outline-none focus:border-red-400"
                      value={sku.skuCode}
                      onChange={(e) => update(i, "skuCode", e.target.value)}
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Giá bán *</label>
                    <input
                      type="number"
                      min={0}
                      className="w-full px-2 py-1.5 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                      value={sku.price || ""}
                      onChange={(e) => update(i, "price", parseFloat(e.target.value) || 0)}
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Giá gốc</label>
                    <input
                      type="number"
                      min={0}
                      className="w-full px-2 py-1.5 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                      value={sku.originalPrice || ""}
                      onChange={(e) => update(i, "originalPrice", parseFloat(e.target.value) || 0)}
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Giá nhập</label>
                    <input
                      type="number"
                      min={0}
                      className="w-full px-2 py-1.5 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                      value={sku.costPrice || ""}
                      onChange={(e) => update(i, "costPrice", parseFloat(e.target.value) || 0)}
                    />
                    {(sku.costPrice ?? 0) > 0 && sku.price > 0 && (
                      <p className={`text-[10px] mt-0.5 font-bold ${sku.price - (sku.costPrice ?? 0) >= 0 ? "text-green-600" : "text-red-500"}`}>
                        LN: {((sku.price - (sku.costPrice ?? 0)) / sku.price * 100).toFixed(1)}%
                      </p>
                    )}
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Cân nặng (g)</label>
                    <input
                      type="number"
                      min={0}
                      className="w-full px-2 py-1.5 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                      value={sku.weightGram || ""}
                      onChange={(e) => update(i, "weightGram", parseInt(e.target.value) || 0)}
                    />
                  </div>
                  <div className="col-span-2 sm:col-span-3 lg:col-span-1">
                    <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Kích thước (cm)</label>
                    <div className="flex gap-1">
                      <input
                        type="number"
                        min={0}
                        placeholder="D"
                        title="Dài (cm)"
                        className="w-full px-1.5 py-1.5 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400 text-center"
                        value={sku.lengthCm || ""}
                        onChange={(e) => update(i, "lengthCm", parseInt(e.target.value) || 0)}
                      />
                      <input
                        type="number"
                        min={0}
                        placeholder="R"
                        title="Rộng (cm)"
                        className="w-full px-1.5 py-1.5 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400 text-center"
                        value={sku.widthCm || ""}
                        onChange={(e) => update(i, "widthCm", parseInt(e.target.value) || 0)}
                      />
                      <input
                        type="number"
                        min={0}
                        placeholder="C"
                        title="Cao (cm)"
                        className="w-full px-1.5 py-1.5 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400 text-center"
                        value={sku.heightCm || ""}
                        onChange={(e) => update(i, "heightCm", parseInt(e.target.value) || 0)}
                      />
                    </div>
                  </div>
                </div>
              </div>

              {/* Inventory fields */}
              <div className="mt-3 pt-3 border-t border-slate-100">
                <p className="text-[10px] font-bold text-slate-400 uppercase mb-2 flex items-center gap-1">
                  <Package className="w-3 h-3" /> Tồn kho ban đầu
                </p>
                <div className="grid grid-cols-3 gap-2">
                  <div>
                    <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Số lượng kho *</label>
                    <input
                      type="number"
                      min={0}
                      className="w-full px-2 py-1.5 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                      value={sku.invTotalStock || ""}
                      onChange={(e) => update(i, "invTotalStock", parseInt(e.target.value) || 0)}
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Cảnh báo hết hàng</label>
                    <input
                      type="number"
                      min={0}
                      className="w-full px-2 py-1.5 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                      value={sku.invLowStockThreshold ?? 5}
                      onChange={(e) => update(i, "invLowStockThreshold", parseInt(e.target.value) || 0)}
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Mã vị trí kho</label>
                    <input
                      className="w-full px-2 py-1.5 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                      placeholder="VD: A1-01"
                      value={sku.invLocationCode ?? ""}
                      onChange={(e) => update(i, "invLocationCode", e.target.value)}
                    />
                  </div>
                </div>
              </div>
              </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ─── Specification editor ──────────────────────────────────────────────────────

function SpecificationsEditor({
  attributes,
  specs,
  onChange,
  errors,
}: {
  attributes: CategoryAttributeResponse[];
  specs: Record<string, string>;
  onChange: (specs: Record<string, string>) => void;
  errors: Record<string, string>;
}) {
  const [customKey, setCustomKey] = useState("");
  const [customVal, setCustomVal] = useState("");

  const attributeNames = new Set(attributes.map((a) => a.value));
  const customSpecs = Object.entries(specs).filter(([key]) => !attributeNames.has(key));

  const updateSpec = (key: string, value: string) => {
    onChange({ ...specs, [key]: value });
  };

  const removeSpec = (key: string) => {
    const next = { ...specs };
    delete next[key];
    onChange(next);
  };

  const addCustom = () => {
    const k = customKey.trim();
    if (!k) return;
    onChange({ ...specs, [k]: customVal.trim() });
    setCustomKey("");
    setCustomVal("");
  };

  return (
    <div className="space-y-4">
      {/* Category attributes */}
      {attributes.length > 0 && (
        <div className="space-y-3">
          {attributes.map((attr) => {
            const hasError = !!errors[attr.value];
            return (
              <div key={attr.id}>
                <label className="flex items-center gap-1 text-xs font-bold text-slate-500 uppercase tracking-widest mb-1">
                  {attr.value}
                  {attr.required && <span className="text-red-500">*</span>}
                  {attr.filterable && (
                    <span className="text-[9px] px-1 py-0.5 bg-blue-50 text-blue-500 rounded font-bold normal-case">
                      lọc
                    </span>
                  )}
                </label>

                {attr.dataType === "enum" && attr.predefinedValues.length > 0 ? (
                  <select
                    className={`${inputCls} ${hasError ? "border-red-400 bg-red-50" : ""}`}
                    value={specs[attr.value] || ""}
                    onChange={(e) => updateSpec(attr.value, e.target.value)}
                  >
                    <option value="">-- Chọn --</option>
                    {attr.predefinedValues.map((pv) => (
                      <option key={pv.id} value={pv.value}>
                        {pv.value}
                      </option>
                    ))}
                  </select>
                ) : attr.dataType === "boolean" ? (
                  <select
                    className={`${inputCls} ${hasError ? "border-red-400 bg-red-50" : ""}`}
                    value={specs[attr.value] || ""}
                    onChange={(e) => updateSpec(attr.value, e.target.value)}
                  >
                    <option value="">-- Chọn --</option>
                    <option value="true">Có</option>
                    <option value="false">Không</option>
                  </select>
                ) : (
                  <input
                    type={attr.dataType === "number" ? "number" : "text"}
                    className={`${inputCls} ${hasError ? "border-red-400 bg-red-50" : ""}`}
                    placeholder={`Nhập ${attr.value.toLowerCase()}...`}
                    value={specs[attr.value] || ""}
                    onChange={(e) => updateSpec(attr.value, e.target.value)}
                  />
                )}

                {hasError && (
                  <p className="text-xs text-red-500 mt-0.5 flex items-center gap-1">
                    <AlertCircle className="w-3 h-3" />
                    {errors[attr.value]}
                  </p>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Custom specs */}
      {customSpecs.length > 0 && (
        <div className="space-y-2">
          <p className="text-xs font-bold text-slate-400 uppercase tracking-widest">Thông số tùy chỉnh</p>
          {customSpecs.map(([key, val]) => (
            <div key={key} className="flex items-center gap-2">
              <input className={`${inputBase} w-1/3 shrink-0`} value={key} readOnly />
              <input
                className={`${inputBase} flex-1 min-w-0`}
                value={val}
                onChange={(e) => updateSpec(key, e.target.value)}
              />
              <button
                type="button"
                onClick={() => removeSpec(key)}
                className="p-1.5 rounded-lg hover:bg-red-50 text-slate-400 hover:text-red-500"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Add custom spec */}
      <div className="flex items-center gap-2">
        <input
          className={`${inputBase} w-1/3 shrink-0`}
          placeholder="Tên thông số"
          value={customKey}
          onChange={(e) => setCustomKey(e.target.value)}
        />
        <input
          className={`${inputBase} flex-1 min-w-0`}
          placeholder="Giá trị"
          value={customVal}
          onChange={(e) => setCustomVal(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              addCustom();
            }
          }}
        />
        <button
          type="button"
          onClick={addCustom}
          className="px-3 py-2 rounded-xl bg-slate-200 hover:bg-slate-300 text-xs font-bold text-slate-600 shrink-0"
        >
          Thêm
        </button>
      </div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════════════════
// Main page
// ═══════════════════════════════════════════════════════════════════════════════

export default function SellerAddProductPage() {
  const navigate = useNavigate();

  // ── Global state ──
  const [sellerId, setSellerId] = useState("");
  const [saving, setSaving] = useState(false);

  // ── Basic info ──
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const descriptionRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (!descriptionRef.current) return;
    descriptionRef.current.style.height = "auto";
    descriptionRef.current.style.height = `${descriptionRef.current.scrollHeight}px`;
  }, [description]);

  // ── Category ──
  const [categoryTree, setCategoryTree] = useState<CategoryTreeResponse[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<CategoryTreeResponse | null>(null);
  const [categoryAttrs, setCategoryAttrs] = useState<CategoryAttributeResponse[]>([]);
  const [loadingAttrs, setLoadingAttrs] = useState(false);

  // ── Images ──
  const [primaryImage, setPrimaryImage] = useState("");
  const [galleryImages, setGalleryImages] = useState<string[]>([]);

  // ── Options & SKUs ──
  const [options, setOptions] = useState<OptionRow[]>([]);
  const [skus, setSkus] = useState<SkuRow[]>([]);

  // ── Specifications ──
  const [specs, setSpecs] = useState<Record<string, string>>({});
  const [specErrors, setSpecErrors] = useState<Record<string, string>>({});

  // ── Apply price/stock to all SKUs ──
  const [bulkPrice, setBulkPrice] = useState("");
  const [bulkStock, setBulkStock] = useState("");
  const [bulkLowStockThreshold, setBulkLowStockThreshold] = useState("");
  const [bulkLocationCode, setBulkLocationCode] = useState("");
  const [bulkOriginalPrice, setBulkOriginalPrice] = useState("");
  const [bulkCostPrice, setBulkCostPrice] = useState("");
  const [bulkWeight, setBulkWeight] = useState("");
  const [bulkLength, setBulkLength] = useState("");
  const [bulkWidth, setBulkWidth] = useState("");
  const [bulkHeight, setBulkHeight] = useState("");

  // ── Created product for preview link ──
  const [createdProductId, setCreatedProductId] = useState<string | null>(null);

  // ── Init ──
  useEffect(() => {
    Promise.all([
      getMySellerProfile(),
      adminGetCategoryTree(),
    ]).then(([profile, tree]) => {
      setSellerId(profile.id);
      setCategoryTree(tree);
    });
  }, []);

  // ── Load category attributes when category changes ──
  useEffect(() => {
    if (!selectedCategory) {
      setCategoryAttrs([]);
      return;
    }
    setLoadingAttrs(true);
    adminGetCategoryAttributes(selectedCategory.id)
      .then((attrs) => setCategoryAttrs(attrs))
      .catch(() => setCategoryAttrs([]))
      .finally(() => setLoadingAttrs(false));
  }, [selectedCategory]);

  // ── Regenerate SKUs whenever options change ──
  useEffect(() => {
    const validOptions = options.filter((o) => o.name.trim() && o.values.length > 0);
    if (validOptions.length === 0) {
      setSkus([]);
      return;
    }

    const combos = cartesian(...validOptions.map((o) => o.values.map((v) => v.value)));
    const newSkus: SkuRow[] = combos.map((combo, idx) => {
      const attrs: Record<string, string> = {};
      validOptions.forEach((opt, oi) => {
        attrs[opt.name] = combo[oi];
      });
      // Try to preserve existing data for same combination
      const key = Object.values(attrs).join("|");
      const existing = skus.find((s) => s._key === key);
      return {
        _key: key,
        skuCode: existing?.skuCode || buildSkuCode(name, attrs, idx),
        price: existing?.price || 0,
        originalPrice: existing?.originalPrice || 0,
        costPrice: existing?.costPrice || 0,
        weightGram: existing?.weightGram || 0,
        lengthCm: existing?.lengthCm || 0,
        widthCm: existing?.widthCm || 0,
        heightCm: existing?.heightCm || 0,
        selectionAttributes: attrs,
        invTotalStock: existing?.invTotalStock ?? 0,
        invLowStockThreshold: existing?.invLowStockThreshold ?? 5,
        invLocationCode: existing?.invLocationCode ?? "",
      };
    });
    setSkus(newSkus);
  }, [options]); // eslint-disable-line react-hooks/exhaustive-deps

  // ── Apply bulk values to all SKUs ──
  const applyBulkAll = () => {
    setSkus((prev) => {
      let updated = [...prev];
      const p = parseFloat(bulkPrice);
      if (!isNaN(p) && p >= 0) updated = updated.map((s) => ({ ...s, price: p }));
      const op = parseFloat(bulkOriginalPrice);
      if (!isNaN(op) && op >= 0) updated = updated.map((s) => ({ ...s, originalPrice: op }));
      const cp = parseFloat(bulkCostPrice);
      if (!isNaN(cp) && cp >= 0) updated = updated.map((s) => ({ ...s, costPrice: cp }));
      const st = parseInt(bulkStock);
      if (!isNaN(st) && st >= 0) updated = updated.map((s) => ({ ...s, invTotalStock: st }));
      const lst = parseInt(bulkLowStockThreshold);
      if (!isNaN(lst) && lst >= 0) updated = updated.map((s) => ({ ...s, invLowStockThreshold: lst }));
      if (bulkLocationCode.trim()) updated = updated.map((s) => ({ ...s, invLocationCode: bulkLocationCode.trim() }));
      const wg = parseInt(bulkWeight);
      if (!isNaN(wg) && wg >= 0) updated = updated.map((s) => ({ ...s, weightGram: wg }));
      const l = bulkLength ? parseInt(bulkLength) : NaN;
      if (!isNaN(l)) updated = updated.map((s) => ({ ...s, lengthCm: l }));
      const w = bulkWidth ? parseInt(bulkWidth) : NaN;
      if (!isNaN(w)) updated = updated.map((s) => ({ ...s, widthCm: w }));
      const h = bulkHeight ? parseInt(bulkHeight) : NaN;
      if (!isNaN(h)) updated = updated.map((s) => ({ ...s, heightCm: h }));
      return updated;
    });
  };

  // ── Validate & Submit ──
  const handleSubmit = async () => {
    setSpecErrors({});

    // Basic validations
    if (!name.trim() || name.trim().length < 10) {
      toast.error("Tên sản phẩm phải có ít nhất 10 ký tự");
      return;
    }
    if (!selectedCategory) {
      toast.error("Vui lòng chọn danh mục sản phẩm (phải chọn danh mục cuối cùng)");
      return;
    }
    if (!primaryImage) {
      toast.error("Vui lòng tải lên ảnh chính của sản phẩm");
      return;
    }
    if (skus.length === 0) {
      toast.error("Vui lòng thêm ít nhất 1 phân loại hàng để tạo biến thể SKU");
      return;
    }

    // Validate SKUs — auto-fill empty SKU codes
    const finalSkus = skus.map((sku, idx) => ({
      ...sku,
      skuCode: sku.skuCode.trim() || buildSkuCode(name, sku.selectionAttributes, idx),
    }));
    setSkus(finalSkus);

    for (const sku of finalSkus) {
      if (sku.price <= 0) {
        toast.error(`Giá bán của biến thể "${Object.values(sku.selectionAttributes).join(", ")}" phải lớn hơn 0`);
        return;
      }
      if ((sku.invTotalStock ?? 0) < 0) {
        toast.error("Số lượng kho không được âm");
        return;
      }
    }

    // Validate required specifications
    const newSpecErrors: Record<string, string> = {};
    for (const attr of categoryAttrs) {
      if (attr.required && !specs[attr.value]?.trim()) {
        newSpecErrors[attr.value] = `${attr.value} là bắt buộc`;
      }
    }
    if (Object.keys(newSpecErrors).length > 0) {
      setSpecErrors(newSpecErrors);
      toast.error("Vui lòng điền đầy đủ các thông số kỹ thuật bắt buộc");
      return;
    }

    // Build images array
    const images = [
      { url: primaryImage, isPrimary: true, sortOrder: 0 },
      ...galleryImages.map((url, i) => ({ url, isPrimary: false, sortOrder: i + 1 })),
    ];

    // Build clean specifications (convert to array of {name, value})
    const cleanSpecs: { name: string; value: string }[] = [];
    for (const [k, v] of Object.entries(specs)) {
      if (v.trim()) cleanSpecs.push({ name: k, value: v.trim() });
    }

    const payload: CreateProductRequest = {
      name: name.trim(),
      sellerId,
      categoryId: selectedCategory.id,
      description: description.trim() || undefined,
      images,
      options: options
        .filter((o) => o.name.trim() && o.values.length > 0)
        .map((o) => ({ name: o.name.trim(), values: o.values.map((v) => ({ value: v.value, imageUrl: v.imageUrl || undefined })) })),
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      skus: finalSkus.map(({ _key, invLowStockThreshold, invLocationCode, ...rest }) => ({
        skuCode: rest.skuCode,
        price: rest.price,
        originalPrice: rest.originalPrice || undefined,
        costPrice: rest.costPrice || undefined,
        weightGram: rest.weightGram || undefined,
        lengthCm: rest.lengthCm || undefined,
        widthCm: rest.widthCm || undefined,
        heightCm: rest.heightCm || undefined,
        selectionAttributes: rest.selectionAttributes,
        totalStock: rest.invTotalStock ?? 0,
        lowStockThreshold: invLowStockThreshold ?? 5,
        locationCode: invLocationCode || undefined,
      })),
      specifications: cleanSpecs,
    };

    setSaving(true);
    try {
      const product = await createProduct(payload);

      setCreatedProductId(product.id);
      toast.success("Tạo sản phẩm thành công!");
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        "Tạo sản phẩm thất bại. Vui lòng thử lại.";
      toast.error(msg);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="w-full space-y-5 pb-10">
      {/* Header */}
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={() => navigate("/seller/products")}
          className="p-2 rounded-xl border border-slate-200 hover:bg-slate-50"
        >
          <ArrowLeft className="w-4 h-4 text-slate-600" />
        </button>
        <div>
          <h1 className="text-xl font-black text-slate-900">Thêm sản phẩm mới</h1>
          <p className="text-xs text-slate-400 mt-0.5">Điền đầy đủ thông tin sản phẩm</p>
        </div>
      </div>

      {/* Success message with preview link */}
      {createdProductId && (
        <div className="flex items-center gap-3 px-5 py-4 rounded-2xl bg-green-50 border border-green-200">
          <div className="w-10 h-10 rounded-full bg-green-100 flex items-center justify-center shrink-0">
            <Package className="w-5 h-5 text-green-600" />
          </div>
          <div className="flex-1">
            <p className="text-sm font-bold text-green-800">Tạo sản phẩm thành công!</p>
            <p className="text-xs text-green-600 mt-0.5">Sản phẩm đã được tạo ở trạng thái nháp.</p>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => navigate(`/product/${createdProductId}`)}
              className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-green-600 hover:bg-green-700 text-white text-sm font-bold transition-colors shadow-sm"
            >
              <Eye className="w-4 h-4" />
              Xem trước
            </button>
            <button
              type="button"
              onClick={() => navigate("/seller/products")}
              className="px-4 py-2 rounded-xl border border-green-300 text-green-700 text-sm font-bold hover:bg-green-100 transition-colors"
            >
              Quản lý sản phẩm
            </button>
            <button
              type="button"
              onClick={() => {
                setCreatedProductId(null);
                setName("");
                setDescription("");
                setSelectedCategory(null);
                setPrimaryImage("");
                setGalleryImages([]);
                setOptions([]);
                setSkus([]);
                setSpecs({});
              }}
              className="px-4 py-2 rounded-xl border border-green-300 text-green-700 text-sm font-bold hover:bg-green-100 transition-colors"
            >
              Tạo sản phẩm khác
            </button>
          </div>
        </div>
      )}

      {/* ─── 1. Basic info ─── */}
      <SectionCard title="Thông tin cơ bản" subtitle="Tên và mô tả sản phẩm">
        <div className="space-y-3">
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">
              Tên sản phẩm *
            </label>
            <input
              autoFocus
              className={inputCls}
              placeholder="Nhập tên sản phẩm (ít nhất 10 ký tự)..."
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
            <p className="text-[11px] text-slate-400 mt-1">{name.length} ký tự</p>
          </div>
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">
              Mô tả
            </label>
            <textarea
              ref={descriptionRef}
              rows={4}
              className={`${inputCls} resize-none`}
              placeholder="Mô tả chi tiết về sản phẩm..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
        </div>
      </SectionCard>

      {/* ─── 2. Category ─── */}
      <SectionCard title="Danh mục" subtitle="Chọn danh mục cuối cùng (lá) trong cây danh mục">
        {categoryTree.length === 0 ? (
          <div className="flex items-center gap-2 text-slate-400 text-sm py-4">
            <Loader2 className="w-4 h-4 animate-spin" />
            Đang tải danh mục...
          </div>
        ) : (
          <CategoryPicker
            tree={categoryTree}
            selected={selectedCategory}
            onSelect={(cat, namePath) => {
              if (cat?.id) {
                (cat as CategoryTreeResponse & { _namePath?: string })._namePath = namePath;
                setSelectedCategory({ ...cat });
              } else {
                setSelectedCategory(null);
              }
              setSpecs({});
              setSpecErrors({});
            }}
          />
        )}
      </SectionCard>

      {/* ─── 3. Images ─── */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        <SectionCard title="Ảnh chính" subtitle="Ảnh hiển thị đầu tiên cho sản phẩm">
          <PrimaryImageUpload
            value={primaryImage}
            onChange={setPrimaryImage}
            folder="products"
          />
        </SectionCard>

        <SectionCard title="Bộ sưu tập" subtitle="Thêm ảnh phụ cho sản phẩm (tối đa 8)">
          <GalleryUpload
            images={galleryImages}
            onChange={setGalleryImages}
            folder="products"
            maxImages={8}
          />
        </SectionCard>
      </div>

      {/* ─── 4. Options & Variants ─── */}
      <SectionCard
        title="Phân loại hàng"
        subtitle="Thêm các phân loại (VD: Màu sắc, Kích thước) để tự động tạo biến thể SKU"
      >
        <OptionsEditor options={options} onChange={setOptions} folder="products/options" />
      </SectionCard>

      {/* ─── 5. SKU Table ─── */}
      {skus.length > 0 && (
        <SectionCard
          title={`Danh sách SKU (${skus.length} biến thể)`}
          subtitle="Chỉnh sửa giá, kho, kích thước, cân nặng, ảnh cho từng biến thể"
        >
          {/* Bulk actions */}
          <div className="mb-4 p-4 rounded-xl bg-slate-50 border border-slate-100 space-y-3">
            <div className="flex items-center gap-2">
              <Info className="w-4 h-4 text-slate-400 shrink-0" />
              <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Áp dụng cho tất cả biến thể</span>
            </div>

            {/* Row 1: Price, Original Price, Cost Price */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Giá bán</label>
                <input
                  type="number"
                  min={0}
                  placeholder="VD: 100000"
                  className="w-full px-2.5 py-2 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                  value={bulkPrice}
                  onChange={(e) => setBulkPrice(e.target.value)}
                />
              </div>
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Giá gốc</label>
                <input
                  type="number"
                  min={0}
                  placeholder="VD: 150000"
                  className="w-full px-2.5 py-2 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                  value={bulkOriginalPrice}
                  onChange={(e) => setBulkOriginalPrice(e.target.value)}
                />
              </div>
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Giá nhập</label>
                <input
                  type="number"
                  min={0}
                  placeholder="VD: 80000"
                  className="w-full px-2.5 py-2 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                  value={bulkCostPrice}
                  onChange={(e) => setBulkCostPrice(e.target.value)}
                />
                {bulkCostPrice && bulkPrice && parseFloat(bulkPrice) > 0 && (
                  <p className={`text-[10px] mt-0.5 font-bold ${parseFloat(bulkPrice) - parseFloat(bulkCostPrice) >= 0 ? "text-green-600" : "text-red-500"}`}>
                    Lợi nhuận: {((parseFloat(bulkPrice) - parseFloat(bulkCostPrice)) / parseFloat(bulkPrice) * 100).toFixed(1)}%
                    ({(parseFloat(bulkPrice) - parseFloat(bulkCostPrice)).toLocaleString()}đ)
                  </p>
                )}
              </div>
            </div>

            {/* Row 2: Inventory */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Số lượng kho</label>
                <input
                  type="number"
                  min={0}
                  placeholder="VD: 100"
                  className="w-full px-2.5 py-2 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                  value={bulkStock}
                  onChange={(e) => setBulkStock(e.target.value)}
                />
              </div>
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Cảnh báo hết hàng</label>
                <input
                  type="number"
                  min={0}
                  placeholder="VD: 5"
                  className="w-full px-2.5 py-2 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                  value={bulkLowStockThreshold}
                  onChange={(e) => setBulkLowStockThreshold(e.target.value)}
                />
              </div>
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Mã vị trí kho</label>
                <input
                  placeholder="VD: A1-01"
                  className="w-full px-2.5 py-2 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                  value={bulkLocationCode}
                  onChange={(e) => setBulkLocationCode(e.target.value)}
                />
              </div>
            </div>

            {/* Row 3: Weight & Dimensions */}
            <div className="grid grid-cols-1 sm:grid-cols-4 gap-3">
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Cân nặng (g)</label>
                <input
                  type="number"
                  min={0}
                  placeholder="VD: 500"
                  className="w-full px-2.5 py-2 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                  value={bulkWeight}
                  onChange={(e) => setBulkWeight(e.target.value)}
                />
              </div>
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Dài (cm)</label>
                <input
                  type="number"
                  min={0}
                  placeholder="VD: 30"
                  className="w-full px-2.5 py-2 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                  value={bulkLength}
                  onChange={(e) => setBulkLength(e.target.value)}
                />
              </div>
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Rộng (cm)</label>
                <input
                  type="number"
                  min={0}
                  placeholder="VD: 20"
                  className="w-full px-2.5 py-2 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                  value={bulkWidth}
                  onChange={(e) => setBulkWidth(e.target.value)}
                />
              </div>
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Cao (cm)</label>
                <input
                  type="number"
                  min={0}
                  placeholder="VD: 10"
                  className="w-full px-2.5 py-2 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                  value={bulkHeight}
                  onChange={(e) => setBulkHeight(e.target.value)}
                />
              </div>
            </div>

            {/* Apply all button */}
            <div className="flex justify-end pt-1">
              <button
                type="button"
                onClick={applyBulkAll}
                className="flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white text-xs font-bold shadow-sm transition-colors"
              >
                Áp dụng tất cả
              </button>
            </div>
          </div>

          <SkuTable skus={skus} onChange={setSkus} />
        </SectionCard>
      )}

      {/* ─── 6. Specifications ─── */}
      <SectionCard
        title="Thông số kỹ thuật"
        subtitle={
          selectedCategory
            ? loadingAttrs
              ? "Đang tải thông số từ danh mục..."
              : categoryAttrs.length > 0
              ? `${categoryAttrs.length} thông số từ danh mục "${selectedCategory.name}" — trường có dấu * là bắt buộc`
              : "Danh mục này không có thông số. Bạn có thể thêm thông số tùy chỉnh."
            : "Chọn danh mục trước để hiển thị các thông số bắt buộc"
        }
      >
        {loadingAttrs ? (
          <div className="flex items-center gap-2 text-slate-400 text-sm py-4">
            <Loader2 className="w-4 h-4 animate-spin" />
            Đang tải...
          </div>
        ) : (
          <SpecificationsEditor
            attributes={categoryAttrs}
            specs={specs}
            onChange={setSpecs}
            errors={specErrors}
          />
        )}
      </SectionCard>

      {/* ─── Submit ─── */}
      {!createdProductId && (
        <div className="flex items-center justify-end gap-3 pt-2">
          <button
            type="button"
            onClick={() => navigate("/seller/products")}
            className="px-5 py-2.5 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50"
          >
            Hủy
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={saving}
            className="flex items-center gap-2 px-6 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-60 shadow-lg shadow-red-200"
          >
            {saving && <Loader2 className="w-4 h-4 animate-spin" />}
            Tạo sản phẩm
          </button>
        </div>
      )}
    </div>
  );
}
