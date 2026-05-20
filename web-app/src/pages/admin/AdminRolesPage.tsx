import { useEffect, useRef, useState, useCallback } from "react";
import {
  Loader2, Plus, Pencil, Trash2, Users,
  X, RefreshCw, KeyRound, ChevronRight, UserMinus, UserPlus, Search,
} from "lucide-react";
import {
  getAllRoles,
  createRole,
  updateRole,
  deleteRole,
  getUsersByRole,
  assignRoleToUser,
  removeRoleFromUser,
} from "../../api/roleApi";
import { adminListUsers } from "../../api/adminApi";
import type {
  RoleResponse,
  UserWithRolesResponse,
  CreateRoleRequest,
  UpdateRoleRequest,
} from "../../types/role";
import type { AdminUserResponse } from "../../types/admin";

// ─── Role Form Modal ───────────────────────────────────────────────────────────

interface RoleFormModalProps {
  mode: "create" | "edit";
  initial?: RoleResponse;
  onClose: () => void;
  onSave: (data: CreateRoleRequest | UpdateRoleRequest, id?: string) => Promise<void>;
}

function RoleFormModal({ mode, initial, onClose, onSave }: RoleFormModalProps) {
  const [name, setName]               = useState(initial?.name ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [saving, setSaving]           = useState(false);
  const [error, setError]             = useState<string | null>(null);

  const handleSave = async () => {
    if (!name.trim()) { setError("Tên role không được để trống"); return; }
    setSaving(true); setError(null);
    try {
      await onSave({ name: name.trim(), description: description.trim() || undefined }, initial?.id);
    } catch {
      setError("Lưu thất bại. Vui lòng thử lại.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-full max-w-sm shadow-2xl">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
          <h3 className="font-black text-slate-800">{mode === "create" ? "Tạo role mới" : "Chỉnh sửa role"}</h3>
          <button onClick={onClose} className="p-1 rounded-lg hover:bg-slate-100"><X className="w-5 h-5 text-slate-400" /></button>
        </div>
        <div className="p-5 space-y-4">
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">
              Tên role <span className="text-red-500">*</span>
            </label>
            <input autoFocus type="text"
              className="w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400"
              value={name} onChange={e => setName(e.target.value)}
              placeholder="VD: ADMIN, MODERATOR..." />
          </div>
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Mô tả</label>
            <textarea rows={2}
              className="w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400 resize-none"
              value={description} onChange={e => setDescription(e.target.value)}
              placeholder="Mô tả quyền hạn của role..." />
          </div>
          {error && <p className="text-xs text-red-500 font-semibold">{error}</p>}
        </div>
        <div className="px-5 pb-5 flex justify-end gap-2">
          <button onClick={onClose} className="px-4 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50">Hủy</button>
          <button onClick={handleSave} disabled={saving}
            className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-60 flex items-center gap-2">
            {saving && <Loader2 className="w-3.5 h-3.5 animate-spin" />}Lưu
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Delete Confirm Modal ──────────────────────────────────────────────────────

function DeleteRoleModal({ role, onClose, onDelete }: { role: RoleResponse; onClose: () => void; onDelete: () => Promise<void> }) {
  const [loading, setLoading] = useState(false);
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-full max-w-sm shadow-2xl p-6">
        <h3 className="font-black text-slate-800 mb-2">Xoá role</h3>
        <p className="text-sm text-slate-500 mb-4">Bạn có chắc muốn xoá role <strong>"{role.name}"</strong>? Thao tác này không thể hoàn tác.</p>
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

// ─── Assign Role Modal ─────────────────────────────────────────────────────────

function AssignRoleModal({ role, onClose, onAssigned }: { role: RoleResponse; onClose: () => void; onAssigned: () => void }) {
  const [keyword, setKeyword]             = useState("");
  const [users, setUsers]                 = useState<AdminUserResponse[]>([]);
  const [searching, setSearching]         = useState(false);
  const [selectedUser, setSelectedUser]   = useState<AdminUserResponse | null>(null);
  const [saving, setSaving]               = useState(false);
  const [error, setError]                 = useState<string | null>(null);
  const [hasSearched, setHasSearched]     = useState(false);
  const debounceRef                       = useRef<ReturnType<typeof setTimeout> | null>(null);

  const searchUsers = useCallback(async (kw: string) => {
    if (!kw.trim()) { setUsers([]); setHasSearched(false); return; }
    setSearching(true); setError(null);
    try {
      const page = await adminListUsers({ keyword: kw.trim(), size: 10 });
      setUsers(page.content);
      setHasSearched(true);
    } catch {
      setError("Tìm kiếm thất bại.");
    } finally {
      setSearching(false);
    }
  }, []);

  const handleKeywordChange = (value: string) => {
    setKeyword(value);
    setSelectedUser(null);
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => searchUsers(value), 400);
  };

  const handleAssign = async () => {
    if (!selectedUser) { setError("Vui lòng chọn người dùng"); return; }
    if (!selectedUser.authId) { setError("Người dùng này chưa có Keycloak ID"); return; }
    setSaving(true); setError(null);
    try {
      await assignRoleToUser({ userId: selectedUser.authId, roleName: role.name, roleId: role.id });
      onAssigned();
    } catch {
      setError("Gán role thất bại. Vui lòng thử lại.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-full max-w-md shadow-2xl flex flex-col max-h-[80vh]">
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100 shrink-0">
          <div>
            <h3 className="font-black text-slate-800">Gán role</h3>
            <p className="text-xs text-slate-400 mt-0.5">Role: <span className="font-bold text-red-600">{role.name}</span></p>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg hover:bg-slate-100"><X className="w-5 h-5 text-slate-400" /></button>
        </div>

        {/* Search */}
        <div className="px-5 pt-4 pb-2 shrink-0">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
            <input
              autoFocus type="text"
              className="w-full pl-9 pr-3 py-2.5 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400"
              value={keyword} onChange={e => handleKeywordChange(e.target.value)}
              placeholder="Tìm theo tên, email hoặc username..."
            />
            {searching && <Loader2 className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 animate-spin text-slate-400" />}
          </div>
        </div>

        {/* User list */}
        <div className="flex-1 overflow-y-auto px-5 pb-2 min-h-0">
          {!hasSearched && !searching && (
            <p className="text-xs text-slate-400 text-center py-6">Nhập tên hoặc email để tìm người dùng</p>
          )}
          {hasSearched && users.length === 0 && !searching && (
            <p className="text-xs text-slate-400 text-center py-6">Không tìm thấy người dùng nào</p>
          )}
          {users.length > 0 && (
            <div className="space-y-1 py-1">
              {users.map(u => {
                const isSelected = selectedUser?.id === u.id;
                return (
                  <button
                    key={u.id}
                    onClick={() => setSelectedUser(isSelected ? null : u)}
                    className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-left transition-colors ${
                      isSelected
                        ? "bg-red-50 border-2 border-red-300"
                        : "hover:bg-slate-50 border-2 border-transparent"
                    }`}
                  >
                    {/* Avatar */}
                    <div className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 ${
                      isSelected ? "bg-red-200" : "bg-red-100"
                    }`}>
                      {u.avatarUrl ? (
                        <img src={u.avatarUrl} alt="" className="w-9 h-9 rounded-xl object-cover" />
                      ) : (
                        <span className="text-red-600 font-black text-sm">
                          {(u.fullName?.[0] || u.username?.[0] || "?").toUpperCase()}
                        </span>
                      )}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-bold text-sm text-slate-800 truncate">
                        {u.fullName || u.username}
                      </p>
                      <p className="text-xs text-slate-400 truncate">{u.email}</p>
                    </div>
                    <span className={`px-1.5 py-0.5 rounded-full text-[10px] font-bold ${
                      u.userType === "SELLER" ? "bg-purple-100 text-purple-600" : "bg-blue-100 text-blue-600"
                    }`}>
                      {u.userType}
                    </span>
                  </button>
                );
              })}
            </div>
          )}
        </div>

        {/* Selected user info + actions */}
        <div className="px-5 pb-5 pt-2 border-t border-slate-100 shrink-0">
          {selectedUser && (
            <div className="flex items-center gap-2 mb-3 px-3 py-2 rounded-xl bg-green-50 border border-green-200">
              <span className="text-xs text-green-700 font-semibold truncate">
                Đã chọn: <strong>{selectedUser.fullName || selectedUser.username}</strong> ({selectedUser.email})
              </span>
            </div>
          )}
          {error && <p className="text-xs text-red-500 font-semibold mb-2">{error}</p>}
          <div className="flex justify-end gap-2">
            <button onClick={onClose} className="px-4 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50">Hủy</button>
            <button onClick={handleAssign} disabled={saving || !selectedUser}
              className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-60 flex items-center gap-2">
              {saving && <Loader2 className="w-3.5 h-3.5 animate-spin" />}Gán role
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Users Panel (slide-in) ────────────────────────────────────────────────────

function UsersPanel({
  role, onClose,
}: { role: RoleResponse; onClose: () => void }) {
  const [users, setUsers]   = useState<UserWithRolesResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [removing, setRemoving] = useState<string | null>(null);
  const [showAssign, setShowAssign] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      setUsers(await getUsersByRole(role.name));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [role.name]);

  const handleRemove = async (userId: string) => {
    setRemoving(userId);
    try {
      await removeRoleFromUser(userId, role.name);
      setUsers(prev => prev.filter(u => u.userId !== userId));
    } finally {
      setRemoving(null);
    }
  };

  return (
    <>
      <div className="fixed inset-0 z-40 bg-black/20" onClick={onClose} />
      <div className="fixed right-0 top-0 h-full w-full max-w-md z-50 bg-white shadow-2xl flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
          <div>
            <h3 className="font-black text-slate-800">Người dùng có role</h3>
            <p className="text-xs text-slate-400 mt-0.5">
              <span className="font-bold text-red-600">{role.name}</span>
              {!loading && <span> · {users.length} người dùng</span>}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button onClick={() => setShowAssign(true)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-red-600 hover:bg-red-700 text-white text-xs font-bold">
              <UserPlus className="w-3.5 h-3.5" />Gán role
            </button>
            <button onClick={onClose} className="p-1.5 rounded-xl hover:bg-slate-100"><X className="w-5 h-5 text-slate-400" /></button>
          </div>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto">
          {loading ? (
            <div className="flex items-center justify-center py-16 gap-2 text-slate-400">
              <Loader2 className="w-5 h-5 animate-spin" />
              <span className="text-sm">Đang tải...</span>
            </div>
          ) : users.length === 0 ? (
            <div className="text-center py-16 text-slate-400 text-sm">Chưa có người dùng nào có role này</div>
          ) : (
            <div className="divide-y divide-slate-50">
              {users.map(u => (
                <div key={u.userId} className="flex items-center gap-3 px-5 py-3 hover:bg-slate-50">
                  {/* Avatar */}
                  <div className="w-9 h-9 rounded-xl bg-red-100 flex items-center justify-center shrink-0">
                    <span className="text-red-600 font-black text-sm">
                      {(u.firstName?.[0] || u.username?.[0] || "?").toUpperCase()}
                    </span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-bold text-sm text-slate-800 truncate">
                      {u.firstName || u.lastName ? `${u.firstName} ${u.lastName}`.trim() : u.username}
                    </p>
                    <p className="text-xs text-slate-400 truncate">{u.email}</p>
                    {/* role chips */}
                    <div className="flex flex-wrap gap-1 mt-1">
                      {u.roles.map(r => (
                        <span key={r} className={`px-1.5 py-0.5 rounded-full text-[10px] font-bold ${
                          r === role.name
                            ? "bg-red-100 text-red-700"
                            : "bg-slate-100 text-slate-500"
                        }`}>{r}</span>
                      ))}
                    </div>
                  </div>
                  <button
                    onClick={() => handleRemove(u.userId)}
                    disabled={removing === u.userId}
                    title="Gỡ role khỏi người dùng này"
                    className="p-1.5 rounded-lg hover:bg-red-100 text-slate-400 hover:text-red-500 transition-colors"
                  >
                    {removing === u.userId
                      ? <Loader2 className="w-4 h-4 animate-spin" />
                      : <UserMinus className="w-4 h-4" />
                    }
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {showAssign && (
        <AssignRoleModal
          role={role}
          onClose={() => setShowAssign(false)}
          onAssigned={() => { setShowAssign(false); load(); }}
        />
      )}
    </>
  );
}

// ─── Main Page ─────────────────────────────────────────────────────────────────

type Modal = "create" | "edit" | "delete" | null;

export default function AdminRolesPage() {
  const [roles, setRoles]         = useState<RoleResponse[]>([]);
  const [loading, setLoading]     = useState(false);
  const [modal, setModal]         = useState<Modal>(null);
  const [selected, setSelected]   = useState<RoleResponse | null>(null);
  const [usersPanel, setUsersPanel] = useState<RoleResponse | null>(null);

  const load = async () => {
    setLoading(true);
    try { setRoles(await getAllRoles()); }
    finally { setLoading(false); }
  };

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { load(); }, []);

  const handleSave = async (data: CreateRoleRequest | UpdateRoleRequest, id?: string) => {
    if (modal === "create") await createRole(data as CreateRoleRequest);
    else if (id)            await updateRole(id, data as UpdateRoleRequest);
    setModal(null); setSelected(null);
    load();
  };

  const handleDelete = async () => {
    if (!selected) return;
    await deleteRole(selected.id);
    setModal(null); setSelected(null);
    load();
  };

  // System roles from Keycloak that shouldn't be deleted but can be viewed
  const isSystemRole = (name: string) =>
    ["default-roles-ecommerce", "offline_access", "uma_authorization"].includes(name);

  return (
    <div className="space-y-5 max-w-4xl">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-xl font-black text-slate-900">Quản lý Roles</h1>
          <p className="text-sm text-slate-400 mt-0.5">{loading ? "..." : `${roles.length} roles`}</p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={load} className="flex items-center gap-1.5 px-3 py-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50">
            <RefreshCw className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={() => { setSelected(null); setModal("create"); }}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold"
          >
            <Plus className="w-3.5 h-3.5" />Tạo role
          </button>
        </div>
      </div>

      {/* Roles grid */}
      {loading ? (
        <div className="flex items-center justify-center py-16 gap-2 text-slate-400">
          <Loader2 className="w-5 h-5 animate-spin" />
          <span className="text-sm font-semibold">Đang tải...</span>
        </div>
      ) : roles.length === 0 ? (
        <div className="text-center py-16 text-slate-400 text-sm font-semibold">Không có role nào</div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {roles.map(role => {
            const system = isSystemRole(role.name);
            return (
              <div key={role.id}
                className="bg-white rounded-2xl border border-slate-200 shadow-sm p-4 flex items-start gap-3 hover:border-red-200 transition-colors">
                {/* Icon */}
                <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${
                  system ? "bg-slate-100" : "bg-red-100"
                }`}>
                  <KeyRound className={`w-5 h-5 ${system ? "text-slate-400" : "text-red-600"}`} />
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-black text-slate-800 text-sm">{role.name}</span>
                    {role.composite && (
                      <span className="px-1.5 py-0.5 rounded-full text-[10px] font-bold bg-purple-100 text-purple-600">Composite</span>
                    )}
                    {system && (
                      <span className="px-1.5 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-500">System</span>
                    )}
                  </div>
                  {role.description && (
                    <p className="text-xs text-slate-400 mt-0.5 truncate">{role.description}</p>
                  )}
                  <p className="text-[10px] text-slate-300 font-mono mt-1 truncate">{role.id}</p>
                </div>

                {/* Actions */}
                <div className="flex items-center gap-0.5 shrink-0">
                  <button onClick={() => setUsersPanel(role)} title="Xem người dùng"
                    className="p-1.5 rounded-lg hover:bg-red-50 text-slate-400 hover:text-red-600 transition-colors">
                    <Users className="w-4 h-4" />
                  </button>
                  {!system && (
                    <>
                      <button onClick={() => { setSelected(role); setModal("edit"); }} title="Chỉnh sửa"
                        className="p-1.5 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-700 transition-colors">
                        <Pencil className="w-4 h-4" />
                      </button>
                      <button onClick={() => { setSelected(role); setModal("delete"); }} title="Xoá"
                        className="p-1.5 rounded-lg hover:bg-red-100 text-slate-400 hover:text-red-500 transition-colors">
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </>
                  )}
                  <button onClick={() => setUsersPanel(role)} title="Xem thành viên"
                    className="p-1.5 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-600 transition-colors">
                    <ChevronRight className="w-4 h-4" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Modals */}
      {(modal === "create" || modal === "edit") && (
        <RoleFormModal
          mode={modal}
          initial={modal === "edit" ? selected ?? undefined : undefined}
          onClose={() => { setModal(null); setSelected(null); }}
          onSave={handleSave}
        />
      )}
      {modal === "delete" && selected && (
        <DeleteRoleModal
          role={selected}
          onClose={() => { setModal(null); setSelected(null); }}
          onDelete={handleDelete}
        />
      )}

      {/* Users slide-in panel */}
      {usersPanel && (
        <UsersPanel role={usersPanel} onClose={() => setUsersPanel(null)} />
      )}
    </div>
  );
}
