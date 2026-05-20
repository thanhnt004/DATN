import { useEffect, useState } from "react";
import {
  Loader2, Save, Landmark, CheckCircle2, AlertCircle,
} from "lucide-react";
import { getMyBankAccount, updateMyBankAccount } from "../../api/sellerDashboardApi";
import type { BankAccountResponse, UpdateBankAccountRequest } from "../../types/seller";

// ─── Field helper ──────────────────────────────────────────────────────────────

function Field({
  label, value, onChange, placeholder, required = false, mono = false,
}: {
  label: string; value: string; onChange: (v: string) => void;
  placeholder?: string; required?: boolean; mono?: boolean;
}) {
  const cls = `w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400 transition-colors ${mono ? "font-mono" : ""}`;
  return (
    <div>
      <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">
        {label} {required && <span className="text-red-500">*</span>}
      </label>
      <input className={cls} value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder} />
    </div>
  );
}

// ─── Main page ─────────────────────────────────────────────────────────────────

export default function SellerBankAccountPage() {
  const [account, setAccount] = useState<BankAccountResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [bankName, setBankName] = useState("");
  const [bankCode, setBankCode] = useState("");
  const [branchName, setBranchName] = useState("");
  const [accountNumber, setAccountNumber] = useState("");
  const [accountHolderName, setAccountHolderName] = useState("");

  useEffect(() => {
    getMyBankAccount()
      .then(acc => {
        setAccount(acc);
        if (acc) {
          setBankName(acc.bankName ?? "");
          setBankCode(acc.bankCode ?? "");
          setBranchName(acc.branchName ?? "");
          setAccountNumber(acc.accountNumber ?? "");
          setAccountHolderName(acc.accountHolderName ?? "");
        }
      })
      .finally(() => setLoading(false));
  }, []);

  const handleSave = async () => {
    if (!bankName.trim()) { setError("Tên ngân hàng không được để trống"); return; }
    if (!accountNumber.trim()) { setError("Số tài khoản không được để trống"); return; }
    if (!accountHolderName.trim()) { setError("Tên chủ tài khoản không được để trống"); return; }

    setSaving(true);
    setError(null);
    setSuccess(false);
    const payload: UpdateBankAccountRequest = {
      bankName: bankName.trim(),
      bankCode: bankCode.trim() || undefined,
      branchName: branchName.trim() || undefined,
      accountNumber: accountNumber.trim(),
      accountHolderName: accountHolderName.trim(),
    };
    try {
      const updated = await updateMyBankAccount(payload);
      setAccount(updated);
      setSuccess(true);
      setTimeout(() => setSuccess(false), 3000);
    } catch {
      setError("Cập nhật thất bại. Vui lòng thử lại.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24 gap-2 text-slate-400">
        <Loader2 className="w-5 h-5 animate-spin" /><span className="text-sm font-semibold">Đang tải...</span>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-black text-slate-900">Tài khoản ngân hàng</h1>
          <p className="text-sm text-slate-400 mt-0.5">Thông tin nhận thanh toán từ đơn hàng</p>
        </div>
        {account?.isVerified ? (
          <span className="flex items-center gap-1 px-2.5 py-1 rounded-xl text-[11px] font-bold bg-green-100 text-green-700">
            <CheckCircle2 className="w-3 h-3" /> Đã xác minh
          </span>
        ) : account ? (
          <span className="flex items-center gap-1 px-2.5 py-1 rounded-xl text-[11px] font-bold bg-yellow-100 text-yellow-700">
            <AlertCircle className="w-3 h-3" /> Chưa xác minh
          </span>
        ) : null}
      </div>

      {/* Preview card */}
      <div className="bg-gradient-to-br from-slate-800 to-slate-900 rounded-2xl p-5 text-white shadow-lg">
        <div className="flex items-start justify-between mb-6">
          <Landmark className="w-8 h-8 text-red-400" />
          <span className="text-xs font-bold text-slate-400 uppercase tracking-widest">
            {bankName || "Ngân hàng"}
          </span>
        </div>
        <p className="font-mono text-lg tracking-widest mb-4">
          {accountNumber ? accountNumber.replace(/(.{4})/g, "$1 ").trim() : "•••• •••• •••• ••••"}
        </p>
        <div className="flex justify-between items-end">
          <div>
            <p className="text-[10px] text-slate-400 uppercase tracking-widest">Chủ tài khoản</p>
            <p className="text-sm font-bold">{accountHolderName || "—"}</p>
          </div>
          {branchName && (
            <div className="text-right">
              <p className="text-[10px] text-slate-400 uppercase tracking-widest">Chi nhánh</p>
              <p className="text-sm font-bold">{branchName}</p>
            </div>
          )}
        </div>
      </div>

      {/* Form */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-5 space-y-4">
        <h2 className="font-black text-slate-800 text-sm">Thông tin ngân hàng</h2>
        <Field label="Tên ngân hàng" value={bankName} onChange={setBankName}
          placeholder="VD: Vietcombank, BIDV, Techcombank..." required />
        <div className="grid grid-cols-2 gap-3">
          <Field label="Mã ngân hàng" value={bankCode} onChange={setBankCode}
            placeholder="VD: VCB, BIDV, TCB" mono />
          <Field label="Chi nhánh" value={branchName} onChange={setBranchName}
            placeholder="VD: Hồ Chí Minh" />
        </div>
        <Field label="Số tài khoản" value={accountNumber} onChange={setAccountNumber}
          placeholder="Nhập số tài khoản" required mono />
        <Field label="Tên chủ tài khoản" value={accountHolderName} onChange={setAccountHolderName}
          placeholder="Tên in trên tài khoản (viết hoa)" required />
      </div>

      {error && <p className="text-sm text-red-500 font-semibold px-1">{error}</p>}
      {success && <p className="text-sm text-green-600 font-semibold px-1">✓ Cập nhật thành công!</p>}

      <div className="flex justify-end">
        <button onClick={handleSave} disabled={saving}
          className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-60">
          {saving ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
          Lưu thay đổi
        </button>
      </div>
    </div>
  );
}
