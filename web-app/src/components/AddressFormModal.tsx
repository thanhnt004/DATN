import { useState, useEffect } from "react";
import { MapPin, X, Loader2, Check, AlertCircle } from "lucide-react";
import { getProvinces, getDistricts, getWards } from "../api/shippingApi";
import type { Province, District, Ward } from "../api/shippingApi";

// ─── Types ────────────────────────────────────────────────────────────────────

export type AddressFormValues = {
  receiverName: string;
  receiverPhone: string;
  province: string;
  district: string;
  ward: string;
  addressLine: string;
  isDefault: boolean;
};

export const EMPTY_FORM: AddressFormValues = {
  receiverName: "",
  receiverPhone: "",
  province: "",
  district: "",
  ward: "",
  addressLine: "",
  isDefault: false,
};

// ─── Validation ───────────────────────────────────────────────────────────────

export function validateAddressForm(
  f: AddressFormValues
): Partial<Record<keyof AddressFormValues, string>> {
  const errs: Partial<Record<keyof AddressFormValues, string>> = {};
  if (!f.receiverName.trim()) errs.receiverName = "Vui lòng nhập họ tên người nhận";
  if (!f.receiverPhone.trim()) errs.receiverPhone = "Vui lòng nhập số điện thoại";
  else if (!/^(0|84)(3|5|7|8|9)\d{8}$/.test(f.receiverPhone))
    errs.receiverPhone = "Số điện thoại không hợp lệ";
  if (!f.province.trim()) errs.province = "Vui lòng chọn tỉnh/thành phố";
  if (!f.district.trim()) errs.district = "Vui lòng chọn quận/huyện";
  if (!f.ward.trim()) errs.ward = "Vui lòng chọn phường/xã";
  if (!f.addressLine.trim()) errs.addressLine = "Vui lòng nhập số nhà, tên đường";
  return errs;
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function FormField({
  label,
  error,
  required,
  children,
}: {
  label: string;
  error?: string;
  required?: boolean;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-1.5">
      <label className="text-xs font-bold text-gray-500 uppercase tracking-widest">
        {label}
        {required && <span className="text-red-500 ml-0.5">*</span>}
      </label>
      {children}
      {error && <p className="text-red-500 text-[11px] font-bold">{error}</p>}
    </div>
  );
}

function TextInput({
  error,
  ...props
}: React.InputHTMLAttributes<HTMLInputElement> & { error?: string }) {
  return (
    <input
      {...props}
      className={`w-full px-4 py-2.5 rounded-xl border-2 text-sm font-semibold transition-all outline-none
        ${error ? "border-red-400 bg-red-50" : "border-gray-100 bg-gray-50 focus:border-red-400"}
        disabled:opacity-50 disabled:cursor-not-allowed`}
    />
  );
}

function SelectInput({
  error,
  options,
  value,
  onChange,
  disabled,
  placeholder,
}: {
  error?: string;
  options: { value: string; label: string }[];
  value: string;
  onChange: (val: string) => void;
  disabled?: boolean;
  placeholder?: string;
}) {
  return (
    <select
      value={value}
      onChange={(e) => onChange(e.target.value)}
      disabled={disabled}
      className={`w-full px-4 py-2.5 rounded-xl border-2 text-sm font-semibold transition-all outline-none appearance-none cursor-pointer
        ${error ? "border-red-400 bg-red-50" : "border-gray-100 bg-gray-50 focus:border-red-400"}
        disabled:opacity-50 disabled:cursor-not-allowed`}
    >
      <option value="" disabled>{placeholder || "Chọn..."}</option>
      {options.map((opt) => (
        <option key={opt.value} value={opt.value}>
          {opt.label}
        </option>
      ))}
    </select>
  );
}

// ─── Modal ────────────────────────────────────────────────────────────────────

interface AddressFormModalProps {
  open: boolean;
  onClose: () => void;
  onSave: (values: AddressFormValues, id?: string) => Promise<void>;
  initial?: { id: string } & Partial<AddressFormValues> | null;
}

export default function AddressFormModal({
  open,
  onClose,
  onSave,
  initial,
}: AddressFormModalProps) {
  const [form, setForm] = useState<AddressFormValues>(EMPTY_FORM);
  const [errors, setErrors] = useState<Partial<Record<keyof AddressFormValues, string>>>({});
  const [saving, setSaving] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  const [provinces, setProvinces] = useState<Province[]>([]);
  const [districts, setDistricts] = useState<District[]>([]);
  const [wards, setWards] = useState<Ward[]>([]);
  const [loadingLoc, setLoadingLoc] = useState(false);

  useEffect(() => {
    if (open) {
      if (initial) {
        setForm({
          receiverName: initial.receiverName ?? "",
          receiverPhone: initial.receiverPhone ?? "",
          province: initial.province ?? "",
          district: initial.district ?? "",
          ward: initial.ward ?? "",
          addressLine: initial.addressLine ?? "",
          isDefault: initial.isDefault ?? false,
        });
      } else {
        setForm(EMPTY_FORM);
      }
      setErrors({});
      setApiError(null);
      
      // Fetch provinces
      setLoadingLoc(true);
      getProvinces().then(res => setProvinces(res.data)).catch(() => {}).finally(() => setLoadingLoc(false));
    }
  }, [open, initial]);

  // Load districts when province changes
  useEffect(() => {
    if (!form.province || provinces.length === 0) return;
    const p = provinces.find(x => x.ProvinceName === form.province);
    if (p) {
      getDistricts(p.ProvinceID).then(res => setDistricts(res.data)).catch(() => {});
    } else {
      setDistricts([]);
    }
  }, [form.province, provinces]);

  // Load wards when district changes
  useEffect(() => {
    if (!form.district || districts.length === 0) return;
    const d = districts.find(x => x.DistrictName === form.district);
    if (d) {
      getWards(d.DistrictID).then(res => setWards(res.data)).catch(() => {});
    } else {
      setWards([]);
    }
  }, [form.district, districts]);

  if (!open) return null;

  const set = <K extends keyof AddressFormValues>(k: K, v: AddressFormValues[K]) => {
    setForm((f) => ({ ...f, [k]: v }));
    setErrors((e) => ({ ...e, [k]: undefined }));
  };

  const handleProvinceChange = (val: string) => {
    setForm(f => ({ ...f, province: val, district: "", ward: "" }));
    setErrors(e => ({ ...e, province: undefined, district: undefined, ward: undefined }));
  };

  const handleDistrictChange = (val: string) => {
    setForm(f => ({ ...f, district: val, ward: "" }));
    setErrors(e => ({ ...e, district: undefined, ward: undefined }));
  };

  const handleSubmit = async () => {
    const errs = validateAddressForm(form);
    if (Object.keys(errs).length > 0) {
      setErrors(errs);
      return;
    }
    setSaving(true);
    setApiError(null);
    try {
      await onSave(form, initial?.id);
      onClose();
    } catch (err) {
      const e = err as { response?: { data?: { message?: string } } };
      setApiError(e?.response?.data?.message ?? "Không thể lưu địa chỉ. Vui lòng thử lại.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center px-4 py-8">
      <div
        className="absolute inset-0 bg-black/40 backdrop-blur-sm"
        onClick={onClose}
      />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100 sticky top-0 bg-white rounded-t-2xl z-10">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-red-100 flex items-center justify-center">
              <MapPin className="w-4 h-4 text-red-600" />
            </div>
            <h3 className="font-black text-gray-900">
              {initial ? "Cập nhật địa chỉ" : "Thêm địa chỉ mới"}
            </h3>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 hover:bg-gray-100 rounded-xl transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="p-6 space-y-4">
          {apiError && (
            <div className="flex items-center gap-2.5 p-3.5 rounded-xl bg-red-50 text-red-700 text-sm font-semibold">
              <AlertCircle className="w-4 h-4 shrink-0" />
              {apiError}
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <FormField label="Họ tên người nhận" error={errors.receiverName} required>
              <TextInput
                placeholder="Nguyễn Văn A"
                value={form.receiverName}
                onChange={(e) => set("receiverName", e.target.value)}
                error={errors.receiverName}
                disabled={saving}
              />
            </FormField>
            <FormField label="Số điện thoại" error={errors.receiverPhone} required>
              <TextInput
                placeholder="0987654321"
                type="tel"
                value={form.receiverPhone}
                onChange={(e) => set("receiverPhone", e.target.value)}
                error={errors.receiverPhone}
                disabled={saving}
              />
            </FormField>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <FormField label="Tỉnh / Thành phố" error={errors.province} required>
              <SelectInput
                placeholder="Chọn Tỉnh/Thành"
                options={provinces.map(p => ({ value: p.ProvinceName, label: p.ProvinceName }))}
                value={form.province}
                onChange={handleProvinceChange}
                error={errors.province}
                disabled={saving || loadingLoc}
              />
            </FormField>
            <FormField label="Quận / Huyện" error={errors.district} required>
              <SelectInput
                placeholder="Chọn Quận/Huyện"
                options={districts.map(d => ({ value: d.DistrictName, label: d.DistrictName }))}
                value={form.district}
                onChange={handleDistrictChange}
                error={errors.district}
                disabled={saving || !form.province}
              />
            </FormField>
            <FormField label="Phường / Xã" error={errors.ward} required>
              <SelectInput
                placeholder="Chọn Phường/Xã"
                options={wards.map(w => ({ value: w.WardName, label: w.WardName }))}
                value={form.ward}
                onChange={(val) => set("ward", val)}
                error={errors.ward}
                disabled={saving || !form.district}
              />
            </FormField>
          </div>

          <FormField label="Số nhà, tên đường" error={errors.addressLine} required>
            <TextInput
              placeholder="123 Đường Láng"
              value={form.addressLine}
              onChange={(e) => set("addressLine", e.target.value)}
              error={errors.addressLine}
              disabled={saving}
            />
          </FormField>

          <label className="flex items-center gap-3 cursor-pointer group">
            <div
              onClick={() => set("isDefault", !form.isDefault)}
              className={`w-10 h-6 rounded-full relative transition-colors ${
                form.isDefault ? "bg-red-500" : "bg-gray-200"
              }`}
            >
              <span
                className={`absolute top-1 w-4 h-4 rounded-full bg-white shadow transition-all ${
                  form.isDefault ? "left-5" : "left-1"
                }`}
              />
            </div>
            <span className="text-sm font-semibold text-gray-700 group-hover:text-gray-900 select-none">
              Đặt làm địa chỉ mặc định
            </span>
          </label>
        </div>

        {/* Footer */}
        <div className="px-6 pb-6 flex gap-3">
          <button
            onClick={onClose}
            disabled={saving}
            className="flex-1 py-3 rounded-xl border-2 border-gray-100 text-sm font-bold text-gray-600 hover:bg-gray-50 transition-all"
          >
            Hủy bỏ
          </button>
          <button
            onClick={handleSubmit}
            disabled={saving}
            className="flex-1 py-3 rounded-xl bg-red-600 hover:bg-red-700 text-sm font-bold text-white transition-all disabled:opacity-60 flex items-center justify-center gap-2"
          >
            {saving ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Check className="w-4 h-4" />
            )}
            {initial ? "Lưu cập nhật" : "Thêm địa chỉ"}
          </button>
        </div>
      </div>
    </div>
  );
}
