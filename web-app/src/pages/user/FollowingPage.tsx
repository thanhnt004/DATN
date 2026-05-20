import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  Store, Star, Package, Users, Loader2, Heart, HeartOff,
} from "lucide-react";
import { getFollowedSellers, getSellerPublic, unfollowSeller } from "../../api/sellerPublicApi";
import type { SellerResponse } from "../../types/seller";
import toast from "react-hot-toast";

function fmt(n: number | null | undefined) {
  return (n ?? 0).toLocaleString("vi-VN");
}

export default function FollowingPage() {
  const [sellers, setSellers] = useState<SellerResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [unfollowingId, setUnfollowingId] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const { data } = await getFollowedSellers();
        const ids: string[] = data.result ?? [];
        if (ids.length === 0) {
          setSellers([]);
          return;
        }
        const details = await Promise.all(
          ids.map(id =>
            getSellerPublic(id)
              .then(r => r.data.result)
              .catch(() => null)
          )
        );
        setSellers(details.filter((s): s is SellerResponse => s !== null));
      } catch {
        // ignore
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const handleUnfollow = async (sellerId: string) => {
    setUnfollowingId(sellerId);
    try {
      await unfollowSeller(sellerId);
      setSellers(prev => prev.filter(s => s.id !== sellerId));
      toast.success("Đã bỏ theo dõi");
    } catch {
      toast.error("Không thể bỏ theo dõi");
    } finally {
      setUnfollowingId(null);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24 gap-2 text-slate-400">
        <Loader2 className="w-5 h-5 animate-spin" />
        <span className="text-sm font-semibold">Đang tải...</span>
      </div>
    );
  }

  return (
    <div className="space-y-5 max-w-3xl">
      <div>
        <h1 className="text-xl font-black text-slate-900">Shop đang theo dõi</h1>
        <p className="text-sm text-slate-400 mt-0.5">
          {sellers.length > 0
            ? `Bạn đang theo dõi ${sellers.length} shop`
            : "Bạn chưa theo dõi shop nào"}
        </p>
      </div>

      {sellers.length === 0 && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-10 text-center">
          <Heart className="w-10 h-10 text-slate-200 mx-auto mb-3" />
          <p className="text-sm text-slate-400 font-semibold">
            Chưa có shop nào trong danh sách theo dõi
          </p>
          <Link
            to="/"
            className="inline-block mt-4 px-5 py-2 rounded-xl bg-red-600 text-white text-sm font-bold hover:bg-red-700 transition-colors"
          >
            Khám phá ngay
          </Link>
        </div>
      )}

      <div className="space-y-3">
        {sellers.map(seller => (
          <div
            key={seller.id}
            className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden hover:shadow-md transition-shadow"
          >
            {/* Banner */}
            <div className="h-16 bg-gradient-to-r from-red-500 to-red-400 relative">
              {seller.bannerUrl && (
                <img
                  src={seller.bannerUrl}
                  alt="banner"
                  className="w-full h-full object-cover"
                />
              )}
            </div>

            <div className="px-4 pb-4 flex items-start gap-3 -mt-5 relative z-10">
              {/* Logo */}
              <Link to={`/shop/${seller.id}`} className="shrink-0">
                <div className="w-12 h-12 rounded-xl border-3 border-white bg-white flex items-center justify-center overflow-hidden shadow-md">
                  {seller.logoUrl ? (
                    <img
                      src={seller.logoUrl}
                      alt={seller.shopName}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <Store className="w-6 h-6 text-red-600" />
                  )}
                </div>
              </Link>

              {/* Info */}
              <div className="flex-1 min-w-0 pt-6">
                <Link
                  to={`/shop/${seller.id}`}
                  className="text-sm font-black text-slate-800 hover:text-red-600 transition-colors truncate block"
                >
                  {seller.shopName}
                </Link>

                <div className="flex flex-wrap items-center gap-x-4 gap-y-1 mt-1.5">
                  <span className="flex items-center gap-1 text-xs text-slate-400">
                    <Package className="w-3 h-3" />
                    {fmt(seller.totalProducts)} sản phẩm
                  </span>
                  <span className="flex items-center gap-1 text-xs text-slate-400">
                    <Star className="w-3 h-3 text-yellow-500" />
                    {seller.ratingAvg?.toFixed(1) ?? "—"} ({fmt(seller.ratingCount)})
                  </span>
                  <span className="flex items-center gap-1 text-xs text-slate-400">
                    <Users className="w-3 h-3" />
                    {fmt(seller.followerCount)} theo dõi
                  </span>
                  {seller.city && (
                    <span className="text-xs text-slate-400">
                      📍 {seller.city}
                    </span>
                  )}
                </div>
              </div>

              {/* Actions */}
              <div className="flex items-center gap-2 pt-6 shrink-0">
                <Link
                  to={`/shop/${seller.id}`}
                  className="px-3 py-1.5 rounded-xl border border-slate-200 text-xs font-bold text-slate-600 hover:border-red-300 hover:text-red-600 transition-colors"
                >
                  Xem shop
                </Link>
                <button
                  onClick={() => handleUnfollow(seller.id)}
                  disabled={unfollowingId === seller.id}
                  className="px-3 py-1.5 rounded-xl border border-red-200 text-xs font-bold text-red-600 hover:bg-red-50 transition-colors disabled:opacity-50 flex items-center gap-1"
                >
                  {unfollowingId === seller.id ? (
                    <Loader2 className="w-3 h-3 animate-spin" />
                  ) : (
                    <HeartOff className="w-3 h-3" />
                  )}
                  Bỏ theo dõi
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
