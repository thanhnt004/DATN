import { useEffect, useState } from "react";
import {
  Loader2, Plus, Pencil, Trash2, X, RefreshCw, Tag, AlertCircle,
} from "lucide-react";
import {
  adminListOptionTemplates,
  adminCreateOptionTemplate,
  adminUpdateOptionTemplate,
  adminDeleteOptionTemplate,
} from "../../api/adminApi";
import type {
  OptionTemplateResponse,
  CreateOptionTemplateRequest,
  UpdateOptionTemplateRequest,
} from "../../types/admin";

// ─── Modal form ────────────────────────────────────────────────────────────────

function OptionTemplateModal({
  template,
  onClose,
  onSaved,
}: {
  template: OptionTemplateResponse | null; // null = create
  onClose: () => void;
  onSaved: () => void;
}) {
  const isEdit = !!template;
  const [name, setName] = useState(template?.name || "");
  const [values, setValues] = useState<string[]>(template?.values.map((v) => v.value) || []);
  const [newValue, setNewValue] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const addValue = () => {
    const v = newValue.trim();
    if (!v || values.includes(v)) return;
    setValues((prev) => [...prev, v]);
    setNewValue("");
  };

  const removeValue = (index: number) => {
    setValues((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSave = async () => {
    if (!name.trim()) {
      setError("Tên option không được để trống");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      if (isEdit) {
        const payload: UpdateOptionTemplateRequest = { name: name.trim(), values };
        await adminUpdateOptionTemplate(template.id, payload);
      } else {
        const payload: CreateOptionTemplateRequest = { name: name.trim(), values };
        await adminCreateOptionTemplate(payload);
      }
      onSaved();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        "Thao tác thất bại";
      setError(msg);
    } finally {
      setSaving(false);
    }
  };

  const cls =
    "w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400";

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-full max-w-md shadow-2xl">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
          <h3 className="font-black text-slate-800">
            {isEdit ? "Chỉnh sửa Option" : "Tạo Option mới"}
          </h3>
          <button onClick={onClose} className="p-1 rounded-lg hover:bg-slate-100">
            <X className="w-5 h-5 text-slate-400" />
          </button>
        </div>

        <div className="p-5 space-y-4">
          {/* Name */}
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">
              Tên phân loại *
            </label>
            <input
              autoFocus
              className={cls}
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="VD: Màu sắc, Kích thước, Chất liệu..."
            />
          </div>

          {/* Values */}
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">
              Giá trị
            </label>
            <div className="flex flex-wrap gap-1.5 mb-2 min-h-[28px]">
              {values.map((val, i) => (
                <span
                  key={i}
                  className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-red-50 border border-red-200 text-xs font-semibold text-red-700"
                >
                  {val}
                  <button
                    type="button"
                    onClick={() => removeValue(i)}
                    className="text-red-300 hover:text-red-600"
                  >
                    <X className="w-3 h-3" />
                  </button>
                </span>
              ))}
              {values.length === 0 && (
                <span className="text-xs text-slate-400">Chưa có giá trị nào</span>
              )}
            </div>
            <div className="flex items-center gap-2">
              <input
                className={`${cls} flex-1`}
                placeholder="Thêm giá trị..."
                value={newValue}
                onChange={(e) => setNewValue(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    addValue();
                  }
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

          {error && (
            <p className="text-xs text-red-500 font-semibold flex items-center gap-1">
              <AlertCircle className="w-3 h-3" />
              {error}
            </p>
          )}
        </div>

        <div className="px-5 pb-5 flex justify-end gap-2">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50"
          >
            Hủy
          </button>
          <button
            onClick={handleSave}
            disabled={saving}
            className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-60 flex items-center gap-2"
          >
            {saving && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
            {isEdit ? "Lưu" : "Tạo mới"}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Delete confirm ────────────────────────────────────────────────────────────

function DeleteModal({
  template,
  onClose,
  onDone,
}: {
  template: OptionTemplateResponse;
  onClose: () => void;
  onDone: () => void;
}) {
  const [loading, setLoading] = useState(false);
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-full max-w-sm shadow-2xl p-6">
        <h3 className="font-black text-slate-800 mb-2">Xóa option template</h3>
        <p className="text-sm text-slate-500 mb-4">
          Bạn có chắc muốn xóa <strong>"{template.name}"</strong>? Thao tác này không thể hoàn tác.
        </p>
        <div className="flex gap-2 justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50"
          >
            Hủy
          </button>
          <button
            disabled={loading}
            onClick={async () => {
              setLoading(true);
              try {
                await adminDeleteOptionTemplate(template.id);
                onDone();
              } finally {
                setLoading(false);
              }
            }}
            className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-60 flex items-center gap-2"
          >
            {loading && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
            Xóa
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Main page ─────────────────────────────────────────────────────────────────

type ModalState =
  | { type: "create" }
  | { type: "edit"; template: OptionTemplateResponse }
  | { type: "delete"; template: OptionTemplateResponse }
  | null;

export default function AdminOptionsPage() {
  const [templates, setTemplates] = useState<OptionTemplateResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState<ModalState>(null);

  const load = async () => {
    setLoading(true);
    try {
      const list = await adminListOptionTemplates();
      setTemplates(list);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  return (
    <div className="space-y-5 max-w-4xl">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-xl font-black text-slate-900">Quản lý phân loại hàng</h1>
          <p className="text-sm text-slate-400 mt-0.5">
            Tạo các mẫu option (Màu sắc, Kích thước, ...) để seller dùng khi tạo sản phẩm
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={load}
            className="p-2 rounded-xl border border-slate-200 hover:bg-slate-50"
          >
            <RefreshCw className="w-4 h-4 text-slate-500" />
          </button>
          <button
            onClick={() => setModal({ type: "create" })}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold"
          >
            <Plus className="w-3.5 h-3.5" />
            Thêm mẫu option
          </button>
        </div>
      </div>

      {/* Content */}
      {loading ? (
        <div className="flex items-center justify-center py-16 gap-2 text-slate-400">
          <Loader2 className="w-5 h-5 animate-spin" />
          <span className="text-sm font-semibold">Đang tải...</span>
        </div>
      ) : templates.length === 0 ? (
        <div className="text-center py-16">
          <Tag className="w-12 h-12 mx-auto text-slate-200 mb-3" />
          <p className="text-sm text-slate-400 font-semibold">Chưa có mẫu option nào</p>
          <p className="text-xs text-slate-300 mt-1">
            Tạo mẫu option để seller có thể dùng khi đăng sản phẩm
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {templates.map((tpl) => (
            <div
              key={tpl.id}
              className="bg-white rounded-2xl border border-slate-200 shadow-sm p-4 flex flex-col gap-3"
            >
              {/* Header */}
              <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-2 min-w-0">
                  <div className="w-8 h-8 rounded-lg bg-red-50 flex items-center justify-center shrink-0">
                    <Tag className="w-4 h-4 text-red-500" />
                  </div>
                  <div className="min-w-0">
                    <p className="font-bold text-sm text-slate-800 truncate">{tpl.name}</p>
                    <p className="text-[10px] text-slate-400">
                      {tpl.values.length} giá trị
                    </p>
                  </div>
                </div>
                <span className="px-1.5 py-0.5 rounded-full text-[10px] font-bold bg-blue-100 text-blue-700 shrink-0">
                  {tpl.source}
                </span>
              </div>

              {/* Values */}
              <div className="flex flex-wrap gap-1.5">
                {tpl.values.slice(0, 8).map((v) => (
                  <span
                    key={v.id}
                    className="px-2 py-0.5 rounded-md bg-slate-50 border border-slate-100 text-[11px] font-semibold text-slate-600"
                  >
                    {v.value}
                  </span>
                ))}
                {tpl.values.length > 8 && (
                  <span className="px-2 py-0.5 rounded-md bg-slate-100 text-[11px] font-bold text-slate-400">
                    +{tpl.values.length - 8}
                  </span>
                )}
                {tpl.values.length === 0 && (
                  <span className="text-[11px] text-slate-400 italic">Chưa có giá trị</span>
                )}
              </div>

              {/* Actions */}
              <div className="flex items-center gap-1 pt-1 border-t border-slate-50">
                <button
                  onClick={() => setModal({ type: "edit", template: tpl })}
                  className="flex-1 flex items-center justify-center gap-1 py-1.5 rounded-lg text-xs font-bold border border-slate-200 hover:bg-slate-50 text-slate-600"
                >
                  <Pencil className="w-3 h-3" />
                  Sửa
                </button>
                <button
                  onClick={() => setModal({ type: "delete", template: tpl })}
                  className="p-1.5 rounded-lg border border-slate-200 hover:bg-red-50 text-slate-400 hover:text-red-500"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modals */}
      {modal?.type === "create" && (
        <OptionTemplateModal
          template={null}
          onClose={() => setModal(null)}
          onSaved={() => {
            setModal(null);
            load();
          }}
        />
      )}
      {modal?.type === "edit" && (
        <OptionTemplateModal
          template={modal.template}
          onClose={() => setModal(null)}
          onSaved={() => {
            setModal(null);
            load();
          }}
        />
      )}
      {modal?.type === "delete" && (
        <DeleteModal
          template={modal.template}
          onClose={() => setModal(null)}
          onDone={() => {
            setModal(null);
            load();
          }}
        />
      )}
    </div>
  );
}
