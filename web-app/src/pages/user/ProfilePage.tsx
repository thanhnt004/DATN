import { useState, useEffect, useRef } from "react";
import {
  User,
  Mail,
  Edit3,
  Check,
  X,
  Loader2,
  AlertCircle,
  CheckCircle,
  Camera,
  ShoppingBag,
  MapPin,
} from "lucide-react";
import { getMyProfile, updateMyProfile, changeMyEmail } from "../../api/userApi";
import type { UserProfileResponse, UpdateProfileRequest, Gender } from "../../types/user";
import ImageUpload from "../../components/ImageUpload";

// ─── helpers ──────────────────────────────────────────────────────────────────

function formatDate(iso: string | null): string {
  if (!iso) return "—";
  const [y, m, d] = iso.split("-");
  return `${d}/${m}/${y}`;
}

function formatPhone(phone: string | null, masked = false): string {
  if (!phone) return "—";
  if (masked) return phone.slice(0, 3) + "****" + phone.slice(-2);
  return phone;
}

function genderLabel(g: Gender | null): string {
  if (g === "MALE") return "Nam";
  if (g === "FEMALE") return "Nữ";
  if (g === "OTHER") return "Khác";
  return "—";
}

function avatarInitials(name: string | null, username: string): string {
  const src = name || username;
  return src.slice(0, 2).toUpperCase();
}

// ─── sub-components ──────────────────────────────────────────────────────────

function InfoRow({
  label,
  value,
}: {
  label: string;
  value: React.ReactNode;
}) {
  return (
    <div className="flex items-start gap-2 py-3.5 border-b border-gray-50 last:border-0">
      <span className="w-40 shrink-0 text-sm text-gray-500">{label}</span>
      <span className="flex-1 text-sm font-semibold text-gray-800 break-all">
        {value || <span className="text-gray-300">Chưa cập nhật</span>}
      </span>
    </div>
  );
}

type InputFieldProps = React.InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  error?: string;
};
function InputField({ label, error, ...props }: InputFieldProps) {
  return (
    <div className="space-y-1.5">
      <label className="text-xs font-bold text-gray-500 uppercase tracking-widest">
        {label}
      </label>
      <input
        {...props}
        className={`w-full px-4 py-2.5 rounded-xl border-2 text-sm font-semibold transition-all outline-none
          ${error ? "border-red-400 bg-red-50" : "border-gray-100 bg-gray-50 focus:border-red-400"}
          disabled:opacity-50 disabled:cursor-not-allowed`}
      />
      {error && (
        <p className="text-red-500 text-[11px] font-bold">{error}</p>
      )}
    </div>
  );
}

// ─── main component ───────────────────────────────────────────────────────────

