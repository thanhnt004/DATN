import { useEffect, useState, useCallback } from "react";
import {
  Megaphone, Plus, Loader2, ChevronLeft, ChevronRight, X, Pencil,
  Calendar, Trash2,
} from "lucide-react";
import {
  adminGetCampaigns,
  adminCreateCampaign,
  adminUpdateCampaign,
  adminDeleteCampaign,
} from "../../api/couponApi";
import type { CampaignResponse, CreateCampaignRequest, UpdateCampaignRequest } from "../../types/coupon";

function fmtDate(s: string) {
  return new Date(s).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });
}

const STATUS_MAP: Record<string, { label: string; cls: string }> = {
  DRAFT:     { label: "Bản nháp",  cls: "bg-gray-100 text-gray-600" },
  ACTIVE:    { label: "Hoạt động", cls: "bg-green-100 text-green-700" },
  ENDED:     { label: "Đã kết thúc", cls: "bg-blue-100 text-blue-700" },
  CANCELLED: { label: "Đã hủy",    cls: "bg-red-100 text-red-700" },
};

// ─── Form modal ──────────────────────────────────────────────────────────────
function CampaignFormModal({ campaign, onClose, onSaved }: { campaign?: CampaignResponse | null; onClose: () => void; onSaved: () => void }) {
  const isEdit = !!campaign;
  const [name, setName] = useState(campaign?.name ?? "");
  const [description, setDescription] = useState(campaign?.description ?? "");
  const [campaignType, setCampaignType] = useState(campaign?.campaignType ?? "PLATFORM");
  const [status, setStatus] = useState(campaign?.status ?? "DRAFT");
  const [startDate, setStartDate] = useState(campaign ? campaign.startDate.slice(0, 16) : "");
  const [endDate, setEndDate] = useState(campaign ? campaign.endDate.slice(0, 16) : "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !startDate || !endDate) { setError("Vui lòng điền đầy đủ thông tin"); return; }
    setSaving(true); setError(null);
    try {
      if (isEdit) {
        const data: UpdateCampaignRequest = { name: name.trim(), description: description.trim() || undefined, status, startDate: new Date(startDate).toISOString(), endDate: new Date(endDate).toISOString() };
        await adminUpdateCampaign(campaign!.id, data);
      } else {
        const data: CreateCampaignRequest = { name: name.trim(), description: description.trim() || undefined, campaignType, startDate: new Date(startDate).toISOString(), endDate: new Date(endDate).toISOString() };
        await adminCreateCampaign(data);
      }
      onSaved();
    } catch { setError("Có lỗi xảy ra"); }
    finally { setSaving(false); }
  };

  const cls = "w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400";
  const lbl = "block text-xs font-bold text-slate-500 uppercase tracking-widest mb-1";

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm overflow-auto">
      <form onSubmit={handleSubmit} className="bg-white rounded-2xl w-full max-w-md shadow-2xl">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
          <h3 className="font-black text-slate-800">{isEdit ? "Sửa chiến dịch" : "Tạo chiến dịch"}</h3>
          <button type="button" onClick={onClose} className="p-1 rounded-lg hover:bg-slate-100"><X className="w-5 h-5 text-slate-400" /></button>
        </div>
        <div className="p-5 space-y-4">
          <div>
            <label className={lbl}>Tên chiến dịch *</label>
            <input className={cls} value={name} onChange={e => setName(e.target.value)} placeholder="VD: Siêu sale 12.12" />
          </div>
          <div>
            <label className={lbl}>Mô tả</label>
            <textarea rows={2} className={`${cls} resize-none`} value={description} onChange={e => setDescription(e.target.value)} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className={lbl}>Loại</label>
              <select className={cls} value={campaignType} onChange={e => setCampaignType(e.target.value)} disabled={isEdit}>
                <option value="PLATFORM">Sàn</option>
                <option value="SHOP">Shop</option>
              </select>
            </div>
            {isEdit && (
              <div>
                <label className={lbl}>Trạng thái</label>
                <select className={cls} value={status} onChange={e => setStatus(e.target.value)}>
                  <option value="DRAFT">Nháp</option>
                  <option value="ACTIVE">Hoạt động</option>
                  <option value="ENDED">Kết thúc</option>
                  <option value="CANCELLED">Hủy</option>
                </select>
              </div>
            )}
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div><label className={lbl}>Bắt đầu *</label><input type="datetime-local" className={cls} value={startDate} onChange={e => setStartDate(e.target.value)} /></div>
            <div><label className={lbl}>Kết thúc *</label><input type="datetime-local" className={cls} value={endDate} onChange={e => setEndDate(e.target.value)} /></div>
          </div>
          {error && <p className="text-xs text-red-500 font-semibold">{error}</p>}
        </div>
        <div className="px-5 pb-5 flex justify-end gap-2">
          <button type="button" onClick={onClose} className="px-4 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50">Hủy</button>
          <button type="submit" disabled={saving} className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-60 flex items-center gap-2">
            {saving && <Loader2 className="w-3.5 h-3.5 animate-spin" />}{isEdit ? "Cập nhật" : "Tạo"}
          </button>
        </div>
      </form>
    </div>
  );
}

