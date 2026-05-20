import { useEffect, useState } from "react";
import { Loader2, Save, Store, Camera, X } from "lucide-react";
import {
  getMySellerProfile,
  updateMySellerProfile,
  type UpdateSellerProfileRequest,
} from "../../api/sellerDashboardApi";
import {
  getProvinces,
  getDistricts,
  getWards,
  type Province,
  type District,
  type Ward,
} from "../../api/shippingApi";
import type { SellerResponse } from "../../types/seller";
import ImageUpload from "../../components/ImageUpload";

// ─── Field row ─────────────────────────────────────────────────────────────────

function Field({
  label, value, onChange, placeholder, type = "text", textarea = false,
}: {
  label: string; value: string; onChange: (v: string) => void;
  placeholder?: string; type?: string; textarea?: boolean;
}) {
  const cls = "w-full px-3 py-2 rounded-xl border-2 border-slate-100 bg-slate-50 text-sm outline-none focus:border-red-400 transition-colors";
  return (
    <div>
      <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">{label}</label>
      {textarea
        ? <textarea rows={3} className={`${cls} resize-none`} value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder} />
        : <input type={type} className={cls} value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder} />
      }
    </div>
  );
}

function SelectField({
  label,
  value,
  options,
  onChange,
  placeholder,
  disabled,
}: {
  label: string;
  value: string;
  options: { value: string; label: string }[];
  onChange: (value: string) => void;
  placeholder: string;
  disabled?: boolean;
}) {
  return (
    <div>
      <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">{label}</label>
      <select
        className={`w-full px-3 py-2 rounded-xl border-2 text-sm outline-none transition-colors bg-white ${disabled ? "border-slate-200 text-slate-400 cursor-not-allowed" : "border-slate-100 focus:border-red-400"}`}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
      >
        <option value="">{placeholder}</option>
        {options.map((option) => (
          <option key={option.value} value={option.value}>{option.label}</option>
        ))}
      </select>
    </div>
  );
}

// ─── Main ──────────────────────────────────────────────────────────────────────

