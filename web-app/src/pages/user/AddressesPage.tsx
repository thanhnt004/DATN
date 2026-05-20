import { useState, useEffect, useCallback } from "react";
import { Plus, MapPin, Loader2, AlertCircle, Pencil, Trash2, Star, Home } from "lucide-react";
import { getMyAddresses, createAddress, updateAddress, deleteAddress, setDefaultAddress } from "../../api/userApi";
import type { AddressResponse, CreateAddressRequest, UpdateAddressRequest } from "../../types/user";
import AddressFormModal from "../../components/AddressFormModal";
import type { AddressFormValues } from "../../components/AddressFormModal";

// ─── Sub-component: address card ─────────────────────────────────────────────

function AddressCard({
  address,
  onEdit,
  onDelete,
  onSetDefault,
  deletingId,
  defaultingId,
}: {
  address: AddressResponse;
  onEdit: (a: AddressResponse) => void;
  onDelete: (id: string) => void;
  onSetDefault: (id: string) => void;
  deletingId: string | null;
  defaultingId: string | null;
}) {
  const isDeleting = deletingId === address.id;
  const isDefaulting = defaultingId === address.id;

  return (
    <div
      className={`relative border-2 rounded-2xl p-5 transition-all ${
        address.isDefault
          ? "border-red-400 bg-red-50/30"
          : "border-gray-100 bg-white hover:border-gray-200"
      }`}
    >
      {/* Badges */}
      <div className="flex flex-wrap gap-1.5 mb-3">
        {address.isDefault && (
          <span className="flex items-center gap-1 text-xs font-bold px-2.5 py-1 bg-red-100 text-red-600 rounded-full">
            <Star className="w-3 h-3 fill-red-500" />
            Mặc định
          </span>
        )}
        <span className="flex items-center gap-1 text-xs font-bold px-2.5 py-1 bg-gray-100 text-gray-600 rounded-full">
          <Home className="w-3 h-3" />
          Nhà
        </span>
      </div>

      {/* Receiver */}
      <p className="font-black text-gray-900 text-sm">
        {address.receiverName}
        <span className="font-semibold text-gray-500 ml-2">{address.receiverPhone}</span>
      </p>

      {/* Full address */}
      <p className="text-sm text-gray-500 mt-1 leading-relaxed">
        {address.fullAddress || `${address.addressLine}, ${address.ward}, ${address.district}, ${address.province}`}
      </p>

      {/* Actions */}
      <div className="flex flex-wrap items-center gap-3 mt-4 pt-3.5 border-t border-gray-100">
        {!address.isDefault && (
          <button
            onClick={() => onSetDefault(address.id)}
            disabled={isDefaulting}
            className="flex items-center gap-1.5 text-xs font-bold text-gray-500 hover:text-red-600 transition-colors disabled:opacity-50"
          >
            {isDefaulting ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin" />
            ) : (
              <Star className="w-3.5 h-3.5" />
            )}
            Đặt mặc định
          </button>
        )}

        <button
          onClick={() => onEdit(address)}
          className="flex items-center gap-1.5 text-xs font-bold text-gray-500 hover:text-red-600 transition-colors ml-auto"
        >
          <Pencil className="w-3.5 h-3.5" />
          Cập nhật
        </button>

        <button
          onClick={() => onDelete(address.id)}
          disabled={isDeleting}
          className="flex items-center gap-1.5 text-xs font-bold text-gray-500 hover:text-red-600 transition-colors disabled:opacity-50"
        >
          {isDeleting ? (
            <Loader2 className="w-3.5 h-3.5 animate-spin" />
          ) : (
            <Trash2 className="w-3.5 h-3.5" />
          )}
          Xóa
        </button>
      </div>
    </div>
  );
}

// ─── Delete confirm modal ─────────────────────────────────────────────────────

function DeleteModal({
  open,
  onClose,
  onConfirm,
  confirming,
}: {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  confirming: boolean;
}) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center px-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-sm p-6 space-y-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-red-100 flex items-center justify-center">
            <Trash2 className="w-5 h-5 text-red-600" />
          </div>
          <div>
            <h3 className="font-black text-gray-900">Xóa địa chỉ</h3>
            <p className="text-sm text-gray-500">Hành động này không thể hoàn tác</p>
          </div>
        </div>
        <p className="text-sm text-gray-600 leading-relaxed">
          Bạn có chắc chắn muốn xóa địa chỉ này không?
        </p>
        <div className="flex gap-3">
          <button
            onClick={onClose}
            disabled={confirming}
            className="flex-1 py-2.5 rounded-xl border-2 border-gray-100 text-sm font-bold text-gray-600 hover:bg-gray-50"
          >
            Hủy
          </button>
          <button
            onClick={onConfirm}
            disabled={confirming}
            className="flex-1 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-sm font-bold text-white flex items-center justify-center gap-2 disabled:opacity-60"
          >
            {confirming && <Loader2 className="w-4 h-4 animate-spin" />}
            Xóa địa chỉ
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Main page ────────────────────────────────────────────────────────────────

