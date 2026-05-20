import { useEffect, useState, useRef } from "react";
import {
  Search,
  Loader2,
  ChevronLeft,
  ChevronRight,
  ShieldOff,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Filter,
  RefreshCw,
} from "lucide-react";
import { adminListUsers, adminUpdateUserStatus } from "../../api/adminApi";
import type { AdminUserResponse, UserStatus, PageResponse } from "../../types/admin";

// ─── helpers ──────────────────────────────────────────────────────────────────

const STATUS_LABELS: Record<UserStatus, { label: string; color: string }> = {
  ACTIVE:      { label: "Hoạt động",    color: "bg-green-100 text-green-700" },
  LOCKED:      { label: "Bị khóa",      color: "bg-orange-100 text-orange-700" },
  BANNED:      { label: "Bị cấm",       color: "bg-red-100 text-red-700" },
  DEACTIVATED: { label: "Đã vô hiệu",   color: "bg-slate-100 text-slate-500" },
  SUSPENDED:   { label: "Tạm ngưng",    color: "bg-yellow-100 text-yellow-700" },
  DELETED:     { label: "Đã xóa",       color: "bg-slate-200 text-slate-400" },
};

function Badge({ status }: { status: UserStatus }) {
  const { label, color } = STATUS_LABELS[status] ?? { label: status, color: "bg-slate-100 text-slate-500" };
  return <span className={`inline-block px-2.5 py-0.5 rounded-full text-[11px] font-bold ${color}`}>{label}</span>;
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString("vi-VN");
}

// ─── Action modal ──────────────────────────────────────────────────────────────

interface ActionModalProps {
  user: AdminUserResponse;
  onClose: () => void;
  onDone: (updated: AdminUserResponse) => void;
}

