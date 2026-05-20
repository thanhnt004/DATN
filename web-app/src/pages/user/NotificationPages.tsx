import { useEffect, useState } from "react";
import { useDispatch } from "react-redux";
import { useNavigate } from "react-router-dom";
import { Bell, Settings, CheckCheck, Loader2, Package, Ticket, ShieldCheck, Megaphone } from "lucide-react";
import { getMyNotifications, markAsRead, markAllAsRead, getMyNotificationSetting, putNotificationSetting } from "../../api/notificationApi";
import type { ChannelType, NotificationResponse } from "../../types/notification";
import { setUnreadCount, decrementUnreadCount, clearUnreadCount } from "../../store/notificationSlice";
import type { AppDispatch } from "../../store/store";

function fmtDate(s: string) {
  return new Date(s).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });
}

const NOTIFICATION_SETTING_DEFINITIONS = [
  // Giao dịch
  { group: "Đơn hàng & Thanh toán", type: "ORDER_CREATED", label: "Đơn hàng mới", desc: "Khi có người đặt hàng mới", defaultEnabled: true },
  { group: "Đơn hàng & Thanh toán", type: "ORDER_CONFIRMED", label: "Xác nhận đơn", desc: "Khi đơn hàng được xác nhận", defaultEnabled: true },
  { group: "Đơn hàng & Thanh toán", type: "ORDER_SHIPPED", label: "Đang giao hàng", desc: "Khi đơn hàng bắt đầu giao", defaultEnabled: true },
  { group: "Đơn hàng & Thanh toán", type: "ORDER_DELIVERED", label: "Đã giao hàng", desc: "Khi đơn hàng giao thành công", defaultEnabled: true },
  { group: "Đơn hàng & Thanh toán", type: "ORDER_CANCELLED", label: "Hủy đơn", desc: "Khi đơn hàng bị hủy", defaultEnabled: true },
  { group: "Đơn hàng & Thanh toán", type: "PAYMENT_UPDATES", label: "Cập nhật thanh toán", desc: "Thông báo về trạng thái thanh toán đơn hàng", defaultEnabled: true },

  // Bảo mật & Tài khoản
  { group: "Bảo mật & Tài khoản", type: "PASSWORD_RESET", label: "Bảo mật tài khoản", desc: "Cảnh báo khi có hoạt động đăng nhập hoặc đổi mật khẩu", defaultEnabled: true },

  // Marketing
  { group: "Khuyến mãi & Cập nhật", type: "PROMOTIONS", label: "Chương trình khuyến mãi", desc: "Nhận thông tin về các đợt giảm giá và ưu đãi", defaultEnabled: false },
  { group: "Khuyến mãi & Cập nhật", type: "NEWS_AND_UPDATES", label: "Tin tức từ Sellico", desc: "Cập nhật về tính năng mới và tin tức từ hệ thống", defaultEnabled: false },
];

export const TYPE_ICON: Record<string, { icon: typeof Bell; cls: string }> = {
  ORDER_CREATED:   { icon: Package,     cls: "bg-blue-100 text-blue-600" },
  ORDER_CONFIRMED: { icon: Package,     cls: "bg-blue-100 text-blue-600" },
  ORDER_SHIPPED:   { icon: Package,     cls: "bg-blue-100 text-blue-600" },
  ORDER_DELIVERED: { icon: Package,     cls: "bg-blue-100 text-blue-600" },
  ORDER_CANCELLED: { icon: Package,     cls: "bg-blue-100 text-blue-600" },
  PAYMENT_UPDATES: { icon: Ticket,      cls: "bg-green-100 text-green-600" },
  PASSWORD_RESET:  { icon: ShieldCheck, cls: "bg-red-100 text-red-600" },
  PROMOTIONS:      { icon: Megaphone,   cls: "bg-purple-100 text-purple-600" },
  NEWS_AND_UPDATES:{ icon: Megaphone,   cls: "bg-purple-100 text-purple-600" },
};

const NOTIFICATION_TYPE_LABELS = NOTIFICATION_SETTING_DEFINITIONS.reduce((acc, item) => {
  acc[item.type] = item.label;
  return acc;
}, {} as Record<string, string>);

const CHANNELS: Array<{ key: ChannelType; label: string }> = [
  { key: "EMAIL", label: "Email" },
  { key: "PUSH", label: "Thông báo đẩy" },
  { key: "SMS", label: "SMS" },
];