export default function ProfilePage() {
  const [profile, setProfile] = useState<UserProfileResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [fetchError, setFetchError] = useState<string | null>(null);

  // edit mode state
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saveStatus, setSaveStatus] = useState<"idle" | "success" | "error">("idle");
  const [saveMsg, setSaveMsg] = useState("");

  // form draft
  const [draft, setDraft] = useState<UpdateProfileRequest>({});
  const [draftErrors, setDraftErrors] = useState<Partial<Record<keyof UpdateProfileRequest, string>>>({});

  // change email
  const [showEmailModal, setShowEmailModal] = useState(false);
  const [newEmail, setNewEmail] = useState("");
  const [emailError, setEmailError] = useState("");
  const [emailSaving, setEmailSaving] = useState(false);
  const [emailStatus, setEmailStatus] = useState<"idle" | "success" | "error">("idle");
  const [emailMsg, setEmailMsg] = useState("");

  const statusTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // ── fetch profile ──────────────────────────────────────────────────────────
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await getMyProfile();
        if (!cancelled) {
          setProfile(data);
          setDraft({
            fullName: data.fullName ?? "",
            gender: data.gender ?? undefined,
            dateOfBirth: data.dateOfBirth ?? "",
            phone: data.phone ?? "",
            avatarUrl: data.avatarUrl ?? "",
          });
        }
      } catch {
        if (!cancelled) setFetchError("Không thể tải thông tin cá nhân. Vui lòng thử lại.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, []);

  // ── save profile ───────────────────────────────────────────────────────────
  const validateDraft = () => {
    const errs: typeof draftErrors = {};
    if (draft.fullName && draft.fullName.length > 100)
      errs.fullName = "Họ và tên không được quá 100 ký tự";
    if (draft.phone && !/^(0|84)(3|5|7|8|9)\d{8}$/.test(draft.phone))
      errs.phone = "Số điện thoại không hợp lệ";
    setDraftErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSave = async () => {
    if (!validateDraft()) return;
    setSaving(true);
    try {
      const updated = await updateMyProfile({
        fullName: draft.fullName || undefined,
        gender: draft.gender || undefined,
        dateOfBirth: draft.dateOfBirth || undefined,
        phone: draft.phone || undefined,
        avatarUrl: draft.avatarUrl || undefined,
      });
      setProfile(updated);
      setEditing(false);
      setSaveStatus("success");
      setSaveMsg("Cập nhật thông tin thành công!");
    } catch (err) {
      const e = err as { response?: { data?: { message?: string } } };
      setSaveStatus("error");
      setSaveMsg(e?.response?.data?.message ?? "Cập nhật thất bại. Vui lòng thử lại.");
    } finally {
      setSaving(false);
      clearTimeout(statusTimerRef.current ?? undefined);
      statusTimerRef.current = setTimeout(() => setSaveStatus("idle"), 4000);
    }
  };

  // ── change email ──────────────────────────────────────────────────────────
  const handleChangeEmail = async () => {
    if (!newEmail.trim()) {
      setEmailError("Vui lòng nhập email mới");
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(newEmail)) {
      setEmailError("Email không hợp lệ");
      return;
    }
    setEmailError("");
    setEmailSaving(true);
    try {
      const updated = await changeMyEmail({ newEmail });
      setProfile(updated);
      setEmailStatus("success");
      setEmailMsg("Email đã được cập nhật thành công!");
      setNewEmail("");
      setTimeout(() => {
        setShowEmailModal(false);
        setEmailStatus("idle");
      }, 1800);
    } catch (err) {
      const e = err as { response?: { data?: { message?: string } } };
      setEmailStatus("error");
      setEmailMsg(e?.response?.data?.message ?? "Cập nhật email thất bại.");
    } finally {
      setEmailSaving(false);
    }
  };

  // ── render ─────────────────────────────────────────────────────────────────
  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex flex-col items-center gap-3 text-gray-400">
          <Loader2 className="w-8 h-8 animate-spin text-red-500" />
          <span className="text-sm font-medium">Đang tải thông tin...</span>
        </div>
      </div>
    );
  }

  if (fetchError || !profile) {
    return (
      <div className="bg-red-50 border border-red-100 rounded-2xl p-6 flex items-center gap-4">
        <AlertCircle className="w-6 h-6 text-red-500 shrink-0" />
        <p className="text-red-700 font-semibold text-sm">{fetchError ?? "Có lỗi xảy ra"}</p>
      </div>
    );
  }

  return (
    <div className="space-y-5">
      {/* ── Hero card ──────────────────────────────────────────────────── */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5">
        <div className="flex flex-col sm:flex-row items-center sm:items-start gap-5">
          {/* Avatar */}
          <div className="relative shrink-0 group">
            <div className="w-20 h-20 rounded-full bg-gradient-to-br from-red-500 to-rose-600 flex items-center justify-center text-white text-2xl font-black shadow-lg">
              {profile.avatarUrl ? (
                <img
                  src={profile.avatarUrl}
                  alt={profile.fullName ?? "avatar"}
                  className="w-20 h-20 rounded-full object-cover"
                />
              ) : (
                avatarInitials(profile.fullName, profile.username)
              )}
            </div>
            <button className="absolute bottom-0 right-0 w-6 h-6 bg-white border-2 border-gray-200 rounded-full flex items-center justify-center shadow hover:border-red-400 transition-colors">
              <Camera className="w-3 h-3 text-gray-500" />
            </button>
          </div>

          {/* Name + info */}
          <div className="flex-1 text-center sm:text-left">
            <h1 className="text-xl font-black text-gray-900">
              {profile.fullName || profile.username}
            </h1>
            <p className="text-sm text-gray-500 mt-0.5">
              @{profile.username}
            </p>
            <div className="flex flex-wrap justify-center sm:justify-start gap-2 mt-2">
              <span className="text-xs font-bold px-2.5 py-1 bg-gray-100 text-gray-600 rounded-full">
                {profile.userType === "BUYER" ? "Người mua" : "Người bán"}
              </span>
              <span
                className={`text-xs font-bold px-2.5 py-1 rounded-full ${
                  profile.status === "ACTIVE"
                    ? "bg-green-100 text-green-700"
                    : "bg-red-100 text-red-600"
                }`}
              >
                {profile.status === "ACTIVE" ? "Hoạt động" : profile.status}
              </span>
            </div>
          </div>

          {/* Quick stats */}
          <div className="flex sm:flex-col gap-4 sm:gap-3 text-center sm:text-right shrink-0">
            <div className="flex items-center gap-2 text-sm text-gray-500">
              <ShoppingBag className="w-4 h-4 shrink-0 text-red-400" />
              <span className="font-bold text-gray-800">0</span>
              <span className="text-xs">đơn hàng</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-gray-500">
              <MapPin className="w-4 h-4 shrink-0 text-red-400" />
              <span className="font-bold text-gray-800">—</span>
              <span className="text-xs">địa chỉ</span>
            </div>
          </div>
        </div>
      </div>

      {/* ── Status banner ──────────────────────────────────────────────── */}
      {saveStatus !== "idle" && (
        <div
          className={`flex items-center gap-3 p-3.5 rounded-xl text-sm font-semibold ${
            saveStatus === "success"
              ? "bg-green-50 text-green-700"
              : "bg-red-50 text-red-700"
          }`}
        >
          {saveStatus === "success" ? (
            <CheckCircle className="w-4 h-4 shrink-0" />
          ) : (
            <AlertCircle className="w-4 h-4 shrink-0" />
          )}
          {saveMsg}
        </div>
      )}

      {/* ── Personal info card ─────────────────────────────────────────── */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm">
        <div className="flex items-center justify-between px-6 pt-5 pb-3 border-b border-gray-50">
          <h2 className="font-black text-gray-900 text-base">Thông tin cá nhân</h2>
          {!editing ? (
            <button
              onClick={() => setEditing(true)}
              className="flex items-center gap-1.5 text-sm font-bold text-red-600 hover:text-red-700 transition-colors"
            >
              <Edit3 className="w-3.5 h-3.5" />
              Cập nhật
            </button>
          ) : (
            <div className="flex gap-2">
              <button
                onClick={() => {
                  setEditing(false);
                  setDraftErrors({});
                }}
                disabled={saving}
                className="flex items-center gap-1.5 text-sm font-bold text-gray-500 hover:text-gray-700 px-3 py-1.5 rounded-lg hover:bg-gray-100 transition-all"
              >
                <X className="w-3.5 h-3.5" />
                Hủy
              </button>
              <button
                onClick={handleSave}
                disabled={saving}
                className="flex items-center gap-1.5 text-sm font-bold text-white bg-red-600 hover:bg-red-700 px-4 py-1.5 rounded-lg transition-all disabled:opacity-60"
              >
                {saving ? (
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                ) : (
                  <Check className="w-3.5 h-3.5" />
                )}
                Lưu lại
              </button>
            </div>
          )}
        </div>

        <div className="px-6 py-4">
          {!editing ? (
            /* ── View mode ─────────────────────────────────────────── */
            <div className="grid grid-cols-1 md:grid-cols-2 gap-x-12">
              <div>
                <InfoRow label="Họ và tên" value={profile.fullName} />
                <InfoRow label="Giới tính" value={genderLabel(profile.gender)} />
                <InfoRow label="Ngày sinh" value={formatDate(profile.dateOfBirth)} />
              </div>
              <div>
                <InfoRow label="Số điện thoại" value={formatPhone(profile.phone)} />
                <InfoRow
                  label="Email"
                  value={
                    <span className="flex items-center gap-2">
                      {profile.email}
                      <button
                        onClick={() => setShowEmailModal(true)}
                        className="text-xs text-red-500 font-bold hover:underline"
                      >
                        Đổi email
                      </button>
                    </span>
                  }
                />
              </div>
            </div>
          ) : (
            /* ── Edit mode ─────────────────────────────────────────── */
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <InputField
                label="Họ và tên"
                placeholder="Nguyễn Văn A"
                value={draft.fullName ?? ""}
                onChange={(e) => setDraft((d) => ({ ...d, fullName: e.target.value }))}
                error={draftErrors.fullName}
              />

              <div className="space-y-1.5">
                <label className="text-xs font-bold text-gray-500 uppercase tracking-widest">
                  Giới tính
                </label>
                <div className="flex gap-2">
                  {(["MALE", "FEMALE", "OTHER"] as Gender[]).map((g) => (
                    <button
                      key={g}
                      type="button"
                      onClick={() => setDraft((d) => ({ ...d, gender: g }))}
                      className={`flex-1 py-2.5 rounded-xl border-2 text-sm font-bold transition-all ${
                        draft.gender === g
                          ? "border-red-500 bg-red-50 text-red-600"
                          : "border-gray-100 bg-gray-50 text-gray-600 hover:border-gray-200"
                      }`}
                    >
                      {g === "MALE" ? "Nam" : g === "FEMALE" ? "Nữ" : "Khác"}
                    </button>
                  ))}
                </div>
              </div>

              <InputField
                label="Ngày sinh"
                type="date"
                value={draft.dateOfBirth ?? ""}
                onChange={(e) => setDraft((d) => ({ ...d, dateOfBirth: e.target.value }))}
              />

              <InputField
                label="Số điện thoại"
                type="tel"
                placeholder="0987654321"
                value={draft.phone ?? ""}
                onChange={(e) => setDraft((d) => ({ ...d, phone: e.target.value }))}
                error={draftErrors.phone}
              />

              <div className="md:col-span-2">
                <ImageUpload
                  label="Ảnh đại diện"
                  value={draft.avatarUrl ?? ""}
                  onChange={(url) => setDraft((d) => ({ ...d, avatarUrl: url }))}
                  folder="avatars"
                  size="md"
                />
              </div>

              <div className="md:col-span-2 bg-amber-50 border border-amber-100 rounded-xl px-4 py-3 flex items-start gap-2.5">
                <Mail className="w-4 h-4 text-amber-500 mt-0.5 shrink-0" />
                <p className="text-xs text-amber-700 font-medium">
                  Để đổi email, vui lòng nhấn nút{" "}
                  <button
                    type="button"
                    onClick={() => {
                      setEditing(false);
                      setShowEmailModal(true);
                    }}
                    className="font-bold underline hover:text-amber-900"
                  >
                    Đổi email
                  </button>{" "}
                  trong chế độ xem.
                </p>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* ── Account info card ─────────────────────────────────────────── */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm px-6 py-5">
        <h2 className="font-black text-gray-900 text-base mb-4">Thông tin tài khoản</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-x-12">
          <div>
            <InfoRow label="Tên đăng nhập" value={
              <span className="flex items-center gap-1.5">
                <User className="w-3.5 h-3.5 text-gray-400" />
                {profile.username}
              </span>
            } />
            <InfoRow label="Loại tài khoản" value={
              profile.userType === "BUYER" ? "Người mua" : "Người bán"
            } />
          </div>
          <div>
            <InfoRow label="ID tài khoản" value={
              <span className="font-mono text-xs text-gray-400">{profile.id}</span>
            } />
            <InfoRow label="Ngày tham gia" value={
              profile.createdAt
                ? new Date(profile.createdAt).toLocaleDateString("vi-VN")
                : "—"
            } />
          </div>
        </div>
      </div>

      {/* ── Change email modal ──────────────────────────────────────────── */}
      {showEmailModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center px-4">
          <div
            className="absolute inset-0 bg-black/40 backdrop-blur-sm"
            onClick={() => { setShowEmailModal(false); setEmailStatus("idle"); setEmailError(""); setNewEmail(""); }}
          />
          <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-md p-6 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="font-black text-gray-900 text-lg">Đổi địa chỉ email</h3>
              <button
                onClick={() => { setShowEmailModal(false); setEmailStatus("idle"); setEmailError(""); setNewEmail(""); }}
                className="p-1.5 hover:bg-gray-100 rounded-xl"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-sm text-gray-500">
              Email hiện tại: <span className="font-bold text-gray-800">{profile.email}</span>
            </p>

            <InputField
              label="Email mới"
              type="email"
              placeholder="newemail@example.com"
              value={newEmail}
              onChange={(e) => { setNewEmail(e.target.value); setEmailError(""); }}
              error={emailError}
              disabled={emailSaving}
            />

            {emailStatus !== "idle" && (
              <div
                className={`flex items-center gap-2.5 px-4 py-3 rounded-xl text-sm font-semibold ${
                  emailStatus === "success"
                    ? "bg-green-50 text-green-700"
                    : "bg-red-50 text-red-700"
                }`}
              >
                {emailStatus === "success" ? (
                  <CheckCircle className="w-4 h-4 shrink-0" />
                ) : (
                  <AlertCircle className="w-4 h-4 shrink-0" />
                )}
                {emailMsg}
              </div>
            )}

            <div className="flex gap-2 pt-1">
              <button
                onClick={() => { setShowEmailModal(false); setEmailStatus("idle"); setEmailError(""); setNewEmail(""); }}
                disabled={emailSaving}
                className="flex-1 py-2.5 rounded-xl border-2 border-gray-100 text-sm font-bold text-gray-600 hover:bg-gray-50 transition-all"
              >
                Hủy
              </button>
              <button
                onClick={handleChangeEmail}
                disabled={emailSaving}
                className="flex-1 py-2.5 rounded-xl bg-red-600 text-white text-sm font-bold hover:bg-red-700 transition-all disabled:opacity-60 flex items-center justify-center gap-2"
              >
                {emailSaving && <Loader2 className="w-4 h-4 animate-spin" />}
                Xác nhận đổi
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