export default function AddressesPage() {
  const [addresses, setAddresses] = useState<AddressResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [fetchError, setFetchError] = useState<string | null>(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<AddressResponse | null>(null);

  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);
  const [confirming, setConfirming] = useState(false);

  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [defaultingId, setDefaultingId] = useState<string | null>(null);

  // ── fetch ────────────────────────────────────────────────────────────────
  const fetchAddresses = useCallback(async () => {
    setLoading(true);
    setFetchError(null);
    try {
      const list = await getMyAddresses();
      setAddresses(list);
    } catch {
      setFetchError("Không thể tải danh sách địa chỉ. Vui lòng thử lại.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchAddresses(); }, [fetchAddresses]);

  // ── handlers ─────────────────────────────────────────────────────────────
  const handleSave = async (values: AddressFormValues, id?: string) => {
    if (id) {
      const payload: UpdateAddressRequest = {
        receiverName: values.receiverName,
        receiverPhone: values.receiverPhone,
        province: values.province,
        district: values.district,
        ward: values.ward,
        addressLine: values.addressLine,
      };
      const updated = await updateAddress(id, payload);
      // if marking as default, call setDefault too
      if (values.isDefault && !addresses.find((a) => a.id === id)?.isDefault) {
        await setDefaultAddress(id);
      }
      setAddresses((prev) =>
        prev.map((a) => (a.id === updated.id ? updated : a))
      );
      // refresh to sync default flags
      await fetchAddresses();
    } else {
      const payload: CreateAddressRequest = {
        receiverName: values.receiverName,
        receiverPhone: values.receiverPhone,
        province: values.province,
        district: values.district,
        ward: values.ward,
        addressLine: values.addressLine,
        isDefault: values.isDefault,
      };
      await createAddress(payload);
      await fetchAddresses();
    }
  };

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;
    setConfirming(true);
    setDeletingId(deleteTarget);
    try {
      await deleteAddress(deleteTarget);
      await fetchAddresses();
    } catch {
      // ignore - address may be protected
    } finally {
      setConfirming(false);
      setDeletingId(null);
      setDeleteTarget(null);
    }
  };

  const handleSetDefault = async (id: string) => {
    setDefaultingId(id);
    try {
      await setDefaultAddress(id);
      await fetchAddresses();
    } finally {
      setDefaultingId(null);
    }
  };

  // ── render ───────────────────────────────────────────────────────────────
  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm px-6 py-4 flex items-center justify-between">
        <div>
          <h1 className="font-black text-gray-900 text-lg">Sổ địa chỉ</h1>
          <p className="text-sm text-gray-500 mt-0.5">
            {addresses.length} địa chỉ · Tối đa 10 địa chỉ
          </p>
        </div>
        <button
          onClick={() => { setEditTarget(null); setModalOpen(true); }}
          disabled={addresses.length >= 10}
          className="flex items-center gap-2 px-4 py-2.5 bg-red-600 hover:bg-red-700 text-white text-sm font-bold rounded-xl transition-all disabled:opacity-50 disabled:cursor-not-allowed shadow-sm"
        >
          <Plus className="w-4 h-4" />
          Thêm địa chỉ
        </button>
      </div>

      {/* Content */}
      {loading ? (
        <div className="flex items-center justify-center h-48">
          <Loader2 className="w-7 h-7 animate-spin text-red-500" />
        </div>
      ) : fetchError ? (
        <div className="bg-red-50 border border-red-100 rounded-2xl p-6 flex items-center gap-4">
          <AlertCircle className="w-5 h-5 text-red-500 shrink-0" />
          <p className="text-red-700 font-semibold text-sm">{fetchError}</p>
        </div>
      ) : addresses.length === 0 ? (
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm flex flex-col items-center justify-center py-16 gap-4">
          <div className="w-16 h-16 bg-gray-100 rounded-2xl flex items-center justify-center">
            <MapPin className="w-8 h-8 text-gray-300" />
          </div>
          <div className="text-center">
            <p className="font-bold text-gray-700">Chưa có địa chỉ nào</p>
            <p className="text-sm text-gray-400 mt-1">
              Thêm địa chỉ để việc đặt hàng trở nên dễ dàng hơn
            </p>
          </div>
          <button
            onClick={() => { setEditTarget(null); setModalOpen(true); }}
            className="flex items-center gap-2 px-5 py-2.5 bg-red-600 hover:bg-red-700 text-white text-sm font-bold rounded-xl transition-all"
          >
            <Plus className="w-4 h-4" />
            Thêm địa chỉ đầu tiên
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {/* Default address first */}
          {[...addresses].sort((a, b) => (a.isDefault === b.isDefault ? 0 : a.isDefault ? -1 : 1)).map((addr) => (
            <AddressCard
              key={addr.id}
              address={addr}
              onEdit={(a) => { setEditTarget(a); setModalOpen(true); }}
              onDelete={(id) => setDeleteTarget(id)}
              onSetDefault={handleSetDefault}
              deletingId={deletingId}
              defaultingId={defaultingId}
            />
          ))}
        </div>
      )}

      {/* Address form modal */}
      <AddressFormModal
        open={modalOpen}
        onClose={() => { setModalOpen(false); setEditTarget(null); }}
        onSave={handleSave}
        initial={editTarget}
      />

      {/* Delete confirm modal */}
      <DeleteModal
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDeleteConfirm}
        confirming={confirming}
      />
    </div>
  );
}