// ─── Main page ──────────────────────────────────────────────────────────────
export default function AdminCampaignsPage() {
  const [campaigns, setCampaigns] = useState<CampaignResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [editCampaign, setEditCampaign] = useState<CampaignResponse | null>(null);
  const [deleting, setDeleting] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await adminGetCampaigns({ status: statusFilter || undefined, page, size: 10 });
      const data = res.data.result as { content: CampaignResponse[]; totalPages: number };
      setCampaigns(data.content ?? []);
      setTotalPages(data.totalPages ?? 0);
    } catch { /* */ }
    setLoading(false);
  }, [page, statusFilter]);

  useEffect(() => { load(); }, [load]);

  const handleDelete = async (id: string) => {
    if (!confirm("Bạn chắc chắn muốn xóa chiến dịch này?")) return;
    setDeleting(id);
    try {
      await adminDeleteCampaign(id);
      load();
    } catch { /* */ }
    setDeleting(null);
  };

  return (
    <div className="p-4 md:p-6 space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-purple-100 rounded-xl flex items-center justify-center">
            <Megaphone className="w-5 h-5 text-purple-600" />
          </div>
          <div>
            <h1 className="text-xl font-black text-slate-800">Chiến dịch khuyến mãi</h1>
            <p className="text-xs text-slate-400">Quản lý các chiến dịch giảm giá</p>
          </div>
        </div>
        <button onClick={() => { setEditCampaign(null); setShowForm(true); }}
          className="flex items-center gap-2 px-4 py-2 rounded-xl bg-purple-600 text-white text-sm font-bold hover:bg-purple-700">
          <Plus className="w-4 h-4" />Tạo chiến dịch
        </button>
      </div>

      <div className="flex gap-2 overflow-x-auto">
        {[{ v: "", l: "Tất cả" }, { v: "DRAFT", l: "Nháp" }, { v: "ACTIVE", l: "Hoạt động" }, { v: "ENDED", l: "Kết thúc" }, { v: "CANCELLED", l: "Đã hủy" }].map(f => (
          <button key={f.v} onClick={() => { setStatusFilter(f.v); setPage(0); }}
            className={`px-4 py-2 rounded-xl text-sm font-bold whitespace-nowrap ${statusFilter === f.v ? "bg-purple-600 text-white" : "bg-white text-slate-600 border border-slate-200 hover:bg-slate-50"}`}
          >{f.l}</button>
        ))}
      </div>

      {loading ? (
        <div className="flex justify-center py-16"><Loader2 className="w-8 h-8 animate-spin text-slate-300" /></div>
      ) : campaigns.length === 0 ? (
        <div className="text-center py-16">
          <Megaphone className="w-12 h-12 text-slate-200 mx-auto mb-3" />
          <p className="font-bold text-slate-500">Chưa có chiến dịch nào</p>
        </div>
      ) : (
        <div className="space-y-3">
          {campaigns.map(c => {
            const s = STATUS_MAP[c.status] ?? { label: c.status, cls: "bg-gray-100 text-gray-600" };
            return (
              <div key={c.id} className="bg-white rounded-xl border border-slate-100 p-4 hover:shadow-md transition-shadow">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="font-black text-slate-800 truncate">{c.name}</h3>
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold shrink-0 ${s.cls}`}>{s.label}</span>
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-50 text-slate-400 shrink-0">{c.campaignType === "PLATFORM" ? "Sàn" : "Shop"}</span>
                    </div>
                    {c.description && <p className="text-xs text-slate-500 line-clamp-1 mb-2">{c.description}</p>}
                    <div className="flex items-center gap-3 text-xs text-slate-400">
                      <span className="flex items-center gap-1"><Calendar className="w-3.5 h-3.5" />{fmtDate(c.startDate)} — {fmtDate(c.endDate)}</span>
                    </div>
                  </div>
                  <div className="flex items-center gap-1 shrink-0">
                    <button onClick={() => { setEditCampaign(c); setShowForm(true); }}
                      className="p-2 rounded-xl hover:bg-slate-100 text-slate-400 hover:text-purple-600">
                      <Pencil className="w-4 h-4" />
                    </button>
                    <button onClick={() => handleDelete(c.id)} disabled={deleting === c.id}
                      className="p-2 rounded-xl hover:bg-red-50 text-slate-400 hover:text-red-600 disabled:opacity-50">
                      {deleting === c.id ? <Loader2 className="w-4 h-4 animate-spin" /> : <Trash2 className="w-4 h-4" />}
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex justify-center gap-2">
          <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
            className="p-2 rounded-xl border border-slate-200 hover:bg-slate-50 disabled:opacity-40"><ChevronLeft className="w-4 h-4" /></button>
          <span className="px-4 py-2 text-sm font-bold text-slate-600">{page + 1} / {totalPages}</span>
          <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
            className="p-2 rounded-xl border border-slate-200 hover:bg-slate-50 disabled:opacity-40"><ChevronRight className="w-4 h-4" /></button>
        </div>
      )}

      {showForm && <CampaignFormModal campaign={editCampaign} onClose={() => { setShowForm(false); setEditCampaign(null); }} onSaved={() => { setShowForm(false); setEditCampaign(null); load(); }} />}
    </div>
  );
}
