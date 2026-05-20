import { useEffect, useRef, useState, useCallback } from "react";
import type { Dispatch, SetStateAction } from "react";
import { useNavigate, useParams } from "react-router-dom";
import toast from "react-hot-toast";
import {
  ArrowLeft, ChevronRight, Loader2, Plus, Trash2, Upload, X,
  Image as ImageIcon, Info, AlertCircle, ListFilter, Save, Eye, Package,
} from "lucide-react";
import {
  getMySellerProfile,
  getAvailableOptionTemplates,
  updateProductBasicInfo,
  updateProductImages,
  updateProductSpecifications,
  updateProductSkus,
  updateSingleSku,
  getBatchInventories,
} from "../../api/sellerDashboardApi";
import { restock, adjustStock, updateInventory } from "../../api/inventoryApi";
import { getProductById } from "../../api/productApi";
import { adminGetCategoryTree, adminGetCategoryAttributes } from "../../api/adminApi";
import { uploadFile } from "../../api/fileApi";
import type { CategoryTreeResponse, CategoryAttributeResponse, OptionTemplateResponse } from "../../types/admin";
import type {
  ProductResponse, SkuRequest, OptionResponse,
  UpdateProductBasicInfoRequest, UpdateSingleSkuRequest,
} from "../../types/product";
import type { InventoryResponse } from "../../types/inventory";

// ─── helpers ───────────────────────────────────────────────────────────────────

function cartesian<T>(...arrays: T[][]): T[][] {
  return arrays.reduce<T[][]>(
    (acc, arr) => acc.flatMap((combo) => arr.map((val) => [...combo, val])),
    [[]]
  );
}

function removeDiacritics(str: string): string {
  return str.normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d").replace(/Đ/g, "D");
}

function buildSkuCode(productName: string, attrs: Record<string, string>, index: number): string {
  const prefix = removeDiacritics(productName)
    .replace(/[^a-zA-Z0-9]/g, "")
    .slice(0, 6)
    .toUpperCase();
  const suffix = Object.values(attrs)
    .map((v) => removeDiacritics(v).replace(/[^a-zA-Z0-9]/g, "").slice(0, 3).toUpperCase())
    .join("-");
  return `${prefix || "SKU"}-${suffix || String(index + 1).padStart(3, "0")}`;
}

// ─── Reusable section card ─────────────────────────────────────────────────────

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

// ─── Category Picker ───────────────────────────────────────────────────────────

function CategoryPicker({
  tree, selected, onSelect,
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
      const namePath = [...breadcrumb.map((b) => b.name), cat.name].join(" > ");
      onSelect(cat, namePath);
    }
  };

  const goBack = (index: number) => setBreadcrumb((prev) => prev.slice(0, index));

  return (
    <div>
      {breadcrumb.length > 0 && (
        <div className="flex items-center gap-1 text-xs mb-2 flex-wrap">
          <button type="button" onClick={() => goBack(0)} className="text-red-600 hover:underline font-semibold">Tất cả</button>
          {breadcrumb.map((bc, i) => (
            <span key={bc.id} className="flex items-center gap-1">
              <ChevronRight className="w-3 h-3 text-slate-300" />
              <button type="button" onClick={() => goBack(i + 1)} className="text-red-600 hover:underline font-semibold">{bc.name}</button>
            </span>
          ))}
        </div>
      )}
      <div className="max-h-60 overflow-y-auto border border-slate-100 rounded-xl">
        {currentList.length === 0 ? (
          <p className="text-xs text-slate-400 text-center py-4">Không có danh mục con</p>
        ) : (
          currentList.filter((c) => c.status === "ACTIVE").map((cat) => {
            const isLeaf = !cat.children || cat.children.length === 0;
            return (
              <button key={cat.id} type="button" onClick={() => handleClick(cat)}
                className={`flex items-center justify-between w-full px-4 py-2.5 text-sm hover:bg-slate-50 transition-colors border-b border-slate-50 last:border-0 ${
                  selected?.id === cat.id ? "bg-red-50 text-red-700 font-bold" : "text-slate-700"
                }`}
              >
                <span className="flex items-center gap-2">
                  {cat.iconUrl && <img src={cat.iconUrl} alt="" className="w-5 h-5 rounded object-cover" />}
                  {cat.name}
                  {isLeaf && <span className="text-[10px] bg-green-100 text-green-700 px-1.5 py-0.5 rounded-full font-bold">Chọn được</span>}
                </span>
                {!isLeaf && <ChevronRight className="w-4 h-4 text-slate-300" />}
              </button>
            );
          })
        )}
      </div>
      {selected && (
        <div className="mt-2 flex items-center gap-2 px-3 py-2 rounded-xl bg-red-50 border border-red-200">
          <Package className="w-4 h-4 text-red-500 shrink-0" />
          <span className="text-sm font-bold text-red-700 truncate">{selected.name}</span>
          <button type="button" onClick={() => onSelect(null as unknown as CategoryTreeResponse)} className="ml-auto p-0.5 rounded hover:bg-red-100">
            <X className="w-3.5 h-3.5 text-red-400" />
          </button>
        </div>
      )}
    </div>
  );
}

// ─── Multi-image gallery upload ────────────────────────────────────────────────

function GalleryUpload({ images, onChange, folder, maxImages }: {
  images: string[]; onChange: (urls: string[]) => void; folder: string; maxImages?: number;
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
    } catch (err) { console.error("Gallery upload failed:", err); }
    finally { setUploading(false); }
  };

  return (
    <div>
      <input ref={inputRef} type="file" accept="image/*" multiple className="hidden"
        onChange={(e) => { if (e.target.files) handleFiles(e.target.files); e.target.value = ""; }} />
      <div className="flex flex-wrap gap-3">
        {images.map((url, i) => (
          <div key={i} className="relative group">
            <img src={url} alt="" className="w-24 h-24 rounded-xl object-cover border border-slate-100" />
            <button type="button" onClick={() => onChange(images.filter((_, j) => j !== i))}
              className="absolute -top-1.5 -right-1.5 p-0.5 bg-red-500 text-white rounded-full opacity-0 group-hover:opacity-100 transition-opacity shadow">
              <X className="w-3 h-3" />
            </button>
          </div>
        ))}
        {(!maxImages || images.length < maxImages) && (
          <button type="button" onClick={() => inputRef.current?.click()} disabled={uploading}
            className="w-24 h-24 rounded-xl border-2 border-dashed border-slate-200 bg-slate-50 flex flex-col items-center justify-center gap-1 text-slate-400 hover:border-red-300 hover:text-red-500 transition-colors disabled:opacity-50">
            {uploading ? <Loader2 className="w-5 h-5 animate-spin" /> : <><Upload className="w-5 h-5" /><span className="text-[10px] font-bold">Thêm ảnh</span></>}
          </button>
        )}
      </div>
    </div>
  );
}

// ─── Primary image upload ──────────────────────────────────────────────────────

function PrimaryImageUpload({ value, onChange, folder }: {
  value: string; onChange: (url: string) => void; folder: string;
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
    } catch (err) { console.error("Upload failed:", err); }
    finally { setUploading(false); }
  };

  return (
    <div>
      <input ref={inputRef} type="file" accept="image/*" className="hidden"
        onChange={(e) => { const f = e.target.files?.[0]; if (f) handleFile(f); e.target.value = ""; }} />
      {value ? (
        <div className="relative inline-block group">
          <img src={value} alt="Primary" className="w-40 h-40 rounded-xl object-cover border-2 border-red-200" />
          <div className="absolute inset-0 bg-black/40 rounded-xl opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
            <button type="button" onClick={() => inputRef.current?.click()} disabled={uploading}
              className="px-2 py-1 bg-white rounded-lg text-xs font-bold text-slate-700 hover:bg-slate-100">
              {uploading ? "Đang tải..." : "Đổi ảnh"}
            </button>
            <button type="button" onClick={() => onChange("")}
              className="px-2 py-1 bg-red-500 rounded-lg text-xs font-bold text-white hover:bg-red-600">Xoá</button>
          </div>
          <span className="absolute top-1.5 left-1.5 px-1.5 py-0.5 bg-red-600 text-white text-[10px] font-bold rounded-md">Chính</span>
        </div>
      ) : (
        <button type="button" onClick={() => inputRef.current?.click()} disabled={uploading}
          className="w-40 h-40 rounded-xl border-2 border-dashed border-red-300 bg-red-50 flex flex-col items-center justify-center gap-2 text-red-400 hover:bg-red-100 transition-colors disabled:opacity-50">
          {uploading ? <Loader2 className="w-6 h-6 animate-spin" /> : <><ImageIcon className="w-8 h-8" /><span className="text-xs font-bold">Ảnh chính *</span></>}
        </button>
      )}
    </div>
  );
}

