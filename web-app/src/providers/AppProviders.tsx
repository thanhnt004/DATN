import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Provider, useDispatch, useSelector } from 'react-redux';
import { useEffect, useRef } from 'react';
import type { ReactNode } from 'react';
import { Bell } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { jwtDecode } from 'jwt-decode';
import { store } from '../store/store';
import type { AppDispatch, RootState } from '../store/store';
import { refreshSessionThunk } from '../store/authThunks';
import { getMyNotifications } from '../api/notificationApi';
import { incrementUnreadCount, setUnreadCount } from '../store/notificationSlice';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60 * 1000,
      refetchOnWindowFocus: false,
    },
  },
});

/** Dispatches refreshSessionThunk once on mount to restore session from httpOnly cookie */
function SessionRestorer({ children }: { children: ReactNode }) {
  const dispatch = useDispatch<AppDispatch>();
  const called = useRef(false);

  useEffect(() => {
    if (!called.current) {
      called.current = true;
      console.log('[SessionRestorer] dispatching refreshSessionThunk...');
      dispatch(refreshSessionThunk());
    }
  }, [dispatch]);

  return <>{children}</>;
}

function NotificationSocketManager() {
  const dispatch = useDispatch<AppDispatch>();
  const accessToken = useSelector((s: RootState) => s.auth.accessToken);
  const isAuthenticated = useSelector((s: RootState) => s.auth.isAuthenticated);
  const sessionRestored = useSelector((s: RootState) => s.auth.sessionRestored);
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimerRef = useRef<number | null>(null);

  useEffect(() => {
    if (!isAuthenticated || !accessToken) {
      dispatch(setUnreadCount(0));
      if (wsRef.current) {
        wsRef.current.onclose = null;
        wsRef.current.close();
        wsRef.current = null;
      }
      if (reconnectTimerRef.current !== null) {
        window.clearTimeout(reconnectTimerRef.current);
        reconnectTimerRef.current = null;
      }
      return;
    }

    const decoded = jwtDecode(accessToken) as { sub: string };
    const userId = decoded.sub;

    const configuredWsUrl = (
      import.meta.env.VITE_WS_URL as string | undefined ||
      import.meta.env.VITE_WEBSOCKET_URL as string | undefined
    )?.trim();
    const defaultOrigin = window.location.origin.replace(/^http/i, window.location.protocol === "https:" ? "wss" : "ws");
    const origin = configuredWsUrl && configuredWsUrl.length > 0
      ? configuredWsUrl
      : `${defaultOrigin.replace(/\/+$/, "")}/ws`;
    const wsBase = origin.endsWith("/ws") ? origin : `${origin.replace(/\/+$/, "")}/ws`;
    const wsUrl = `${wsBase}?token=${encodeURIComponent(accessToken)}`;

    const loadUnreadCount = async () => {
      try {
        const response = await getMyNotifications({ size: 50 });
        const list = response.data.result ?? [];
        const unread = list.filter((item) => item.status !== "READ").length;
        dispatch(setUnreadCount(unread));
      } catch (err) {
        console.error("[NotificationSocketManager] failed to load unread count", err);
      }
    };
    loadUnreadCount();

    const connect = () => {
      const ws = new WebSocket(wsUrl);
      wsRef.current = ws;

      ws.onopen = () => {
        console.log("[NotificationSocketManager] connected", wsUrl);
      };

      ws.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);
          if (message.to !== userId) return;

          if (message.feature === "NOTIFICATION") {
            const payload = message.payload ?? {};
            const notificationType = message.notificationType ?? payload.notificationType;
            const title = (payload.title as string | undefined) || (payload.subject as string | undefined) || (notificationType as string | undefined) || "Thông báo mới";
            const body = (payload.body as string | undefined) || (payload.message as string | undefined) || (payload.description as string | undefined) || "";
            const referenceId = payload.referenceId || payload.orderCode || payload.orderId || payload.notificationId;
            const extraLines: string[] = [];
            if (referenceId) extraLines.push(`Mã tham chiếu: ${referenceId}`);
            if (notificationType && notificationType !== title) extraLines.push(`Loại: ${notificationType}`);

            dispatch(incrementUnreadCount(1));
            window.dispatchEvent(new CustomEvent("notification:received", {
              detail: { payload, notificationType, referenceId },
            }));

            toast.custom((t) => (
              <div
                onClick={() => { window.location.href = "/user/notifications"; }}
                className={`${t.visible ? 'animate-enter' : 'animate-leave'} max-w-sm w-full bg-white shadow-lg rounded-2xl ring-1 ring-black ring-opacity-5 overflow-hidden cursor-pointer`}
              >
                <div className="flex items-start gap-3 p-4">
                  <div className="w-10 h-10 rounded-xl bg-red-100 text-red-700 flex items-center justify-center shrink-0">
                    <Bell className="w-5 h-5" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-bold text-gray-900">{title}</p>
                    {body && <p className="mt-1 text-sm text-gray-600 leading-5">{body}</p>}
                    {extraLines.length > 0 && (
                      <div className="mt-2 text-xs text-slate-500 space-y-0.5">
                        {extraLines.map((line) => (
                          <p key={line}>{line}</p>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ), {
              duration: 7000,
              position: "bottom-right",
            });
          }

          if (message.feature === "CHAT" && message.from && message.from !== userId) {
            const payload = message.payload ?? {};
            const content = (payload.content as string | undefined) || "Tin nhắn mới";
            toast(
              `${message.from}: ${content}`,
              {
                duration: 7000,
                position: "bottom-right",
              },
            );
          }
        } catch (error) {
          console.error("[NotificationSocketManager] failed to parse websocket message", error);
        }
      };

      ws.onclose = () => {
        console.log("[NotificationSocketManager] disconnected. reconnecting in 5s...");
        if (wsRef.current) {
          reconnectTimerRef.current = window.setTimeout(connect, 5000);
        }
      };

      ws.onerror = (error) => {
        console.error("[NotificationSocketManager] websocket error", error);
        ws.close();
      };
    };

    connect();

    return () => {
      if (reconnectTimerRef.current !== null) {
        window.clearTimeout(reconnectTimerRef.current);
      }
      if (wsRef.current) {
        wsRef.current.onclose = null;
        wsRef.current.close();
        wsRef.current = null;
      }
    };
  }, [isAuthenticated, accessToken, sessionRestored]);

  return null;
}

export default function AppProviders({ children }: { children: ReactNode }) {
  return (
    <Provider store={store}>
      <QueryClientProvider client={queryClient}>
        <SessionRestorer>
          <NotificationSocketManager />
          {children}
        </SessionRestorer>
      </QueryClientProvider>
    </Provider>
  );
}