function ActionModal({ user, onClose, onDone }: ActionModalProps) {
  const [status, setStatus] = useState<UserStatus>(user.status);
  const [reason, setReason] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const ACTIONS: { value: UserStatus; label: string; icon: React.ReactNode }[] = [
    { value: "ACTIVE",      label: "Kích hoạt",   icon: <CheckCircle2 className="w-4 h-4 text-green-600" /> },
    { value: "LOCKED",      label: "Khóa tài khoản",   icon: <ShieldOff className="w-4 h-4 text-orange-600" /> },
    { value: "SUSPENDED",   label: "Tạm ngưng",   icon: <AlertTriangle className="w-4 h-4 text-yellow-600" /> },
    { value: "BANNED",      label: "Cấm vĩnh viễn", icon: <XCircle className="w-4 h-4 text-red-600" /> },
  ];

  const handleSave = async () => {
    setSaving(true); setError(null);
    try {
      const updated = await adminUpdateUserStatus(user.id, { status, reason: reason || undefined });
      onDone(updated);
    } catch {
      setError("Không thể cập nhật. Vui lòng thử lại.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-full max-w-sm shadow-2xl">
        <div className="px-5 py-4 border-b border-slate-100">
          <h3 className="font-black text-slate-800">Cập nhật trạng thái</h3>
          <p className="text-xs text-slate-400 mt-0.5 truncate">{user.email}</p>
        </div>
        <div className="p-5 space-y-4">
          <div className="grid grid-cols-2 gap-2">
            {ACTIONS.map(({ value, label, icon }) => (
              <button
                key={value}
                onClick={() => setStatus(value)}
                className={`flex items-center gap-2 px-3 py-2.5 rounded-xl border-2 text-sm font-semibold transition-all ${
                  status === value ? "border-red-500 bg-red-50" : "border-slate-100 bg-slate-50 hover:border-slate-200"
                }`}
              >
                {icon}
                {label}
              </button>
            ))}
          </div>
          <div className="space-y-1">
            <label className="text-xs font-bold text-slate-400 uppercase tracking-widest">Lý do (tuỳ chọn)</label>
            <textarea
              rows={2}
              className="w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400 resize-none"
              placeholder="Nhập lý do..."
              value={reason}
              onChange={e => setReason(e.target.value)}
            />
          </div>
          {error && <p className="text-xs text-red-500 font-semibold">{error}</p>}
        </div>
        <div className="px-5 pb-5 flex gap-2 justify-end">
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

// ─── Main page ─────────────────────────────────────────────────────────────────

export default function AdminUsersPage() {
  const [page, setPage]             = useState<PageResponse<AdminUserResponse> | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [keyword, setKeyword]       = useState("");
  const [status, setStatus]         = useState("");
  const [loading, setLoading]       = useState(false);
  const [actionUser, setActionUser] = useState<AdminUserResponse | null>(null);
  const searchRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const load = async (p = 0, kw = keyword, st = status) => {
    setLoading(true);
    try {
      const result = await adminListUsers({ keyword: kw || undefined, status: st || undefined, page: p, size: 15 });
      setPage(result);
      setCurrentPage(p);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(0); }, []);

  const handleKeyword = (v: string) => {
    setKeyword(v);
    if (searchRef.current) clearTimeout(searchRef.current);
    searchRef.current = setTimeout(() => load(0, v, status), 400);
  };

  const handleStatus = (v: string) => {
    setStatus(v);
    load(0, keyword, v);
  };

  const handleActionDone = (updated: AdminUserResponse) => {
    setPage(prev =>
      prev
        ? { ...prev, content: prev.content.map(u => u.id === updated.id ? updated : u) }
        : prev
    );
    setActionUser(null);
  };

  return (
    <div className="space-y-5 max-w-6xl">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-xl font-black text-slate-900">Quản lý người dùng</h1>
          <p className="text-sm text-slate-400 mt-0.5">
            {page ? `${page.totalElements.toLocaleString()} tài khoản` : "—"}
          </p>
        </div>
        <button onClick={() => load(currentPage)} className="flex items-center gap-1.5 px-3 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50">
          <RefreshCw className="w-3.5 h-3.5" /> Làm mới
        </button>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-2">
        <div className="relative flex-1 min-w-48">
          <Search className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
          <input
            className="w-full pl-9 pr-4 py-2 rounded-xl border border-slate-200 bg-white text-sm outline-none focus:border-red-400"
            placeholder="Tìm theo email, username..."
            value={keyword}
            onChange={e => handleKeyword(e.target.value)}
          />
        </div>
        <div className="relative">
          <Filter className="absolute left-3 top-2.5 w-4 h-4 text-slate-400 pointer-events-none" />
          <select
            className="pl-9 pr-4 py-2 rounded-xl border border-slate-200 bg-white text-sm font-semibold outline-none focus:border-red-400 appearance-none cursor-pointer"
            value={status}
            onChange={e => handleStatus(e.target.value)}
          >
            <option value="">Tất cả trạng thái</option>
            {(Object.keys(STATUS_LABELS) as UserStatus[]).map(s => (
              <option key={s} value={s}>{STATUS_LABELS[s].label}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-16 gap-2 text-slate-400">
            <Loader2 className="w-5 h-5 animate-spin" />
            <span className="text-sm font-semibold">Đang tải...</span>
          </div>
        ) : !page || page.content.length === 0 ? (
          <div className="text-center py-16 text-slate-400 text-sm font-semibold">
            Không có dữ liệu
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50">
                  {["Người dùng", "Email", "Loại", "Trạng thái", "Ngày tạo", ""].map(h => (
                    <th key={h} className="px-4 py-3 text-left text-[11px] font-black text-slate-400 uppercase tracking-widest whitespace-nowrap">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {page.content.map(user => (
                  <tr key={user.id} className="hover:bg-slate-50 transition-colors">
                    <td className="px-4 py-3 whitespace-nowrap">
                      <div className="flex items-center gap-2.5">
                        <div className="w-8 h-8 rounded-full bg-red-100 flex items-center justify-center text-red-700 font-black text-xs shrink-0">
                          {(user.fullName || user.username).slice(0, 2).toUpperCase()}
                        </div>
                        <span className="font-semibold text-slate-800">{user.username}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-slate-500 truncate max-w-[180px]">{user.email}</td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-0.5 rounded-full text-[11px] font-bold ${user.userType === "SELLER" ? "bg-purple-100 text-purple-700" : "bg-blue-100 text-blue-700"}`}>
                        {user.userType === "SELLER" ? "Seller" : "Buyer"}
                      </span>
                    </td>
                    <td className="px-4 py-3"><Badge status={user.status} /></td>
                    <td className="px-4 py-3 text-slate-400 whitespace-nowrap">{formatDate(user.createdAt)}</td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => setActionUser(user)}
                        className="px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-red-100 text-slate-600 hover:text-red-700 text-xs font-bold transition-colors"
                      >
                        Thao tác
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {page && page.totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-slate-100">
            <span className="text-xs text-slate-400 font-semibold">
              Trang {currentPage + 1} / {page.totalPages}
            </span>
            <div className="flex gap-1">
              <button
                disabled={page.first}
                onClick={() => load(currentPage - 1)}
                className="p-1.5 rounded-lg border border-slate-200 disabled:opacity-40 hover:bg-slate-50"
              >
                <ChevronLeft className="w-4 h-4 text-slate-600" />
              </button>
              <button
                disabled={page.last}
                onClick={() => load(currentPage + 1)}
                className="p-1.5 rounded-lg border border-slate-200 disabled:opacity-40 hover:bg-slate-50"
              >
                <ChevronRight className="w-4 h-4 text-slate-600" />
              </button>
            </div>
          </div>
        )}
      </div>

      {actionUser && (
        <ActionModal
          user={actionUser}
          onClose={() => setActionUser(null)}
          onDone={handleActionDone}
        />
      )}
    </div>
  );
}
