import { useEffect, useState } from "react";
import {
  Plus, Pencil, Trash2, X, Loader2, SlidersHorizontal, Search,
} from "lucide-react";
import {
  sellerListOptionTemplates,
  sellerCreateOptionTemplate,
  sellerUpdateOptionTemplate,
  sellerDeleteOptionTemplate,
} from "../../api/sellerDashboardApi";
import type {
  OptionTemplateResponse,
  CreateOptionTemplateRequest,
} from "../../types/admin";

/* ────────────────────── Modal ────────────────────── */

function OptionTemplateModal({
  open,
  initial,
  onClose,
  onSave,
}: {
  open: boolean;
  initial: OptionTemplateResponse | null;
  onClose: () => void;
  onSave: (req: CreateOptionTemplateRequest) => Promise<void>;
}) {
  const [name, setName] = useState("");
  const [values, setValues] = useState<string[]>([]);
  const [input, setInput] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) {
      setName(initial?.name ?? "");
      setValues(initial?.values.map((v) => v.value) ?? []);
      setInput("");
    }
  }, [open, initial]);

  const addValue = () => {
    const v = input.trim();
    if (v && !values.includes(v)) setValues([...values, v]);
    setInput("");
  };

  const handleSubmit = async () => {
    if (!name.trim() || values.length === 0) return;
    setSaving(true);
    try {
      await onSave({ name: name.trim(), values });
      onClose();
    } finally {
      setSaving(false);
    }
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md mx-4">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
          <h3 className="font-black text-slate-800">
            {initial ? "Sửa mẫu phân loại" : "Tạo mẫu phân loại"}
          </h3>
          <button onClick={onClose} className="p-1 rounded-lg hover:bg-slate-100">
            <X className="w-5 h-5 text-slate-400" />
          </button>
        </div>

        <div className="p-5 space-y-4">
          <div>
            <label className="text-xs font-bold text-slate-500 mb-1 block">Tên phân loại *</label>
            <input
              className="w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400"
              placeholder="VD: Màu sắc, Kích thước..."
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </div>

          <div>
            <label className="text-xs font-bold text-slate-500 mb-1 block">Các giá trị *</label>
            <div className="flex flex-wrap gap-1.5 mb-2 min-h-[32px]">
              {values.map((v, i) => (
                <span
                  key={i}
                  className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-red-50 border border-red-100 text-xs font-semibold text-red-600"
                >
                  {v}
                  <button onClick={() => setValues(values.filter((_, j) => j !== i))}>
                    <X className="w-3 h-3" />
                  </button>
                </span>
              ))}
            </div>
            <div className="flex gap-2">
              <input
                className="flex-1 px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400"
                placeholder="Nhập giá trị rồi Enter..."
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") { e.preventDefault(); addValue(); }
                }}
              />
              <button
                type="button"
                onClick={addValue}
                className="px-3 py-2 rounded-xl bg-slate-200 hover:bg-slate-300 text-xs font-bold text-slate-600"
              >
                Thêm
              </button>
            </div>
          </div>
        </div>

        <div className="flex justify-end gap-2 px-5 py-4 border-t border-slate-100">
          <button onClick={onClose} className="px-4 py-2 rounded-xl text-sm font-bold text-slate-500 hover:bg-slate-100">
            Hủy
          </button>
          <button
            onClick={handleSubmit}
            disabled={saving || !name.trim() || values.length === 0}
            className="px-4 py-2 rounded-xl bg-red-600 text-white text-sm font-bold hover:bg-red-700 disabled:opacity-50"
          >
            {saving ? <Loader2 className="w-4 h-4 animate-spin" /> : initial ? "Cập nhật" : "Tạo mới"}
          </button>
        </div>
      </div>
    </div>
  );
}

/* ────────────────────── Delete confirm ────────────────────── */