export function NotificationsPage() {
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [markingAll, setMarkingAll] = useState(false);

  useEffect(() => {
    getMyNotifications({ size: 50 })
      .then(r => {
        const list = r.data.result ?? [];
        setNotifications(list);
        const unread = list.filter((item) => item.status !== "READ").length;
        dispatch(setUnreadCount(unread));
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [dispatch]);

  const handleMarkRead = async (id: string) => {
    try {
      await markAsRead(id);
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, status: "READ" } : n));
      dispatch(decrementUnreadCount(1));
    } catch { /* */ }
  };

  const handleMarkAll = async () => {
    setMarkingAll(true);
    try {
      await markAllAsRead();
      setNotifications(prev => prev.map(n => ({ ...n, status: "READ" })));
      dispatch(clearUnreadCount());
    } catch { /* */ }
    setMarkingAll(false);
  };

  const handleClickNotification = (notification: NotificationResponse) => {
    // Mark as read if unread
    if (notification.status !== "READ") {
      handleMarkRead(notification.id);
    }
    // Navigate if it's an order notification with a referenceId
    if (notification.notificationType?.startsWith("ORDER_") && notification.payload?.referenceId) {
      navigate(`/user/orders/${notification.payload.referenceId}`);
    }
  };

  const unreadCount = notifications.filter(n => n.status !== "READ").length;

  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm">
      <div className="flex items-center justify-between px-6 py-5 border-b border-gray-50">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 bg-red-100 rounded-xl flex items-center justify-center">
            <Bell className="w-4.5 h-4.5 text-red-600" />
          </div>
          <div>
            <h1 className="font-black text-gray-900 text-lg">Thông báo</h1>
            {unreadCount > 0 && <p className="text-xs text-slate-400">{unreadCount} chưa đọc</p>}
          </div>
        </div>
        {unreadCount > 0 && (
          <button onClick={handleMarkAll} disabled={markingAll}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold text-red-600 hover:bg-red-50 disabled:opacity-50">
            {markingAll ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <CheckCheck className="w-3.5 h-3.5" />}
            Đánh dấu tất cả đã đọc
          </button>
        )}
      </div>

      {loading ? (
        <div className="flex justify-center py-16"><Loader2 className="w-8 h-8 animate-spin text-slate-300" /></div>
      ) : notifications.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 gap-4 text-center px-6">
          <div className="w-16 h-16 bg-gray-100 rounded-2xl flex items-center justify-center">
            <Bell className="w-8 h-8 text-gray-300" />
          </div>
          <p className="font-bold text-gray-600">Bạn chưa có thông báo nào</p>
          <p className="text-sm text-gray-400 max-w-xs">Các thông báo về đơn hàng, khuyến mãi sẽ xuất hiện tại đây.</p>
        </div>
      ) : (
        <div className="divide-y divide-gray-50">
          {notifications.map(n => {
            const isUnread = n.status !== "READ";
            const typeCfg = TYPE_ICON[n.notificationType] ?? { icon: Bell, cls: "bg-gray-100 text-gray-600" };
            const Icon = typeCfg.icon;
            const title = (n.payload?.title as string) || NOTIFICATION_TYPE_LABELS[n.notificationType] || n.notificationType;
            const message = (n.payload?.message as string) || (n.payload?.body as string) || "";
            
            // Extract additional details based on notification type
            const isOrderNotification = n.notificationType?.startsWith("ORDER_");
            const orderCode = isOrderNotification ? (n.payload?.orderCode as string) : null;
            const customerName = isOrderNotification ? (n.payload?.customerName as string) : null;
            const totalAmount = isOrderNotification ? (n.payload?.totalAmount as number) : null;
            const itemsArray = isOrderNotification ? (n.payload?.items as any[]) : null;
            const itemCount = itemsArray?.length ?? 0;

            return (
              <div key={n.id}
                onClick={() => handleClickNotification(n)}
                className={`px-6 py-4 cursor-pointer hover:bg-slate-50 transition-colors flex gap-3 ${isUnread ? "bg-blue-50/30" : ""}`}>
                <div className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 ${typeCfg.cls}`}>
                  <Icon className="w-4 h-4" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-0.5">
                    <span className={`text-sm font-bold ${isUnread ? "text-slate-800" : "text-slate-600"}`}>{title}</span>
                    {isUnread && <span className="w-2 h-2 rounded-full bg-blue-500 shrink-0" />}
                  </div>
                  
                  {/* Order-specific details */}
                  {isOrderNotification && orderCode && (
                    <div className="text-xs text-slate-600 mb-1.5 space-y-0.5">
                      <p className="font-semibold">
                        {orderCode} {customerName && `- ${customerName}`}
                      </p>
                      <div className="flex gap-3 text-slate-500">
                        {itemCount > 0 && <span>{itemCount} sản phẩm</span>}
                        {totalAmount !== null && <span className="font-semibold text-slate-700">{(totalAmount / 1000).toFixed(0)}K đ</span>}
                      </div>
                    </div>
                  )}
                  
                  {/* Generic message */}
                  {message && <p className="text-xs text-slate-500 line-clamp-1">{message}</p>}
                  
                  <p className="text-[10px] text-slate-400 mt-1.5">{fmtDate(n.createdAt)}</p>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export function NotificationSettingsPage() {
  const groupedSettings = NOTIFICATION_SETTING_DEFINITIONS.reduce((acc, item) => {
    const group = acc[item.group] || [];
    group.push(item);
    acc[item.group] = group;
    return acc;
  }, {} as Record<string, typeof NOTIFICATION_SETTING_DEFINITIONS>);

  const [settings, setSettings] = useState<Record<string, Record<string, boolean>>>({});
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState<string | null>(null);

  const buildNotificationSettings = (userPreferences: Record<string, Record<string, boolean>>) => {
    const finalSettings: Record<string, Record<string, boolean>> = {};
    NOTIFICATION_SETTING_DEFINITIONS.forEach(def => {
      finalSettings[def.type] = {};
      CHANNELS.forEach(channel => {
        const userValue = userPreferences[def.type]?.[channel.key];
        if (userValue !== undefined) {
          finalSettings[def.type][channel.key] = userValue;
        } else if (channel.key === 'SMS') {
          finalSettings[def.type][channel.key] = false;
        } else {
          finalSettings[def.type][channel.key] = def.defaultEnabled;
        }
      });
    });
    return finalSettings;
  };

  useEffect(() => {
    getMyNotificationSetting()
      .then(res => {
        const fetchedSettings = res.data.result || [];

        const userPreferences: Record<string, Record<string, boolean>> = {};
        fetchedSettings.forEach(s => {
          if (!userPreferences[s.notificationType]) {
            userPreferences[s.notificationType] = {};
          }
          userPreferences[s.notificationType][s.channelType] = s.enabled;
        });

        setSettings(buildNotificationSettings(userPreferences));
      })
      .catch(() => {
        setSettings(buildNotificationSettings({}));
      })
      .finally(() => setLoading(false));
  }, []);

  const handleToggle = async (type: string, channel: ChannelType, currentEnabled: boolean) => {
    const updateKey = `${type}:${channel}`;
    if (updating === updateKey) return;
    setUpdating(updateKey);
    const newEnabled = !currentEnabled;
    try {
      await putNotificationSetting({
        notificationType: type,
        channelType: channel,
        enabled: newEnabled,
      });
      setSettings(prev => {
        const newTypeSettings = { ...prev[type], [channel]: newEnabled };
        return { ...prev, [type]: newTypeSettings };
      });
    } catch {
      // Revert on error or show toast
    } finally {
      setUpdating(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3 px-6 py-5 border-b border-gray-50 bg-white rounded-t-2xl shadow-sm">
        <div className="w-9 h-9 bg-red-100 rounded-xl flex items-center justify-center">
          <Settings className="w-4.5 h-4.5 text-red-600" />
        </div>
        <h1 className="font-black text-gray-900 text-lg">Cài đặt thông báo</h1>
      </div>
      {loading ? (
        <div className="flex justify-center py-16"><Loader2 className="w-8 h-8 animate-spin text-slate-300" /></div>
      ) : (
        Object.entries(groupedSettings).map(([groupName, items]) => (
          <div key={groupName}>
            <h3 className="text-sm font-black text-slate-500 uppercase tracking-widest mb-3 px-2">{groupName}</h3>
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-x-auto">
              <table className="w-full text-sm min-w-[600px]">
                <thead>
                  <tr className="bg-gray-50/70">
                    <th className="px-4 py-3 text-left text-xs font-bold text-gray-500 uppercase tracking-widest w-2/5">Loại thông báo</th>
                    {CHANNELS.map(ch => <th key={ch.key} className="px-4 py-3 text-center text-xs font-bold text-gray-500 uppercase tracking-widest">{ch.label}</th>)}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {items.map(({ type, label, desc }) => (
                    <tr key={type}>
                      <td className="px-4 py-3 font-semibold text-gray-800">
                        <p className="font-bold">{label}</p>
                        <p className="text-xs text-gray-500 mt-0.5 font-normal">{desc}</p>
                      </td>
                      {CHANNELS.map(ch => {
                        const enabled = settings[type]?.[ch.key] ?? false;
                        const isUpdating = updating === `${type}:${ch.key}`;
                        return (
                          <td key={ch.key} className="px-4 py-3 text-center">
                            <div onClick={() => handleToggle(type, ch.key, enabled)}
                              className={`inline-flex items-center justify-center w-10 h-6 rounded-full relative transition-colors cursor-pointer ${isUpdating ? "opacity-60" : ""} ${enabled ? "bg-red-500" : "bg-gray-200"}`}>
                              {isUpdating ? (
                                <Loader2 className="w-3 h-3 animate-spin text-white" />
                              ) : (
                                <span className={`absolute top-1 w-4 h-4 rounded-full bg-white shadow transition-transform ${enabled ? "translate-x-2" : "-translate-x-2"}`} />
                              )}
                            </div>
                          </td>
                        );
                      })}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ))
      )}
    </div>
  );
}