// ─── Option row ────────────────────────────────────────────────────────────────

interface OptionRow { name: string; values: { value: string; imageUrl: string }[]; }

function OptionsEditor({ options, onChange, folder }: { options: OptionRow[]; onChange: (opts: OptionRow[]) => void; folder: string }) {
  const [newValueInputs, setNewValueInputs] = useState<Record<number, string>>({});
  const [templates, setTemplates] = useState<OptionTemplateResponse[]>([]);
  const [showTemplatePicker, setShowTemplatePicker] = useState(false);
  const [templateSearch, setTemplateSearch] = useState("");

  useEffect(() => { getAvailableOptionTemplates().then(setTemplates).catch(() => {}); }, []);

  const filteredTemplates = templates.filter(
    (t) => t.name.toLowerCase().includes(templateSearch.toLowerCase()) && !options.some((o) => o.name.toLowerCase() === t.name.toLowerCase())
  );

  const applyTemplate = (tpl: OptionTemplateResponse) => {
    if (options.length >= 3) return;
    onChange([...options, { name: tpl.name, values: tpl.values.map((v) => ({ value: v.value, imageUrl: "" })) }]);
    setShowTemplatePicker(false);
    setTemplateSearch("");
  };

  const updateName = (i: number, name: string) => { const next = [...options]; next[i] = { ...next[i], name }; onChange(next); };

  const addValue = (i: number) => {
    const val = (newValueInputs[i] || "").trim();
    if (!val || options[i].values.some((v) => v.value === val)) return;
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

  return (
    <div className="space-y-4">
      {options.length < 3 && templates.length > 0 && (
        <div className="relative">
          <button type="button" onClick={() => setShowTemplatePicker(!showTemplatePicker)}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-blue-50 text-blue-600 hover:bg-blue-100 text-sm font-bold transition-colors">
            <ListFilter className="w-4 h-4" /> Chọn từ mẫu có sẵn
          </button>
          {showTemplatePicker && (
            <div className="absolute z-30 top-full left-0 mt-1.5 w-80 bg-white border border-slate-200 rounded-xl shadow-lg overflow-hidden">
              <div className="p-2 border-b border-slate-100">
                <input className={inputCls} placeholder="Tìm mẫu..." value={templateSearch}
                  onChange={(e) => setTemplateSearch(e.target.value)} autoFocus />
              </div>
              <div className="max-h-56 overflow-y-auto">
                {filteredTemplates.length === 0 ? (
                  <p className="text-xs text-slate-400 text-center py-4">Không có mẫu phù hợp</p>
                ) : (
                  filteredTemplates.map((tpl) => (
                    <button key={tpl.id} type="button" onClick={() => applyTemplate(tpl)}
                      className="w-full text-left px-3 py-2.5 hover:bg-slate-50 border-b border-slate-50 last:border-b-0 transition-colors">
                      <span className="text-sm font-bold text-slate-700">{tpl.name}</span>
                      <span className="ml-2 text-[10px] font-medium text-slate-400 uppercase">{tpl.source === "ADMIN" ? "Hệ thống" : "Của bạn"}</span>
                      <div className="flex flex-wrap gap-1 mt-1">
                        {tpl.values.slice(0, 8).map((v) => (
                          <span key={v.id} className="px-1.5 py-0.5 rounded bg-slate-100 text-[10px] font-medium text-slate-500">{v.value}</span>
                        ))}
                        {tpl.values.length > 8 && <span className="text-[10px] text-slate-400">+{tpl.values.length - 8}</span>}
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
            <span className="text-xs font-black text-slate-400 uppercase tracking-widest w-24 shrink-0">Phân loại {i + 1}</span>
            <input className={inputCls} placeholder="VD: Màu sắc, Kích thước..." value={opt.name} onChange={(e) => updateName(i, e.target.value)} />
            <button type="button" onClick={() => onChange(options.filter((_, j) => j !== i))}
              className="p-1.5 rounded-lg hover:bg-red-50 text-slate-400 hover:text-red-500"><Trash2 className="w-4 h-4" /></button>
          </div>
          <div className="flex flex-wrap gap-2">
            {opt.values.map((val, vi) => (
              <div key={vi} className="flex flex-col items-center gap-1">
                {i === 0 && (
                  <div className="relative group">
                    {val.imageUrl ? (
                      <div className="relative">
                        <img src={val.imageUrl} alt="" className="w-14 h-14 rounded-lg object-cover border border-slate-200" />
                        <button type="button" onClick={() => removeValueImage(i, vi)}
                          className="absolute -top-1 -right-1 p-0.5 bg-red-500 text-white rounded-full opacity-0 group-hover:opacity-100 transition-opacity shadow">
                          <X className="w-2.5 h-2.5" />
                        </button>
                      </div>
                    ) : (
                      <label className="w-14 h-14 rounded-lg border-2 border-dashed border-slate-200 bg-white flex flex-col items-center justify-center gap-0.5 text-slate-400 hover:border-red-300 hover:text-red-500 transition-colors cursor-pointer">
                        <ImageIcon className="w-3.5 h-3.5" />
                        <span className="text-[8px] font-bold">Ảnh</span>
                        <input type="file" accept="image/*" className="hidden"
                          onChange={(e) => { const f = e.target.files?.[0]; if (f) updateValueImage(i, vi, f); e.target.value = ""; }} />
                      </label>
                    )}
                  </div>
                )}
                <span className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-white border border-slate-200 text-xs font-semibold text-slate-600">
                  {val.value}
                  <button type="button" onClick={() => removeValue(i, vi)} className="text-slate-300 hover:text-red-500"><X className="w-3 h-3" /></button>
                </span>
              </div>
            ))}
          </div>
          <div className="flex items-center gap-2">
            <input className={`${inputCls} max-w-xs`} placeholder="Thêm giá trị..."
              value={newValueInputs[i] || ""} onChange={(e) => setNewValueInputs((prev) => ({ ...prev, [i]: e.target.value }))}
              onKeyDown={(e) => { if (e.key === "Enter") { e.preventDefault(); addValue(i); } }} />
            <button type="button" onClick={() => addValue(i)}
              className="px-3 py-2 rounded-xl bg-slate-200 hover:bg-slate-300 text-xs font-bold text-slate-600">Thêm</button>
          </div>
        </div>
      ))}
      {options.length < 3 && (
        <button type="button" onClick={() => onChange([...options, { name: "", values: [] }])}
          className="flex items-center gap-1.5 px-4 py-2 rounded-xl border-2 border-dashed border-slate-200 text-sm font-bold text-slate-400 hover:border-red-300 hover:text-red-500 transition-colors">
          <Plus className="w-4 h-4" /> Thêm phân loại (tối đa 3)
        </button>
      )}
    </div>
  );
}

// ─── SKU table ─────────────────────────────────────────────────────────────────

interface SkuRow extends SkuRequest {
  _key: string;
  id?: string;
}

/** Serialize a SkuRow to a comparable string for dirty detection */
function skuSnapshot(s: SkuRow): string {
  return JSON.stringify({
    skuCode: s.skuCode, price: s.price, originalPrice: s.originalPrice, costPrice: s.costPrice,
    weightGram: s.weightGram,
    lengthCm: s.lengthCm, widthCm: s.widthCm, heightCm: s.heightCm,
  });
}

/** Number input that stores raw text so users can clear the field and type freely */
function NumInput({ value, onChange, ...rest }: { value: number | undefined; onChange: (v: number) => void } & Omit<React.InputHTMLAttributes<HTMLInputElement>, "value" | "onChange" | "type">) {
  const [raw, setRaw] = useState(value != null && value !== 0 ? String(value) : "");
  const committed = useRef(value);

  useEffect(() => {
    if (value !== committed.current) {
      committed.current = value;
      setRaw(value != null && value !== 0 ? String(value) : "");
    }
  }, [value]);

  return (
    <input
      {...rest}
      type="number"
      value={raw}
      onChange={(e) => {
        setRaw(e.target.value);
        const n = e.target.value === "" ? 0 : Number(e.target.value);
        if (!isNaN(n)) { committed.current = n; onChange(n); }
      }}
    />
  );
}

function SkuTable({ skus, onChange, onSaveSingle, savingSkuId, snapshots }: {
  skus: SkuRow[];
  onChange: (skus: SkuRow[]) => void;
  onSaveSingle: (sku: SkuRow) => void;
  savingSkuId: string | null;
  snapshots: Map<string, string>;
}) {
  const [collapsed, setCollapsed] = useState<Record<string, boolean>>({});
  const toggleCollapse = (key: string) => setCollapsed((prev) => ({ ...prev, [key]: !prev[key] }));
  const collapseAll = () => { const m: Record<string, boolean> = {}; skus.forEach((s) => { m[s._key] = true; }); setCollapsed(m); };
  const expandAll = () => setCollapsed({});

  const update = (index: number, field: keyof SkuRow, value: string | number) => {
    const next = [...skus];
    next[index] = { ...next[index], [field]: value };
    onChange(next);
  };

  const cls = "w-full px-2 py-1.5 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400";

  return (
    <div className="overflow-x-auto">
      {skus.length === 0 ? (
        <p className="text-sm text-slate-400 text-center py-6">Thêm phân loại hàng ở trên để tự động tạo các biến thể SKU</p>
      ) : (
        <div className="space-y-3">
          {skus.length > 1 && (
            <div className="flex justify-end gap-2 mb-1">
              <button type="button" onClick={expandAll} className="text-[11px] text-blue-600 hover:underline">Mở tất cả</button>
              <span className="text-slate-300">|</span>
              <button type="button" onClick={collapseAll} className="text-[11px] text-blue-600 hover:underline">Thu gọn tất cả</button>
            </div>
          )}
          {skus.map((sku, i) => {
            const isDirty = sku.id ? skuSnapshot(sku) !== snapshots.get(sku._key) : false;
            return (
            <div key={sku._key} className="rounded-xl border border-slate-100 bg-slate-50/50 hover:border-slate-200 transition-colors">
              <div className="flex items-center justify-between p-3">
                <button type="button" onClick={() => toggleCollapse(sku._key)} className="flex items-center gap-2 text-left flex-1 min-w-0">
                  <span className="text-xs font-black text-slate-400 uppercase tracking-wider">#{i + 1}</span>
                  <div className="flex flex-wrap gap-1">
                    {Object.entries(sku.selectionAttributes).map(([k, v]) => (
                      <span key={k} className="px-2 py-0.5 rounded-lg bg-red-50 text-[11px] font-bold text-red-600 border border-red-100">{k}: {v}</span>
                    ))}
                  </div>
                  {sku.price > 0 && (
                    <span className="text-[11px] text-slate-500 ml-2 shrink-0">
                      {sku.price.toLocaleString()}đ
                    </span>
                  )}
                  <ChevronRight className={`w-4 h-4 text-slate-400 transition-transform shrink-0 ml-auto ${!collapsed[sku._key] ? "rotate-90" : ""}`} />
                </button>
                {sku.id && (
                  <button type="button" onClick={(e) => { e.stopPropagation(); onSaveSingle(sku); }}
                    disabled={!isDirty || savingSkuId === sku.id}
                    className={`ml-2 flex items-center gap-1 px-3 py-1.5 rounded-lg text-[11px] font-bold transition-colors shrink-0 ${
                      isDirty
                        ? "bg-red-600 hover:bg-red-700 text-white shadow-sm"
                        : "bg-slate-100 text-slate-400 cursor-not-allowed"
                    } disabled:opacity-50`}
                    title={!isDirty ? "Chưa có thay đổi" : `Lưu SKU ${sku.skuCode}`}>
                    {savingSkuId === sku.id ? <Loader2 className="w-3 h-3 animate-spin" /> : <Save className="w-3 h-3" />}
                    Lưu
                  </button>
                )}
              </div>

              {!collapsed[sku._key] && (
              <div className="px-4 pb-4">
              <div className="grid grid-cols-1 gap-4">
                <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-2">
                  <div>
                    <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">SKU</label>
                    <input className={`${cls} font-mono`}
                      value={sku.skuCode} onChange={(e) => update(i, "skuCode", e.target.value)} />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Giá bán *</label>
                    <NumInput min={0} className={cls}
                      value={sku.price} onChange={(v) => update(i, "price", v)} />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Giá gốc</label>
                    <NumInput min={0} className={cls}
                      value={sku.originalPrice} onChange={(v) => update(i, "originalPrice", v)} />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Giá nhập</label>
                    <NumInput min={0} className={cls}
                      value={sku.costPrice} onChange={(v) => update(i, "costPrice", v)} />
                    {(sku.costPrice ?? 0) > 0 && sku.price > 0 && (
                      <p className={`text-[10px] mt-0.5 font-bold ${sku.price - (sku.costPrice ?? 0) >= 0 ? "text-green-600" : "text-red-500"}`}>
                        LN: {((sku.price - (sku.costPrice ?? 0)) / sku.price * 100).toFixed(1)}%
                      </p>
                    )}
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Cân nặng (g)</label>
                    <NumInput min={0} className={cls}
                      value={sku.weightGram} onChange={(v) => update(i, "weightGram", v)} />
                  </div>
                  <div className="col-span-2 sm:col-span-3 lg:col-span-1">
                    <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Kích thước (cm)</label>
                    <div className="flex gap-1">
                      <NumInput min={0} placeholder="D" title="Dài (cm)"
                        className="w-full px-1.5 py-1.5 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400 text-center"
                        value={sku.lengthCm} onChange={(v) => update(i, "lengthCm", v)} />
                      <NumInput min={0} placeholder="R" title="Rộng (cm)"
                        className="w-full px-1.5 py-1.5 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400 text-center"
                        value={sku.widthCm} onChange={(v) => update(i, "widthCm", v)} />
                      <NumInput min={0} placeholder="C" title="Cao (cm)"
                        className="w-full px-1.5 py-1.5 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400 text-center"
                        value={sku.heightCm} onChange={(v) => update(i, "heightCm", v)} />
                    </div>
                  </div>
                </div>
              </div>
              </div>
              )}
            </div>
          );})}
        </div>
      )}
    </div>
  );
}

// ─── Specification editor ──────────────────────────────────────────────────────

function loadSkuInventories(
  skus: SkuRow[],
  setInventoryStates: Dispatch<SetStateAction<Record<string, {
    inventory: InventoryResponse | null;
    changeQty: number;
    editingTotalStock: number | null;
    editingThreshold: number | null;
    isEditing: boolean;
    saving: boolean;
    error: string | null;
    success: string | null;
  }>>>,
  setLoading: Dispatch<SetStateAction<boolean>>, 
  setError: Dispatch<SetStateAction<string | null>>,
) {
  const skuIds = skus.map((s) => s.id).filter(Boolean) as string[];
  if (skuIds.length === 0) {
    setInventoryStates({});
    setLoading(false);
    return;
  }
  setLoading(true);
  setError(null);
  getBatchInventories(skuIds)
    .then((inventories) => {
      const next: Record<string, {
        inventory: InventoryResponse | null;
        changeQty: number;
        editingTotalStock: number | null;
        editingThreshold: number | null;
        isEditing: boolean;
        saving: boolean;
        error: string | null;
        success: string | null;
      }> = {};
      skus.forEach((sku) => {
        if (!sku.id) return;
        const inv = inventories.find((item) => item.skuId === sku.id) ?? null;
        next[sku.id] = {
          inventory: inv,
          changeQty: 0,
          editingTotalStock: null,
          editingThreshold: null,
          isEditing: false,
          saving: false,
          error: null,
          success: null,
        };
      });
      setInventoryStates(next);
    })
    .catch(() => {
      setInventoryStates({});
      setError("Không thể tải dữ liệu tồn kho");
    })
    .finally(() => setLoading(false));
}

function SpecificationsEditor({ attributes, specs, onChange, errors }: {
  attributes: CategoryAttributeResponse[]; specs: Record<string, string>;
  onChange: (specs: Record<string, string>) => void; errors: Record<string, string>;
}) {
  const [customKey, setCustomKey] = useState("");
  const [customVal, setCustomVal] = useState("");
  const attributeNames = new Set(attributes.map((a) => a.value));
  const customSpecs = Object.entries(specs).filter(([key]) => !attributeNames.has(key));

  const updateSpec = (key: string, value: string) => onChange({ ...specs, [key]: value });
  const removeSpec = (key: string) => { const next = { ...specs }; delete next[key]; onChange(next); };
  const addCustom = () => {
    const k = customKey.trim();
    if (!k) return;
    onChange({ ...specs, [k]: customVal.trim() });
    setCustomKey(""); setCustomVal("");
  };

  return (
    <div className="space-y-4">
      {attributes.length > 0 && (
        <div className="space-y-3">
          {attributes.map((attr) => {
            const hasError = !!errors[attr.value];
            return (
              <div key={attr.id}>
                <label className="flex items-center gap-1 text-xs font-bold text-slate-500 uppercase tracking-widest mb-1">
                  {attr.value}
                  {attr.required && <span className="text-red-500">*</span>}
                  {attr.filterable && <span className="text-[9px] px-1 py-0.5 bg-blue-50 text-blue-500 rounded font-bold normal-case">lọc</span>}
                </label>
                {attr.dataType === "enum" && attr.predefinedValues.length > 0 ? (
                  <select className={`${inputCls} ${hasError ? "border-red-400 bg-red-50" : ""}`} value={specs[attr.value] || ""} onChange={(e) => updateSpec(attr.value, e.target.value)}>
                    <option value="">-- Chọn --</option>
                    {attr.predefinedValues.map((pv) => <option key={pv.id} value={pv.value}>{pv.value}</option>)}
                  </select>
                ) : attr.dataType === "boolean" ? (
                  <select className={`${inputCls} ${hasError ? "border-red-400 bg-red-50" : ""}`} value={specs[attr.value] || ""} onChange={(e) => updateSpec(attr.value, e.target.value)}>
                    <option value="">-- Chọn --</option>
                    <option value="true">Có</option>
                    <option value="false">Không</option>
                  </select>
                ) : (
                  <input type={attr.dataType === "number" ? "number" : "text"}
                    className={`${inputCls} ${hasError ? "border-red-400 bg-red-50" : ""}`}
                    placeholder={`Nhập ${attr.value.toLowerCase()}...`}
                    value={specs[attr.value] || ""} onChange={(e) => updateSpec(attr.value, e.target.value)} />
                )}
                {hasError && <p className="text-xs text-red-500 mt-0.5 flex items-center gap-1"><AlertCircle className="w-3 h-3" />{errors[attr.value]}</p>}
              </div>
            );
          })}
        </div>
      )}
      {customSpecs.length > 0 && (
        <div className="space-y-2">
          <p className="text-xs font-bold text-slate-400 uppercase tracking-widest">Thông số tùy chỉnh</p>
          {customSpecs.map(([key, val]) => (
            <div key={key} className="flex items-center gap-2">
              <input className={`${inputBase} w-1/3 shrink-0`} value={key} readOnly />
              <input className={`${inputBase} flex-1 min-w-0`} value={val} onChange={(e) => updateSpec(key, e.target.value)} />
              <button type="button" onClick={() => removeSpec(key)} className="p-1.5 rounded-lg hover:bg-red-50 text-slate-400 hover:text-red-500"><Trash2 className="w-4 h-4" /></button>
            </div>
          ))}
        </div>
      )}
      <div className="flex items-center gap-2">
        <input className={`${inputBase} w-1/3 shrink-0`} placeholder="Tên thông số" value={customKey} onChange={(e) => setCustomKey(e.target.value)} />
        <input className={`${inputBase} flex-1 min-w-0`} placeholder="Giá trị" value={customVal} onChange={(e) => setCustomVal(e.target.value)}
          onKeyDown={(e) => { if (e.key === "Enter") { e.preventDefault(); addCustom(); } }} />
        <button type="button" onClick={addCustom} className="px-3 py-2 rounded-xl bg-slate-200 hover:bg-slate-300 text-xs font-bold text-slate-600 shrink-0">Thêm</button>
      </div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════════════════
// Helper: reconstruct selectionAttributes from product options & sku attributes
// ═══════════════════════════════════════════════════════════════════════════════

function buildSelectionAttrsFromSku(
  skuAttrs: { optionName: string; valueName: string }[],
  productOptions: OptionResponse[],
): Record<string, string> {
  const attrs: Record<string, string> = {};
  // Primary: use optionName directly if available
  for (const a of skuAttrs) {
    if (a.optionName) {
      attrs[a.optionName] = a.valueName;
    }
  }
  // Fallback: for each attribute without optionName, try matching valueName against product options
  for (const a of skuAttrs) {
    if (!a.optionName && a.valueName && productOptions.length > 0) {
      for (const opt of productOptions) {
        if (!attrs[opt.name] && opt.values.some((v) => v.value === a.valueName)) {
          attrs[opt.name] = a.valueName;
          break;
        }
      }
    }
  }
  return attrs;
}

// ═══════════════════════════════════════════════════════════════════════════════
// Main page
// ═══════════════════════════════════════════════════════════════════════════════

export default function SellerEditProductPage() {
  const { id: productId } = useParams<{ id: string }>();
  const navigate = useNavigate();

  // ── Loading states ──
  const [loading, setLoading] = useState(true);

  // ── Per-section saving states ──
  const [savingBasic, setSavingBasic] = useState(false);
  const [savingImages, setSavingImages] = useState(false);
  const [savingSpecs, setSavingSpecs] = useState(false);
  const [savingSkus, setSavingSkus] = useState(false);
  const [savingSkuId, setSavingSkuId] = useState<string | null>(null);
  const [inventoryStates, setInventoryStates] = useState<Record<string, {
    inventory: InventoryResponse | null;
    changeQty: number;
    editingTotalStock: number | null;
    editingThreshold: number | null;
    isEditing: boolean;
    saving: boolean;
    error: string | null;
    success: string | null;
  }>>({});
  const [inventoryLoading, setInventoryLoading] = useState(false);
  const [inventoryError, setInventoryError] = useState<string | null>(null);

  // ── Original product data (for reference) ──
  const [product, setProduct] = useState<ProductResponse | null>(null);
  const [, setSellerId] = useState("");

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
  const optionsInitialized = useRef(false);
  const [skuSnapshots, setSkuSnapshots] = useState<Map<string, string>>(new Map());

  // ── Specifications ──
  const [specs, setSpecs] = useState<Record<string, string>>({});
  const [specErrors, setSpecErrors] = useState<Record<string, string>>({});

  // ── Bulk apply ──
  const [bulkPrice, setBulkPrice] = useState("");
  const [bulkOriginalPrice, setBulkOriginalPrice] = useState("");
  const [bulkCostPrice, setBulkCostPrice] = useState("");
  const [bulkWeight, setBulkWeight] = useState("");
  const [bulkLength, setBulkLength] = useState("");
  const [bulkWidth, setBulkWidth] = useState("");
  const [bulkHeight, setBulkHeight] = useState("");

  // ── Find category in tree by ID (recursive), returns category with name path ──
  const findCategoryById = useCallback((cats: CategoryTreeResponse[], catId: string, ancestors: string[] = []): (CategoryTreeResponse & { _namePath?: string }) | null => {
    for (const c of cats) {
      const currentPath = [...ancestors, c.name];
      if (c.id === catId) {
        return { ...c, _namePath: currentPath.join(" > ") };
      }
      if (c.children && c.children.length > 0) {
        const found = findCategoryById(c.children, catId, currentPath);
        if (found) return found;
      }
    }
    return null;
  }, []);

  // ── Load product + categories + profile ──
  useEffect(() => {
    if (!productId) return;
    setLoading(true);

    Promise.all([
      getProductById(productId),
      adminGetCategoryTree(),
      getMySellerProfile(),
    ]).then(([productResp, tree, profile]) => {
      const p = productResp.data.result;
      setProduct(p);
      setSellerId(profile.id);
      setCategoryTree(tree);

      // Basic info
      setName(p.name);
      setDescription(p.description || "");

      // Category
      const cat = findCategoryById(tree, p.categoryId);
      setSelectedCategory(cat);

      // Images
      const primary = p.images.find((img) => img.isPrimary);
      setPrimaryImage(primary?.url || (p.images[0]?.url ?? ""));
      setGalleryImages(p.images.filter((img) => !img.isPrimary).map((img) => img.url));

      // Options
      const opts: OptionRow[] = p.options.map((opt) => ({
        name: opt.name,
        values: opt.values.map((v) => ({ value: v.value, imageUrl: v.imageUrl || "" })),
      }));
      setOptions(opts);
      // NOTE: do NOT set optionsInitialized yet — defer to after render
      // so the options useEffect skips the initial load

      // SKUs — reconstruct from product data
      // Build cartesian combos from options for positional fallback
      const validOpts = opts.filter((o) => o.name.trim() && o.values.length > 0);
      const allCombos = validOpts.length > 0
        ? cartesian(...validOpts.map((o) => o.values.map((v) => v.value))).map((combo) => {
            const a: Record<string, string> = {};
            validOpts.forEach((opt, oi) => { a[opt.name] = combo[oi]; });
            return a;
          })
        : [];
      const usedComboKeys = new Set<string>();

      const skuRows: SkuRow[] = p.skus.map((sku, idx) => {
        let attrs = buildSelectionAttrsFromSku(
          sku.attributes.map((a) => ({ optionName: a.optionName, valueName: a.valueName })),
          p.options
        );

        // Positional fallback: if SKU has no attributes, assign from unmatched combos
        if (Object.keys(attrs).length === 0 && allCombos.length > 0) {
          const unmatched = allCombos.find((c) => !usedComboKeys.has(Object.values(c).join("|")));
          if (unmatched) attrs = unmatched;
        }

        const key = Object.values(attrs).join("|") || `sku-${idx}`;
        usedComboKeys.add(key);
        return {
          _key: key,
          id: sku.id,
          skuCode: sku.skuCode,
          price: sku.price,
          originalPrice: sku.originalPrice || 0,
          costPrice: sku.costPrice || 0,
          weightGram: sku.weightGram || 0,
          lengthCm: sku.lengthCm || 0,
          widthCm: sku.widthCm || 0,
          heightCm: sku.heightCm || 0,
          selectionAttributes: attrs,
        };
      });
      setSkus(skuRows);

      // Save initial snapshots for dirty detection
      const initialSnaps = new Map<string, string>();
      skuRows.forEach((r) => initialSnaps.set(r._key, skuSnapshot(r)));

      setSkuSnapshots(initialSnaps);

      // Allow options useEffect to regenerate SKUs only after initial load is done
      requestAnimationFrame(() => { optionsInitialized.current = true; });

      // Specifications
      const specMap: Record<string, string> = {};
      if (p.specifications && Array.isArray(p.specifications)) {
        for (const attr of p.specifications) {
          specMap[attr.name] = String(attr.value ?? "");
        }
      }
      setSpecs(specMap);
    }).catch((err) => {
      console.error("Failed to load product:", err);
      toast.error("Không thể tải thông tin sản phẩm");
    }).finally(() => setLoading(false));
  }, [productId, findCategoryById]);

  // ── Load category attributes when category changes ──
  useEffect(() => {
    if (!selectedCategory) { setCategoryAttrs([]); return; }
    setLoadingAttrs(true);
    adminGetCategoryAttributes(selectedCategory.id)
      .then((attrs) => setCategoryAttrs(attrs))
      .catch(() => setCategoryAttrs([]))
      .finally(() => setLoadingAttrs(false));
  }, [selectedCategory]);

  // ── Regenerate SKUs when options change (only after initial load) ──
  useEffect(() => {
    if (!optionsInitialized.current) return;

    const validOptions = options.filter((o) => o.name.trim() && o.values.length > 0);
    if (validOptions.length === 0) { setSkus([]); return; }

    const combos = cartesian(...validOptions.map((o) => o.values.map((v) => v.value)));
    const matchedKeys = new Set<string>();
    const newSkus: SkuRow[] = combos.map((combo, idx) => {
      const attrs: Record<string, string> = {};
      validOptions.forEach((opt, oi) => { attrs[opt.name] = combo[oi]; });
      const key = Object.values(attrs).join("|");

      // Primary match: by _key (attribute combo)
      let existing = skus.find((s) => s._key === key);

      // Fallback: grab any unmatched existing SKU that has real data (preserves prices/stock)
      if (!existing) {
        existing = skus.find((s) => !matchedKeys.has(s._key) && s.id);
      }
      if (existing) matchedKeys.add(existing._key);

      return {
        _key: key,
        id: existing?.id,
        skuCode: existing?.skuCode || buildSkuCode(name, attrs, idx),
        price: existing?.price || 0,
        originalPrice: existing?.originalPrice || 0,
        costPrice: existing?.costPrice || 0,
        weightGram: existing?.weightGram || 0,
        lengthCm: existing?.lengthCm || 0,
        widthCm: existing?.widthCm || 0,
        heightCm: existing?.heightCm || 0,
        selectionAttributes: attrs,
      };
    });
    setSkus(newSkus);
  }, [options]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const skuIds = skus.map((s) => s.id).filter(Boolean).join(",");
    if (!skuIds) {
      setInventoryStates({});
      return;
    }
    loadSkuInventories(skus, setInventoryStates, setInventoryLoading, setInventoryError);
  }, [skus.map((s) => s.id).join(",")]);

  const getSkuVariantImage = (sku: SkuRow) => {
    const primaryOption = options[0];
    if (!primaryOption) return product?.images.find((img) => img.isPrimary)?.url ?? null;
    const value = sku.selectionAttributes[primaryOption.name];
    const matched = primaryOption.values.find((v) => v.value === value);
    return matched?.imageUrl || product?.images.find((img) => img.isPrimary)?.url || null;
  };

  const getSkuVariantLabel = (sku: SkuRow) => {
    const values = Object.values(sku.selectionAttributes).filter(Boolean);
    return values.length > 0 ? values.join(" / ") : sku.skuCode;
  };

  // const adjustInventoryQuantity = (skuId: string, delta: number) => {
  //   setInventoryStates((prev) => {
  //     const state = prev[skuId];
  //     if (!state) return prev;
  //     return {
  //       ...prev,
  //       [skuId]: {
  //         ...state,
  //         changeQty: state.changeQty + delta,
  //         error: null,
  //         success: null,
  //       },
  //     };
  //   });
  // };

  // const setInventoryChangeQty = (skuId: string, value: number) => {
  //   setInventoryStates((prev) => {
  //     const state = prev[skuId];
  //     if (!state) return prev;
  //     return {
  //       ...prev,
  //       [skuId]: {
  //         ...state,
  //         changeQty: value,
  //         error: null,
  //         success: null,
  //       },
  //     };
  //   });
  // };

  const enterEditMode = (skuId: string) => {
    setInventoryStates((prev) => {
      const state = prev[skuId];
      if (!state?.inventory) return prev;
      return {
        ...prev,
        [skuId]: {
          ...state,
          isEditing: true,
          editingTotalStock: state.inventory.totalStock,
          editingThreshold: state.inventory.lowStockThreshold,
          error: null,
          success: null,
        },
      };
    });
  };

  const cancelEditMode = (skuId: string) => {
    setInventoryStates((prev) => {
      const state = prev[skuId];
      if (!state) return prev;
      return {
        ...prev,
        [skuId]: {
          ...state,
          isEditing: false,
          editingTotalStock: null,
          editingThreshold: null,
          changeQty: 0,
          error: null,
          success: null,
        },
      };
    });
  };

  const handleSaveInventory = async (sku: SkuRow) => {
    if (!sku.id) return;
    const state = inventoryStates[sku.id];
    if (!state) return;
    
    if (state.isEditing) {
      // Validate inputs
      if (state.editingTotalStock === null || state.editingTotalStock < 0) {
        setInventoryStates((prev) => ({
          ...prev,
          [sku.id!]: { ...state, error: "Tổng kho phải ≥ 0" },
        }));
        return;
      }
      if (state.editingThreshold === null || state.editingThreshold < 0) {
        setInventoryStates((prev) => ({
          ...prev,
          [sku.id!]: { ...state, error: "Ngưỡng thấp phải ≥ 0" },
        }));
        return;
      }
      
      const skuId = sku.id;
      setInventoryStates((prev) => ({
        ...prev,
        [skuId]: { ...state, saving: true, error: null, success: null },
      }));
      
      try {
        const currentTotal = state.inventory?.totalStock || 0;
        const totalChange = state.editingTotalStock - currentTotal;
        
        let updated = state.inventory;
        if (totalChange !== 0) {
          const resp = totalChange > 0
            ? await restock({ skuId, quantity: totalChange, note: "Điều chỉnh tồn kho" })
            : await adjustStock({ skuId, changeAmount: totalChange, note: "Điều chỉnh tồn kho" });
          updated = resp.data.result;
        }
        
        if (state.editingThreshold !== (state.inventory?.lowStockThreshold || 0)) {
          const resp = await updateInventory(skuId, { lowStockThreshold: state.editingThreshold });
          updated = resp.data.result;
        }
        
        setInventoryStates((prev) => ({
          ...prev,
          [skuId]: {
            inventory: updated,
            changeQty: 0,
            editingTotalStock: null,
            editingThreshold: null,
            isEditing: false,
            saving: false,
            error: null,
            success: "Đã cập nhật tồn kho",
          },
        }));
        
        setTimeout(() => {
          setInventoryStates((prev) => ({
            ...prev,
            [skuId]: prev[skuId] ? { ...prev[skuId], success: null } : prev[skuId],
          }));
        }, 3000);
      } catch {
        setInventoryStates((prev) => ({
          ...prev,
          [sku.id!]: { ...state, saving: false, error: "Cập nhật thất bại" },
        }));
      }
    } else if (state.changeQty === 0) {
      setInventoryStates((prev) => ({
        ...prev,
        [sku.id!]: { ...state, error: "Nhập số lượng khác 0" },
      }));
    } else {
      // Quick update mode - apply quantity change then enter edit mode
      const skuId = sku.id;
      setInventoryStates((prev) => ({
        ...prev,
        [skuId]: { ...state, saving: true, error: null, success: null },
      }));
      
      try {
        const quantity = state.changeQty;
        const resp = quantity > 0
          ? await restock({ skuId, quantity, note: "Điều chỉnh tồn kho" })
          : await adjustStock({ skuId, changeAmount: quantity, note: "Điều chỉnh tồn kho" });
        const updated = resp.data.result;
        
        setInventoryStates((prev) => ({
          ...prev,
          [skuId]: {
            inventory: updated,
            changeQty: 0,
            editingTotalStock: updated.totalStock,
            editingThreshold: updated.lowStockThreshold,
            isEditing: true,
            saving: false,
            error: null,
            success: null,
          },
        }));
      } catch {
        setInventoryStates((prev) => ({
          ...prev,
          [skuId]: { ...state, saving: false, error: "Cập nhật thất bại" },
        }));
      }
    }
  };

  // ── Bulk apply ──
  const applyBulkAll = () => {
    setSkus((prev) => {
      let updated = [...prev];
      const p = parseFloat(bulkPrice); if (!isNaN(p) && p >= 0) updated = updated.map((s) => ({ ...s, price: p }));
      const op = parseFloat(bulkOriginalPrice); if (!isNaN(op) && op >= 0) updated = updated.map((s) => ({ ...s, originalPrice: op }));
      const cp = parseFloat(bulkCostPrice); if (!isNaN(cp) && cp >= 0) updated = updated.map((s) => ({ ...s, costPrice: cp }));
      const wg = parseInt(bulkWeight); if (!isNaN(wg) && wg >= 0) updated = updated.map((s) => ({ ...s, weightGram: wg }));
      const l = bulkLength ? parseInt(bulkLength) : NaN; if (!isNaN(l)) updated = updated.map((s) => ({ ...s, lengthCm: l }));
      const w = bulkWidth ? parseInt(bulkWidth) : NaN; if (!isNaN(w)) updated = updated.map((s) => ({ ...s, widthCm: w }));
      const h = bulkHeight ? parseInt(bulkHeight) : NaN; if (!isNaN(h)) updated = updated.map((s) => ({ ...s, heightCm: h }));
      return updated;
    });
  };

  // ── Helper: extract error message ──
  const extractError = (err: unknown): string =>
    (err as { response?: { data?: { message?: string } } })?.response?.data?.message
    || "Thao tác thất bại. Vui lòng thử lại.";

  // ── Save Basic Info ──
  const handleSaveBasic = async () => {
    if (!productId || !product) return;
    if (!name.trim() || name.trim().length < 10) { toast.error("Tên sản phẩm phải có ít nhất 10 ký tự"); return; }
    if (!selectedCategory) { toast.error("Vui lòng chọn danh mục sản phẩm"); return; }
    setSavingBasic(true);
    try {
      const basicPayload: UpdateProductBasicInfoRequest = {
        name: name.trim(),
        categoryId: selectedCategory.id,
        description: description.trim() || undefined,
      };
      await updateProductBasicInfo(productId, basicPayload);
      toast.success("Đã lưu thông tin cơ bản!");
      const freshResp = await getProductById(productId);
      setProduct(freshResp.data.result);
    } catch (err) { toast.error(extractError(err)); }
    finally { setSavingBasic(false); }
  };

  // ── Save Images ──
  const handleSaveImages = async () => {
    if (!productId || !product) return;
    if (!primaryImage) { toast.error("Vui lòng tải lên ảnh chính"); return; }
    setSavingImages(true);
    try {
      const images = [
        { url: primaryImage, isPrimary: true, sortOrder: 0 },
        ...galleryImages.map((url, i) => ({ url, isPrimary: false, sortOrder: i + 1 })),
      ];
      await updateProductImages(productId, images);
      toast.success("Đã lưu hình ảnh!");
      const freshResp = await getProductById(productId);
      setProduct(freshResp.data.result);
    } catch (err) { toast.error(extractError(err)); }
    finally { setSavingImages(false); }
  };

  // ── Save Specifications ──
  const handleSaveSpecs = async () => {
    if (!productId || !product) return;
    setSpecErrors({});
    const newSpecErrors: Record<string, string> = {};
    for (const attr of categoryAttrs) {
      if (attr.required && !specs[attr.value]?.trim()) {
        newSpecErrors[attr.value] = `${attr.value} là bắt buộc`;
      }
    }
    if (Object.keys(newSpecErrors).length > 0) {
      setSpecErrors(newSpecErrors);
      toast.error("Vui lòng điền đầy đủ các thông số bắt buộc");
      return;
    }
    setSavingSpecs(true);
    try {
      const cleanSpecs: { name: string; value: string }[] = [];
      for (const [k, v] of Object.entries(specs)) {
        if (v.trim()) cleanSpecs.push({ name: k, value: v.trim() });
      }
      await updateProductSpecifications(productId, cleanSpecs);
      toast.success("Đã lưu thông số kỹ thuật!");
    } catch (err) { toast.error(extractError(err)); }
    finally { setSavingSpecs(false); }
  };

  // ── Save All SKUs (batch) ──
  const handleSaveAllSkus = async () => {
    if (!productId || !product) return;
    if (skus.length === 0) { toast.error("Vui lòng có ít nhất 1 biến thể SKU"); return; }
    for (const sku of skus) {
      if (!sku.skuCode.trim()) { toast.error("Mã SKU không được để trống"); return; }
      if (sku.price <= 0) { toast.error(`Giá bán của "${Object.values(sku.selectionAttributes).join(", ")}" phải lớn hơn 0`); return; }
    }
    setSavingSkus(true);
    try {
      await updateProductSkus(
        productId,
        skus.map((sku) => ({
          skuCode: sku.skuCode,
          price: sku.price,
          originalPrice: sku.originalPrice || undefined,
          costPrice: sku.costPrice || undefined,
          weightGram: sku.weightGram || undefined,
          dimensions: (sku.lengthCm || sku.widthCm || sku.heightCm)
            ? { lengthCm: sku.lengthCm || undefined, widthCm: sku.widthCm || undefined, heightCm: sku.heightCm || undefined }
            : undefined,
          selectionAttributes: sku.selectionAttributes,
        }))
      );
      toast.success("Đã lưu tất cả biến thể SKU!");
      const freshResp = await getProductById(productId);
      setProduct(freshResp.data.result);
      // Update SKU IDs from fresh data
      const freshSkus = freshResp.data.result.skus;
      setSkus((prev) => {
        const updated = prev.map((s) => {
          const matched = freshSkus.find((fs) => fs.skuCode === s.skuCode);
          return matched ? { ...s, id: matched.id } : s;
        });
        // Reset snapshots after save-all
        const snaps = new Map<string, string>();
        updated.forEach((r) => snaps.set(r._key, skuSnapshot(r)));
        setSkuSnapshots(snaps);
        return updated;
      });
    } catch (err) { toast.error(extractError(err)); }
    finally { setSavingSkus(false); }
  };

  // ── Save Single SKU ──
  const handleSaveSingleSku = async (sku: SkuRow) => {
    if (!sku.id) {
      toast.error("SKU chưa có ID. Hãy lưu tất cả SKU trước.");
      return;
    }
    if (sku.price <= 0) { toast.error(`Giá bán của "${Object.values(sku.selectionAttributes).join(", ")}" phải lớn hơn 0`); return; }
    setSavingSkuId(sku.id);
    try {
      const payload: UpdateSingleSkuRequest = {
        price: sku.price,
        originalPrice: sku.originalPrice || undefined,
        costPrice: sku.costPrice || undefined,
        weightGram: sku.weightGram || undefined,
        dimensions: (sku.lengthCm || sku.widthCm || sku.heightCm)
          ? { lengthCm: sku.lengthCm || undefined, widthCm: sku.widthCm || undefined, heightCm: sku.heightCm || undefined }
          : undefined,
      };
      await updateSingleSku(sku.id, payload);

      // Update snapshot so the button becomes disabled again
      setSkuSnapshots((prev) => {
        const next = new Map(prev);
        next.set(sku._key, skuSnapshot(sku));
        return next;
      });
      toast.success(`Đã lưu SKU "${sku.skuCode}"!`);
    } catch (err) { toast.error(extractError(err)); }
    finally { setSavingSkuId(null); }
  };

  // ── Loading screen ──
  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <Loader2 className="w-8 h-8 animate-spin text-red-500" />
        <span className="ml-3 text-slate-500 font-bold">Đang tải sản phẩm...</span>
      </div>
    );
  }

  if (!product) {
    return (
      <div className="flex flex-col items-center justify-center h-96 gap-4">
        <AlertCircle className="w-12 h-12 text-red-400" />
        <p className="text-slate-600 font-bold">Không tìm thấy sản phẩm</p>
        <button onClick={() => navigate("/seller/products")}
          className="px-4 py-2 rounded-xl bg-red-600 text-white text-sm font-bold hover:bg-red-700">
          Quay lại danh sách
        </button>
      </div>
    );
  }

  return (
    <div className="w-full space-y-5 pb-10">
      {/* Header */}
      <div className="flex items-center gap-3">
        <button type="button" onClick={() => navigate("/seller/products")}
          className="p-2 rounded-xl border border-slate-200 hover:bg-slate-50">
          <ArrowLeft className="w-4 h-4 text-slate-600" />
        </button>
        <div className="flex-1">
          <h1 className="text-xl font-black text-slate-900">Chỉnh sửa sản phẩm</h1>
          <p className="text-xs text-slate-400 mt-0.5">
            {product.name}
            <span className={`ml-2 px-1.5 py-0.5 rounded text-[10px] font-bold ${
              product.status === "ACTIVE" ? "bg-green-100 text-green-700" :
              product.status === "DRAFT" ? "bg-slate-100 text-slate-600" :
              product.status === "PENDING" ? "bg-yellow-100 text-yellow-700" :
              "bg-red-100 text-red-600"
            }`}>
              {product.status}
            </span>
          </p>
        </div>
        <button type="button" onClick={() => navigate(`/product/${productId}`)}
          className="flex items-center gap-1.5 px-4 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50">
          <Eye className="w-4 h-4" /> Xem trước
        </button>
      </div>

      {/* ─── 1. Basic info ─── */}
      <SectionCard title="Thông tin cơ bản" subtitle="Tên và mô tả sản phẩm">
        <div className="space-y-3">
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Tên sản phẩm *</label>
            <input className={inputCls} placeholder="Nhập tên sản phẩm (ít nhất 10 ký tự)..."
              value={name} onChange={(e) => setName(e.target.value)} />
            <p className="text-[11px] text-slate-400 mt-1">{name.length} ký tự</p>
          </div>
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Mô tả</label>
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

        {/* ── Category (inside basic info section) ── */}
        <div className="mt-5 pt-5 border-t border-slate-100">
          <h3 className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-2">Danh mục *</h3>
          {categoryTree.length === 0 ? (
            <div className="flex items-center gap-2 text-slate-400 text-sm py-4"><Loader2 className="w-4 h-4 animate-spin" /> Đang tải danh mục...</div>
          ) : (
            <CategoryPicker tree={categoryTree} selected={selectedCategory}
              onSelect={(cat, namePath) => {
                if (cat?.id) {
                  (cat as CategoryTreeResponse & { _namePath?: string })._namePath = namePath;
                  setSelectedCategory({ ...cat });
                } else {
                  setSelectedCategory(null);
                }
                setSpecs({}); setSpecErrors({});
              }} />
          )}
        </div>

        <div className="flex justify-end mt-4 pt-4 border-t border-slate-100">
          <button type="button" onClick={handleSaveBasic} disabled={savingBasic}
            className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white text-xs font-bold disabled:opacity-60 shadow-sm">
            {savingBasic ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Save className="w-3.5 h-3.5" />}
            Lưu thông tin cơ bản
          </button>
        </div>
      </SectionCard>

      {/* ─── 2. Images ─── */}
      <SectionCard title="Hình ảnh" subtitle="Ảnh chính và bộ sưu tập">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          <div>
            <h3 className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-2">Ảnh chính *</h3>
            <PrimaryImageUpload value={primaryImage} onChange={setPrimaryImage} folder="products" />
          </div>
          <div>
            <h3 className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-2">Bộ sưu tập (tối đa 8)</h3>
            <GalleryUpload images={galleryImages} onChange={setGalleryImages} folder="products" maxImages={8} />
          </div>
        </div>
        <div className="flex justify-end mt-4 pt-4 border-t border-slate-100">
          <button type="button" onClick={handleSaveImages} disabled={savingImages}
            className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white text-xs font-bold disabled:opacity-60 shadow-sm">
            {savingImages ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Save className="w-3.5 h-3.5" />}
            Lưu hình ảnh
          </button>
        </div>
      </SectionCard>

      {/* ─── 4. Options & Variants ─── */}
      <SectionCard title="Phân loại hàng" subtitle="Thêm các phân loại (VD: Màu sắc, Kích thước) để tự động tạo biến thể SKU">
        <OptionsEditor options={options} onChange={(newOpts) => { optionsInitialized.current = true; setOptions(newOpts); }} folder="products/options" />
      </SectionCard>

      {/* ─── 5. SKU Table ─── */}
      {skus.length > 0 && (
        <SectionCard title={`Danh sách SKU (${skus.length} biến thể)`}
          subtitle="Chỉnh sửa giá, kích thước, cân nặng, ảnh cho từng biến thể">
          {/* Bulk actions */}
          <div className="mb-4 p-4 rounded-xl bg-slate-50 border border-slate-100 space-y-3">
            <div className="flex items-center gap-2">
              <Info className="w-4 h-4 text-slate-400 shrink-0" />
              <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Áp dụng cho tất cả biến thể</span>
            </div>
            {/* Row 1 */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Giá bán</label>
                <input type="number" min={0} placeholder="VD: 100000"
                  className="w-full px-2.5 py-2 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                  value={bulkPrice} onChange={(e) => setBulkPrice(e.target.value)} />
              </div>
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Giá gốc</label>
                <input type="number" min={0} placeholder="VD: 150000"
                  className="w-full px-2.5 py-2 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                  value={bulkOriginalPrice} onChange={(e) => setBulkOriginalPrice(e.target.value)} />
              </div>
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Giá nhập</label>
                <input type="number" min={0} placeholder="VD: 80000"
                  className="w-full px-2.5 py-2 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                  value={bulkCostPrice} onChange={(e) => setBulkCostPrice(e.target.value)} />
                {bulkCostPrice && bulkPrice && parseFloat(bulkPrice) > 0 && (
                  <p className={`text-[10px] mt-0.5 font-bold ${parseFloat(bulkPrice) - parseFloat(bulkCostPrice) >= 0 ? "text-green-600" : "text-red-500"}`}>
                    Lợi nhuận: {((parseFloat(bulkPrice) - parseFloat(bulkCostPrice)) / parseFloat(bulkPrice) * 100).toFixed(1)}%
                    ({(parseFloat(bulkPrice) - parseFloat(bulkCostPrice)).toLocaleString()}đ)
                  </p>
                )}
              </div>
            </div>
            {/* Row 2 */}
            <div className="grid grid-cols-1 sm:grid-cols-4 gap-3">
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Cân nặng (g)</label>
                <input type="number" min={0} placeholder="VD: 500"
                  className="w-full px-2.5 py-2 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                  value={bulkWeight} onChange={(e) => setBulkWeight(e.target.value)} />
              </div>
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Dài (cm)</label>
                <input type="number" min={0} placeholder="VD: 30"
                  className="w-full px-2.5 py-2 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                  value={bulkLength} onChange={(e) => setBulkLength(e.target.value)} />
              </div>
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Rộng (cm)</label>
                <input type="number" min={0} placeholder="VD: 20"
                  className="w-full px-2.5 py-2 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                  value={bulkWidth} onChange={(e) => setBulkWidth(e.target.value)} />
              </div>
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase mb-0.5">Cao (cm)</label>
                <input type="number" min={0} placeholder="VD: 10"
                  className="w-full px-2.5 py-2 rounded-lg border border-slate-200 bg-white text-xs outline-none focus:border-red-400"
                  value={bulkHeight} onChange={(e) => setBulkHeight(e.target.value)} />
              </div>
            </div>
            <div className="flex justify-end pt-1">
              <button type="button" onClick={applyBulkAll}
                className="flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white text-xs font-bold shadow-sm transition-colors">
                Áp dụng tất cả
              </button>
            </div>
          </div>
          <SkuTable skus={skus} onChange={setSkus}
            onSaveSingle={handleSaveSingleSku} savingSkuId={savingSkuId} snapshots={skuSnapshots} />

          <div className="flex justify-end mt-4 pt-4 border-t border-slate-100">
            <button type="button" onClick={handleSaveAllSkus} disabled={savingSkus}
              className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white text-xs font-bold disabled:opacity-60 shadow-sm">
              {savingSkus ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Save className="w-3.5 h-3.5" />}
              Lưu tất cả SKU
            </button>
          </div>
        </SectionCard>
      )}

      {/* ─── 6. Specifications ─── */}
      <SectionCard title="Thông số kỹ thuật"
        subtitle={
          selectedCategory
            ? loadingAttrs ? "Đang tải thông số từ danh mục..."
              : categoryAttrs.length > 0
                ? `${categoryAttrs.length} thông số từ danh mục "${selectedCategory.name}" — trường có dấu * là bắt buộc`
                : "Danh mục này không có thông số. Bạn có thể thêm thông số tùy chỉnh."
            : "Chọn danh mục trước để hiển thị các thông số bắt buộc"
        }>
        {loadingAttrs ? (
          <div className="flex items-center gap-2 text-slate-400 text-sm py-4"><Loader2 className="w-4 h-4 animate-spin" /> Đang tải...</div>
        ) : (
          <SpecificationsEditor attributes={categoryAttrs} specs={specs} onChange={setSpecs} errors={specErrors} />
        )}
        <div className="flex justify-end mt-4 pt-4 border-t border-slate-100">
          <button type="button" onClick={handleSaveSpecs} disabled={savingSpecs}
            className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white text-xs font-bold disabled:opacity-60 shadow-sm">
            {savingSpecs ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Save className="w-3.5 h-3.5" />}
            Lưu thông số
          </button>
        </div>
      </SectionCard>

      <SectionCard title="Tồn kho" subtitle="Theo dõi và điều chỉnh số lượng từng biến thể SKU">
        {inventoryLoading ? (
          <div className="flex items-center justify-center py-10 gap-2 text-slate-400">
            <Loader2 className="w-5 h-5 animate-spin" />
            <span className="text-sm font-semibold">Đang tải dữ liệu tồn kho...</span>
          </div>
        ) : inventoryError ? (
          <div className="text-sm text-red-500 font-semibold py-10 text-center">{inventoryError}</div>
        ) : skus.filter((sku) => sku.id).length === 0 ? (
          <div className="text-sm text-slate-500 py-10 text-center">Chưa có SKU lưu hoặc không có dữ liệu tồn kho.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-xs text-slate-600 border-collapse">
              <thead>
                <tr className="bg-slate-50 text-slate-500 uppercase tracking-widest text-[10px]">
                  <th className="px-3 py-3">Biến thể</th>
                  <th className="px-3 py-3 text-right">Tổng kho</th>
                  <th className="px-3 py-3 text-right">Đã đặt</th>
                  <th className="px-3 py-3 text-right">Có sẵn</th>
                  <th className="px-3 py-3 text-right">Ngưỡng thấp</th>
                  <th className="px-3 py-3 text-right">Hành động</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {skus.map((sku) => {
                  const state = sku.id ? inventoryStates[sku.id] : undefined;
                  const inv = state?.inventory;
                  return (
                    <tr key={sku._key} className="bg-white">
                      <td className="px-3 py-3 align-top">
                        <div className="flex items-center gap-3 min-w-[220px]">
                          <div className="w-11 h-11 rounded-xl overflow-hidden border border-slate-200 bg-slate-100 flex items-center justify-center">
                            {getSkuVariantImage(sku) ? (
                              <img src={getSkuVariantImage(sku) || undefined} alt={sku.skuCode} className="w-full h-full object-cover" />
                            ) : (
                              <Package className="w-5 h-5 text-slate-400" />
                            )}
                          </div>
                          <div className="min-w-0">
                            <p className="font-bold text-slate-800 truncate">{sku.skuCode}</p>
                            <p className="text-[11px] text-slate-400 truncate">{getSkuVariantLabel(sku)}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-3 py-3 text-right">
                        {state?.isEditing ? (
                          <input
                            type="number"
                            min={0}
                            className="w-20 px-2 py-1 rounded-lg border border-slate-200 text-right text-sm"
                            value={state.editingTotalStock ?? 0}
                            onChange={(e) => setInventoryStates((prev) => ({
                              ...prev,
                              [sku.id!]: { ...prev[sku.id!], editingTotalStock: Number(e.target.value) || 0 },
                            }))}
                          />
                        ) : (
                          inv?.totalStock.toLocaleString("vi-VN") || "—"
                        )}
                      </td>
                      <td className="px-3 py-3 text-right">{inv ? inv.reservedStock.toLocaleString("vi-VN") : "—"}</td>
                      <td className="px-3 py-3 text-right">{inv ? inv.availableStock.toLocaleString("vi-VN") : "—"}</td>
                      <td className="px-3 py-3 text-right">
                        {state?.isEditing ? (
                          <input
                            type="number"
                            min={0}
                            className="w-20 px-2 py-1 rounded-lg border border-slate-200 text-right text-sm"
                            value={state.editingThreshold ?? 0}
                            onChange={(e) => setInventoryStates((prev) => ({
                              ...prev,
                              [sku.id!]: { ...prev[sku.id!], editingThreshold: Number(e.target.value) || 0 },
                            }))}
                          />
                        ) : (
                          inv?.lowStockThreshold || "—"
                        )}
                      </td>
                      <td className="px-3 py-3 align-top">
                        {sku.id ? (
                          <div className="space-y-2">
                            {state?.isEditing ? (
                              <>
                                <div className="flex items-center gap-1">
                                  <button type="button" onClick={() => handleSaveInventory(sku)} disabled={!state || state.saving}
                                    className="flex-1 px-2 py-2 rounded-lg bg-green-600 hover:bg-green-700 text-white text-[10px] font-bold disabled:opacity-50">
                                    {state?.saving ? "Đang lưu..." : "Lưu"}
                                  </button>
                                  <button type="button" onClick={() => cancelEditMode(sku.id!)} disabled={state?.saving}
                                    className="px-2 py-2 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-50 text-[10px] font-bold disabled:opacity-50">
                                    Hủy
                                  </button>
                                </div>
                              </>
                            ) : (
                              <button type="button" onClick={() => enterEditMode(sku.id!)} disabled={state?.saving}
                                className="w-full px-2 py-1.5 rounded-lg bg-red-600 hover:bg-red-700 text-white text-[9px] font-bold disabled:opacity-50">
                                Sửa
                              </button>
                            )}
                            {state?.error && <p className="text-[10px] text-red-500">{state.error}</p>}
                            {state?.success && <p className="text-[10px] text-green-600">{state.success}</p>}
                          </div>
                        ) : (
                          <p className="text-[11px] text-slate-400">SKU chưa lưu</p>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </SectionCard>

      {/* ─── Back to list ─── */}
      <div className="flex items-center justify-end gap-3 pt-2">
        <button type="button" onClick={() => navigate("/seller/products")}
          className="px-5 py-2.5 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50">
          Quay lại danh sách
        </button>
      </div>
    </div>
  );
}