function DeleteModal({
  open,
  name,
  onClose,
  onConfirm,
}: {
  open: boolean;
  name: string;
  onClose: () => void;
  onConfirm: () => void;
}) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-sm mx-4 p-6 text-center space-y-4">
        <div className="w-12 h-12 rounded-full bg-red-50 flex items-center justify-center mx-auto">
          <Trash2 className="w-6 h-6 text-red-500" />
        </div>
        <h3 className="font-black text-slate-800">Xóa mẫu "{name}"?</h3>
        <p className="text-sm text-slate-500">Hành động này không thể hoàn tác.</p>
        <div className="flex justify-center gap-3 pt-2">
          <button onClick={onClose} className="px-4 py-2 rounded-xl text-sm font-bold text-slate-500 hover:bg-slate-100">
            Hủy
          </button>
          <button onClick={onConfirm} className="px-4 py-2 rounded-xl bg-red-600 text-white text-sm font-bold hover:bg-red-700">
            Xóa
          </button>
        </div>
      </div>
    </div>
  );
}

/* ────────────────────── Main page ────────────────────── */

export default function SellerOptionsPage() {
  const [templates, setTemplates] = useState<OptionTemplateResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");

  // modal state
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<OptionTemplateResponse | null>(null);
  const [deleting, setDeleting] = useState<OptionTemplateResponse | null>(null);

  const load = async () => {
    setLoading(true);
    try {
      const data = await sellerListOptionTemplates();
      setTemplates(data);
    } catch {
      /* ignore */
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleSave = async (req: CreateOptionTemplateRequest) => {
    if (editing) {
      await sellerUpdateOptionTemplate(editing.id, req);
    } else {
      await sellerCreateOptionTemplate(req);
    }
    await load();
  };

  const handleDelete = async () => {
    if (!deleting) return;
    await sellerDeleteOptionTemplate(deleting.id);
    setDeleting(null);
    await load();
  };

  const filtered = templates.filter(
    (t) => t.name.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-black text-slate-800 flex items-center gap-2">
            <SlidersHorizontal className="w-7 h-7 text-red-500" />
            Mẫu phân loại hàng
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Tạo sẵn các phân loại (Màu sắc, Kích thước, ...) để dùng nhanh khi thêm sản phẩm.
          </p>
        </div>
        <button
          onClick={() => { setEditing(null); setModalOpen(true); }}
          className="flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-red-600 text-white text-sm font-bold hover:bg-red-700 shadow-sm"
        >
          <Plus className="w-4 h-4" />
          Tạo mẫu mới
        </button>
      </div>

      {/* Search */}
      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
        <input
          className="w-full pl-9 pr-3 py-2.5 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400"
          placeholder="Tìm kiếm mẫu..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {/* Content */}
      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="w-8 h-8 animate-spin text-red-400" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="text-center py-20 text-slate-400">
          <SlidersHorizontal className="w-12 h-12 mx-auto mb-3 opacity-30" />
          <p className="font-bold">Chưa có mẫu phân loại nào</p>
          <p className="text-sm mt-1">Bấm "Tạo mẫu mới" để bắt đầu.</p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((tpl) => (
            <div
              key={tpl.id}
              className="bg-white rounded-2xl border border-slate-200 shadow-sm p-4 space-y-3 hover:shadow-md transition-shadow"
            >
              <div className="flex items-start justify-between">
                <h3 className="font-black text-slate-800 text-base">{tpl.name}</h3>
                <div className="flex gap-1">
                  <button
                    onClick={() => { setEditing(tpl); setModalOpen(true); }}
                    className="p-1.5 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-blue-600"
                  >
                    <Pencil className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => setDeleting(tpl)}
                    className="p-1.5 rounded-lg hover:bg-red-50 text-slate-400 hover:text-red-500"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
              <div className="flex flex-wrap gap-1.5">
                {tpl.values.map((v) => (
                  <span
                    key={v.id}
                    className="px-2 py-0.5 rounded-lg bg-slate-100 text-xs font-semibold text-slate-600"
                  >
                    {v.value}
                  </span>
                ))}
              </div>
              <p className="text-[10px] text-slate-400">
                {new Date(tpl.createdAt).toLocaleDateString("vi-VN")}
              </p>
            </div>
          ))}
        </div>
      )}

      {/* Modals */}
      <OptionTemplateModal
        open={modalOpen}
        initial={editing}
        onClose={() => setModalOpen(false)}
        onSave={handleSave}
      />
      <DeleteModal
        open={!!deleting}
        name={deleting?.name ?? ""}
        onClose={() => setDeleting(null)}
        onConfirm={handleDelete}
      />
    </div>
  );
}