export default function SellerShopPage() {
  const [profile, setProfile] = useState<SellerResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving,  setSaving]  = useState(false);
  const [savingMedia, setSavingMedia] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error,   setError]   = useState<string | null>(null);

  // form state
  const [shopName,    setShopName]    = useState("");
  const [description, setDescription] = useState("");
  const [logoUrl,     setLogoUrl]     = useState("");
  const [bannerUrl,   setBannerUrl]   = useState("");
  const [phone,       setPhone]       = useState("");
  const [address,     setAddress]     = useState("");
  const [ward,        setWard]        = useState("");
  const [district,    setDistrict]    = useState("");
  const [city,        setCity]        = useState("");

  const [provinces, setProvinces] = useState<Province[]>([]);
  const [districts, setDistricts] = useState<District[]>([]);
  const [wards, setWards] = useState<Ward[]>([]);
  const [bannerModalOpen, setBannerModalOpen] = useState(false);
  const [logoModalOpen, setLogoModalOpen] = useState(false);

  useEffect(() => {
    getMySellerProfile()
      .then(p => {
        setProfile(p);
        setShopName(p.shopName ?? "");
        setDescription(p.description ?? "");
        setLogoUrl(p.logoUrl ?? "");
        setBannerUrl(p.bannerUrl ?? "");
        setPhone(p.phone ?? "");
        setAddress(p.address ?? "");
        setWard(p.ward ?? "");
        setDistrict(p.district ?? "");
        setCity(p.city ?? "");
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    getProvinces()
      .then((res) => setProvinces(res.data))
      .catch(() => setProvinces([]));
  }, []);

  useEffect(() => {
    if (!city) {
      setDistricts([]);
      return;
    }
    const selectedProvince = provinces.find((province) => province.ProvinceName === city);
    if (!selectedProvince) {
      setDistricts([]);
      return;
    }
    getDistricts(selectedProvince.ProvinceID)
      .then((res) => setDistricts(res.data))
      .catch(() => setDistricts([]));
  }, [city, provinces]);

  useEffect(() => {
    if (!district) {
      setWards([]);
      return;
    }
    const selectedDistrict = districts.find((d) => d.DistrictName === district);
    if (!selectedDistrict) {
      setWards([]);
      return;
    }
    getWards(selectedDistrict.DistrictID)
      .then((res) => setWards(res.data))
      .catch(() => setWards([]));
  }, [district, districts]);

  const autoSaveMedia = async (payload: Omit<UpdateSellerProfileRequest, "shopName">) => {
    setError(null);
    setSavingMedia(true);
    try {
      const updated = await updateMySellerProfile({
        shopName: shopName.trim() || profile?.shopName || "",
        ...payload,
      });
      setProfile(updated);
      setSuccess(true);
      setTimeout(() => setSuccess(false), 3000);
    } catch {
      setError("Lưu ảnh không thành công. Vui lòng thử lại.");
      throw new Error("Media save failed");
    } finally {
      setSavingMedia(false);
    }
  };

  const handleLogoChange = async (url: string) => {
    const previous = logoUrl;
    setLogoUrl(url);
    try {
      await autoSaveMedia({ logoUrl: url });
    } catch {
      setLogoUrl(previous);
    }
  };

  const handleBannerChange = async (url: string) => {
    const previous = bannerUrl;
    setBannerUrl(url);
    try {
      await autoSaveMedia({ bannerUrl: url });
    } catch {
      setBannerUrl(previous);
    }
  };

  const handleSave = async () => {
    if (!shopName.trim()) { setError("Tên shop không được để trống"); return; }
    setSaving(true); setError(null); setSuccess(false);
    const payload: UpdateSellerProfileRequest = {
      shopName: shopName.trim(),
      description: description || undefined,
      logoUrl: logoUrl || undefined,
      bannerUrl: bannerUrl || undefined,
      phone: phone || undefined,
      address: address || undefined,
      ward: ward || undefined,
      district: district || undefined,
      city: city || undefined,
    };
    try {
      const updated = await updateMySellerProfile(payload);
      setProfile(updated);
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
    <div className="max-w-4xl mx-auto space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-black text-slate-900">Thông tin shop</h1>
          <p className="text-sm text-slate-400 mt-0.5">Cập nhật hình ảnh và thông tin cửa hàng</p>
        </div>
        {profile && (
          <span className={`px-2.5 py-1 rounded-xl text-[11px] font-bold ${
            profile.status === "ACTIVE"
              ? "bg-green-100 text-green-700"
              : profile.status === "PENDING"
              ? "bg-yellow-100 text-yellow-700"
              : "bg-red-100 text-red-700"
          }`}>
            {profile.status}
          </span>
        )}
      </div>

      {/* Preview */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <button
          type="button"
          onClick={() => setBannerModalOpen(true)}
          className="w-full h-32 relative overflow-hidden focus:outline-none"
        >
          {bannerUrl
            ? <img src={bannerUrl} alt="banner" className="w-full h-full object-cover" />
            : <div className="w-full h-full bg-gradient-to-r from-red-600 to-red-600 flex items-center justify-center">
                <Camera className="w-6 h-6 text-white/60" />
              </div>
          }
          <div className="absolute inset-0 bg-black/10 opacity-0 hover:opacity-100 transition-opacity flex items-center justify-center">
            <span className="px-3 py-1 rounded-full bg-white/80 text-sm font-bold text-red-600">Nhấn để xem và thay đổi banner</span>
          </div>
        </button>
        <div className="relative z-10 px-5 pb-4 flex items-end gap-4 -mt-8">
          <button
            type="button"
            onClick={() => setLogoModalOpen(true)}
            className="w-16 h-16 rounded-2xl border-4 border-white bg-red-100 flex items-center justify-center overflow-hidden shadow focus:outline-none"
          >
            {logoUrl
              ? <img src={logoUrl} alt="logo" className="w-full h-full object-cover" />
              : <Store className="w-8 h-8 text-red-400" />}
            <div className="absolute inset-0 bg-black/0 hover:bg-black/20 transition" />
          </button>
          <div className="flex-1 pb-1">
            <p className="font-black text-slate-800">{shopName || "Tên shop"}</p>
            <p className="text-xs text-slate-400">{city || "Thành phố"}</p>
          </div>
        </div>
      </div>

      {bannerModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/50" onClick={() => setBannerModalOpen(false)} />
          <div className="relative w-full max-w-3xl rounded-3xl overflow-hidden bg-white shadow-2xl">
            <button
              type="button"
              onClick={() => setBannerModalOpen(false)}
              className="absolute right-4 top-4 z-10 rounded-full bg-white/90 p-2 text-slate-600 shadow hover:bg-white"
            >
              <X className="w-4 h-4" />
            </button>
            <div className="h-80 bg-slate-100">
              {bannerUrl
                ? <img src={bannerUrl} alt="banner large" className="w-full h-full object-cover" />
                : <div className="w-full h-full flex items-center justify-center text-slate-400 text-sm">Chưa có banner</div>
              }
            </div>
            <div className="p-6">
              <p className="text-sm font-semibold text-slate-700 mb-3">Banner shop</p>
              <ImageUpload label="Thay đổi banner" value={bannerUrl} onChange={handleBannerChange} folder="shops/banners" size="lg" />
            </div>
          </div>
        </div>
      )}

      {logoModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/50" onClick={() => setLogoModalOpen(false)} />
          <div className="relative w-full max-w-3xl rounded-3xl overflow-hidden bg-white shadow-2xl">
            <button
              type="button"
              onClick={() => setLogoModalOpen(false)}
              className="absolute right-4 top-4 z-10 rounded-full bg-white/90 p-2 text-slate-600 shadow hover:bg-white"
            >
              <X className="w-4 h-4" />
            </button>
            <div className="h-80 bg-slate-100 flex items-center justify-center">
              {logoUrl
                ? <img src={logoUrl} alt="logo large" className="h-56 object-contain" />
                : <div className="text-slate-400 text-sm">Chưa có logo</div>
              }
            </div>
            <div className="p-6">
              <p className="text-sm font-semibold text-slate-700 mb-3">Logo shop</p>
              <ImageUpload label="Thay đổi logo" value={logoUrl} onChange={handleLogoChange} folder="shops/logos" size="md" />
            </div>
          </div>
        </div>
      )}

      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-5 space-y-4">
        <h2 className="font-black text-slate-800 text-sm">Thông tin cơ bản</h2>
        <Field label="Tên shop *" value={shopName} onChange={setShopName} placeholder="Tên hiển thị của shop" />
        <Field label="Mô tả" value={description} onChange={setDescription} placeholder="Giới thiệu về shop của bạn..." textarea />
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-5 space-y-4">
        <h2 className="font-black text-slate-800 text-sm">Liên hệ & Địa chỉ</h2>
        <Field label="Số điện thoại" value={phone} onChange={setPhone} placeholder="0xxxxxxxxx" />
        <Field label="Số nhà, tên đường" value={address} onChange={setAddress} placeholder="Số nhà, đường..." />
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <SelectField
            label="Tỉnh/Thành phố"
            value={city}
            options={provinces.map((province) => ({ value: province.ProvinceName, label: province.ProvinceName }))}
            onChange={(value) => { setCity(value); setDistrict(""); setWard(""); }}
            placeholder="Chọn tỉnh/thành phố"
          />
          <SelectField
            label="Quận/Huyện"
            value={district}
            options={districts.map((district) => ({ value: district.DistrictName, label: district.DistrictName }))}
            onChange={(value) => { setDistrict(value); setWard(""); }}
            placeholder="Chọn quận/huyện"
            disabled={!city || districts.length === 0}
          />
          <SelectField
            label="Phường/Xã"
            value={ward}
            options={wards.map((ward) => ({ value: ward.WardName, label: ward.WardName }))}
            onChange={setWard}
            placeholder="Chọn phường/xã"
            disabled={!district || wards.length === 0}
          />
        </div>
      </div>

      {/* Static info (read-only) */}
      {profile && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-5 space-y-3">
          <h2 className="font-black text-slate-800 text-sm">Thông tin cố định</h2>
          <div className="grid grid-cols-2 gap-3 text-xs">
            {[
              { label: "Email",       value: profile.email },
              { label: "Loại shop",   value: profile.sellerType === "BUSINESS" ? "Doanh nghiệp" : "Cá nhân" },
              { label: "Rating TB",   value: profile.ratingAvg?.toFixed(1) ?? "—" },
              { label: "Đã ngày",     value: profile.approvedAt ? new Date(profile.approvedAt).toLocaleDateString("vi-VN") : "—" },
            ].map(({ label, value }) => (
              <div key={label} className="flex flex-col gap-0.5">
                <span className="text-slate-400 font-bold uppercase tracking-widest text-[10px]">{label}</span>
                <span className="font-semibold text-slate-700">{value}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {error && <p className="text-sm text-red-500 font-semibold px-1">{error}</p>}
      {success && <p className="text-sm text-green-600 font-semibold px-1">✓ Cập nhật thành công!</p>}

      <div className="flex justify-end">
        <button onClick={handleSave} disabled={saving || savingMedia}
          className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-60">
          {saving || savingMedia ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
          Lưu thay đổi
        </button>
      </div>
    </div>
  );
}
