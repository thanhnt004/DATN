import { useEffect, useMemo, useRef, useState } from "react";
import { useSelector } from "react-redux";
import { useSearchParams } from "react-router-dom";
import { Send } from "lucide-react";
import toast from "react-hot-toast";
import { getMyProfile, getUserPublicProfile } from "../../api/userApi";
import { getMyOrders } from "../../api/orderApi";
import { getSellerOrders, getMySellerProfile } from "../../api/sellerDashboardApi";
import { getSellerPublic } from "../../api/sellerPublicApi";
import { getMyConversations, resolvePrivateConversation, getConversationMessages } from "../../api/chatApi";
import type { RootState } from "../../store/store";
import type { ChatMessageResponse, ConversationResponse, SendMessageRequest, WsMessage } from "../../types/chat";
import type { OrderResponse } from "../../types/order";

type Peer = { userId: string; label: string; avatarUrl?: string | null; isShop?: boolean };
let sharedSocket: WebSocket | null = null;
let sharedSocketUrl: string | null = null;
let sharedSocketConsumers = 0;

function getWsUrl(baseUrl: string, token: string): string {
  const configured = import.meta.env.VITE_WS_URL as string | undefined;
  const origin = (configured && configured.trim().length > 0
    ? configured
    : baseUrl.replace(/^http/i, "ws")) || "ws://localhost:8088";
  const wsBase = origin.endsWith("/ws") ? origin : `${origin.replace(/\/+$/, "")}/ws`;
  return `${wsBase}?token=${encodeURIComponent(token)}`;
}

function makeTraceId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

export default function ChatPage({ mode }: { mode: "buyer" | "seller" }) {
  const token = useSelector((s: RootState) => s.auth.accessToken);
  const [searchParams] = useSearchParams();
  const [me, setMe] = useState<{ id: string; avatarUrl?: string | null; label?: string; isShop?: boolean } | null>(null);
  const [peers, setPeers] = useState<Peer[]>([]);
  const [conversations, setConversations] = useState<ConversationResponse[]>([]);
  const [selectedPeerId, setSelectedPeerId] = useState<string>(searchParams.get("peerId") ?? "");
  const [draft, setDraft] = useState("");
  const [messagesByConversation, setMessagesByConversation] = useState<Record<string, ChatMessageResponse[]>>({});
  const [wsReady, setWsReady] = useState(false);
  const socketRef = useRef<WebSocket | null>(null);
  const reconnectTimerRef = useRef<number | null>(null);
  const shouldReconnectRef = useRef(false);
  const reconnectAttemptRef = useRef(0);
  const stableConnectionTimerRef = useRef<number | null>(null);

  const conversation = useMemo(() => {
    if (!selectedPeerId || !me) return null;
    return conversations.find((c) => c.members?.some((m) => m.userId === selectedPeerId) && c.members.some((m) => m.userId === me.id)) ?? null;
  }, [conversations, selectedPeerId, me]);

  useEffect(() => {
    const init = async () => {
      const profile = await getMyProfile();
      if (mode === "buyer") {
        setMe({ id: profile.id, avatarUrl: profile.avatarUrl, label: profile.fullName || profile.username, isShop: false });
      } else {
        try {
          const sellerProfile = await getMySellerProfile();
          setMe({ id: profile.id, avatarUrl: sellerProfile.logoUrl, label: sellerProfile.shopName, isShop: true });
        } catch {
          setMe({ id: profile.id, avatarUrl: profile.avatarUrl, label: profile.fullName || profile.username, isShop: false });
        }
      }

      // Fetch conversations first
      const convs = await getMyConversations(profile.id).catch(() => []);
      setConversations(convs);

      // Build peer list from conversations with better names
      const peersFromConv = new Map<string, Peer>();
      const peersToFetch = new Map<string, any>();

      for (const conv of convs) {
        const otherMember = conv.members?.find((m) => m.userId !== profile.id);
        if (!otherMember?.userId) continue;

        peersFromConv.set(otherMember.userId, {
          userId: otherMember.userId,
          label: `User ${otherMember.userId.slice(0, 8)}`,
        });
        if (!peersToFetch.has(otherMember.userId)) {
          peersToFetch.set(otherMember.userId, otherMember);
        }
      }

      // Fetch full profiles for peers from conversations
      const profilePromises = Array.from(peersToFetch.values()).map((peer: any) => {
        const userId = typeof peer === 'string' ? peer : peer.userId;
        const userRole = typeof peer === 'string' ? undefined : peer.userRole;
        const fetchAsSeller = userRole
          ? userRole.toUpperCase() === "SELLER"
          : mode === "buyer";

        return fetchAsSeller
          ? getSellerPublic(userId).then((res) => ({ userId, profile: res.data.result, isShop: true })).catch(() => ({ userId, profile: null, isShop: false }))
          : getUserPublicProfile(userId).then((res) => ({ userId, profile: res, isShop: false })).catch(() => ({ userId, profile: null, isShop: false }));
      });
      
      const fetchedProfiles = await Promise.all(profilePromises);
      fetchedProfiles.forEach(({ userId, profile: prof, isShop }) => {
        if (userId === profile.id) return;
        if (prof) {
          const label = isShop ? (prof as any).shopName : (prof as any).fullName || (prof as any).username;
          const avatarUrl = isShop ? (prof as any).logoUrl : (prof as any).avatarUrl;
          peersFromConv.set(userId, { userId, label: label || `User ${userId.slice(0, 8)}`, avatarUrl, isShop });
        }
      });

      if (mode === "buyer") {
        const orders = (await getMyOrders({ size: 100, page: 0 })).data.result.content ?? [];
        const sellerIds = Array.from(new Set(orders.map((o: OrderResponse) => o.sellerId)));
        const sellerProfiles = await Promise.all(
          sellerIds.map(async (id) => {
            try {
              const res = await getSellerPublic(id);
              return res.data.result;
            } catch {
              return null;
            }
          })
        );
        const peerList: Peer[] = sellerProfiles
          .filter((s): s is NonNullable<typeof s> => !!s)
          .map((s) => ({ userId: s.userId, label: s.shopName || `Seller ${s.userId.slice(0, 8)}`, avatarUrl: s.logoUrl, isShop: true }))
          .filter((p) => p.userId !== profile.id);
        
        // Merge: conversations first, then add new peers from orders
        peerList.forEach((p) => {
          peersFromConv.set(p.userId, p); // Always update with better label from orders
        });

        const initialPeerId = searchParams.get("peerId");
        if (initialPeerId && !Array.from(peersFromConv.keys()).includes(initialPeerId)) {
          const bySellerId = sellerProfiles.find((s) => s?.id === initialPeerId);
          if (bySellerId?.userId) {
            setSelectedPeerId(bySellerId.userId);
          }
        }
      } else {
        const sellerOrders = (await getSellerOrders({ size: 100, page: 0 }).catch(() => ({ content: [] }))).content ?? [];
        const buyerIds = Array.from(new Set(sellerOrders.map((o: OrderResponse) => o.userId)));
        const buyerProfiles = await Promise.all(buyerIds.map((id) => getUserPublicProfile(id).catch(() => null)));
        const peerList: Peer[] = buyerProfiles
          .filter((u): u is NonNullable<typeof u> => !!u)
          .map((u) => ({ userId: u.id, label: u.fullName || u.username || `Buyer ${u.id.slice(0, 8)}`, avatarUrl: u.avatarUrl, isShop: false }))
          .filter((p) => p.userId !== profile.id);
        
        // Merge: conversations first, then add new peers from orders
        peerList.forEach((p) => {
          peersFromConv.set(p.userId, p); // Always update with better label from orders
        });
      }

      setPeers(Array.from(peersFromConv.values()));
    };
    init();
  }, [mode, searchParams]);

  // Resolve or create private conversation when peer is selected
  useEffect(() => {
    if (!selectedPeerId || !me?.id) return;

    const resolveConversation = async () => {
      try {
        const uRole = mode === "buyer" ? "SELLER" : "BUYER";
        const cUserRole = mode === "buyer" ? "BUYER" : "SELLER";
        const resolved = await resolvePrivateConversation(selectedPeerId, me.id, uRole, cUserRole);
        // Add or update the conversation in the state
        setConversations((prev) => {
          const exists = prev.some((c) => c.id === resolved.id);
          if (exists) {
            return prev.map((c) => (c.id === resolved.id ? resolved : c));
          }
          return [...prev, resolved];
        });
      } catch (error) {
        console.error("Failed to resolve conversation", error);
        toast.error("Không thể tải hội thoại, vui lòng thử lại");
      }
    };

    resolveConversation();
  }, [selectedPeerId, me?.id]);

  // Load message history when conversation is selected
  useEffect(() => {
    if (!conversation || !me?.id) return;

    const loadHistory = async () => {
      try {
        const messages = await getConversationMessages(conversation.id, me.id);
        if (messages && messages.length > 0) {
          setMessagesByConversation((prev) => ({
            ...prev,
            [conversation.id]: messages,
          }));
        }
      } catch (error) {
        console.error("Failed to load message history", error);
      }
    };

    loadHistory();
  }, [conversation?.id, me?.id]);

  useEffect(() => {
    if (!token) {
      shouldReconnectRef.current = false;
      reconnectAttemptRef.current = 0;
      if (stableConnectionTimerRef.current !== null) {
        window.clearTimeout(stableConnectionTimerRef.current);
        stableConnectionTimerRef.current = null;
      }
      if (reconnectTimerRef.current !== null) {
        window.clearTimeout(reconnectTimerRef.current);
        reconnectTimerRef.current = null;
      }
      if (socketRef.current) {
        socketRef.current.close();
        socketRef.current = null;
      }
      setTimeout(() => setWsReady(false), 0);
      return;
    }
    shouldReconnectRef.current = true;
    reconnectAttemptRef.current = 0;
    const wsUrl = getWsUrl(import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8088", token);
    let manuallyClosed = false;
    const maxReconnectAttempts = 5;
    const stableConnectionMs = 5000;
    const shouldRetry = (code: number) => {
      // 1000/1001 are normal closes, 1008 often indicates policy/auth rejection.
      if (code === 1000 || code === 1001 || code === 1008) return false;
      return true;
    };

    const connect = () => {
      if (!shouldReconnectRef.current) return;

      const canReuse =
        sharedSocket !== null &&
        sharedSocketUrl === wsUrl &&
        (sharedSocket.readyState === WebSocket.OPEN || sharedSocket.readyState === WebSocket.CONNECTING);

      const socket: WebSocket = canReuse ? sharedSocket! : new WebSocket(wsUrl);
      sharedSocket = socket;
      sharedSocketUrl = wsUrl;
      socketRef.current = socket;
      setWsReady(false);

      socket.onopen = () => {
        if (socketRef.current !== socket) return;
        setWsReady(true);
        console.info("[chat] websocket connected");
        if (stableConnectionTimerRef.current !== null) {
          window.clearTimeout(stableConnectionTimerRef.current);
        }
        stableConnectionTimerRef.current = window.setTimeout(() => {
          reconnectAttemptRef.current = 0;
          stableConnectionTimerRef.current = null;
        }, stableConnectionMs);
      };

      socket.onclose = (event) => {
        if (socketRef.current !== socket) return;
        setWsReady(false);
        if (stableConnectionTimerRef.current !== null) {
          window.clearTimeout(stableConnectionTimerRef.current);
          stableConnectionTimerRef.current = null;
        }
        if (manuallyClosed || !shouldReconnectRef.current) return;
        if (!shouldRetry(event.code)) {
          console.warn(`[chat] websocket closed by server (code=${event.code}, reason="${event.reason}", clean=${event.wasClean})`);
          return;
        }
        if (reconnectAttemptRef.current >= maxReconnectAttempts) {
          console.error(`[chat] websocket reconnect limit reached (last code=${event.code}, reason="${event.reason}")`);
          toast.error("Ket noi chat bi gian doan. Vui long tai lai trang.");
          return;
        }
        reconnectAttemptRef.current += 1;
        const delayMs = Math.min(1000 * 2 ** (reconnectAttemptRef.current - 1), 8000);
        console.warn(
          `[chat] websocket closed (code=${event.code}, reason="${event.reason}", clean=${event.wasClean}), retrying (${reconnectAttemptRef.current}/${maxReconnectAttempts})...`
        );
        reconnectTimerRef.current = window.setTimeout(() => {
          reconnectTimerRef.current = null;
          connect();
        }, delayMs);
      };

      socket.onerror = () => {
        if (socketRef.current !== socket) return;
        setWsReady(false);
        if (!manuallyClosed && shouldReconnectRef.current) {
          toast.error("Khong the ket noi websocket chat");
        }
      };

      socket.onmessage = (evt) => {
        if (socketRef.current !== socket) return;
        try {
          const incoming = JSON.parse(evt.data) as WsMessage<ChatMessageResponse>;
          const payload = incoming.payload;
          const key = payload.conversationId || incoming.to || "unknown";
          setMessagesByConversation((prev) => ({
            ...prev,
            [key]: [...(prev[key] ?? []), payload],
          }));
        } catch {
          // ignore invalid event
        }
      };
    };

    connect();
    sharedSocketConsumers += 1;

    return () => {
      shouldReconnectRef.current = false;
      reconnectAttemptRef.current = 0;
      manuallyClosed = true;
      sharedSocketConsumers = Math.max(0, sharedSocketConsumers - 1);
      if (stableConnectionTimerRef.current !== null) {
        window.clearTimeout(stableConnectionTimerRef.current);
        stableConnectionTimerRef.current = null;
      }
      if (reconnectTimerRef.current !== null) {
        window.clearTimeout(reconnectTimerRef.current);
        reconnectTimerRef.current = null;
      }
      if (socketRef.current) {
        if (sharedSocketConsumers === 0) {
          socketRef.current.close();
          sharedSocket = null;
          sharedSocketUrl = null;
        }
        socketRef.current = null;
      }
    };
  }, [token]);

  const handleSend = () => {
    if (!socketRef.current || socketRef.current.readyState !== WebSocket.OPEN || !wsReady) {
      toast.error("Kenh chat chua san sang, vui long thu lai");
      return;
    }
    if (!me) {
      toast.error("Dang tai thong tin tai khoan, vui long thu lai");
      return;
    }
    if (!selectedPeerId) {
      toast.error("Vui long chon nguoi nhan");
      return;
    }
    if (!draft.trim()) return;

    const requestPayload: SendMessageRequest = {
      content: draft.trim(),
      type: "TEXT",
      attachments: [],
      memberIds: [me.id, selectedPeerId],
      conversationType: "PRIVATE",
    };

    const outgoing: WsMessage<SendMessageRequest> = {
      traceId: makeTraceId(),
      feature: "CHAT",
      action: "SEND",
      from: me.id,
      // to is conversationId if exists, null if first message (conversation will be created by backend)
      to: conversation?.id ?? null,
      timestamp: Date.now(),
      payload: requestPayload,
    };
    socketRef.current.send(JSON.stringify(outgoing));
    console.info("[chat] sent message", { to: outgoing.to, from: outgoing.from, traceId: outgoing.traceId });
    setDraft("");
  };

  const selectedPeer = peers.find((p) => p.userId === selectedPeerId) ?? null;
  // Use conversationId if exists, otherwise use selectedPeerId (for new conversations before they're created)
  const messageKey = conversation?.id ?? selectedPeerId;
  const currentMessages = selectedPeerId ? (messagesByConversation[messageKey] ?? []).sort((a, b) => {
    const timeA = typeof a.createdAt === "number" ? a.createdAt : new Date(a.createdAt).getTime();
    const timeB = typeof b.createdAt === "number" ? b.createdAt : new Date(b.createdAt).getTime();
    return timeA - timeB;
  }) : [];

  const formatTime = (timestamp: number | string) => {
    const date = typeof timestamp === "number" ? new Date(timestamp) : new Date(timestamp);
    const hours = date.getHours().toString().padStart(2, "0");
    const minutes = date.getMinutes().toString().padStart(2, "0");
    return `${hours}:${minutes}`;
  };

  return (
    <div className="h-[calc(100vh-180px)] bg-white border border-gray-200 rounded-2xl overflow-hidden grid grid-cols-12">
      <div className="col-span-4 border-r border-gray-100 p-3 overflow-y-auto">
        <h2 className="font-bold text-gray-800 mb-3">Danh sách chat</h2>
        <div className="space-y-1">
          {peers.map((p) => (
            <button
              key={p.userId}
              onClick={() => setSelectedPeerId(p.userId)}
              className={`w-full text-left px-3 py-2 rounded-xl text-sm flex items-center gap-3 ${selectedPeerId === p.userId ? "bg-red-50 text-red-600" : "hover:bg-gray-50 text-gray-700"}`}
            >
              {p.avatarUrl ? (
                <img src={p.avatarUrl} alt={p.label} className="w-8 h-8 rounded-full object-cover border border-gray-200 shrink-0 bg-white" />
              ) : (
                <div className="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center text-gray-500 font-bold shrink-0">
                  {p.label.charAt(0).toUpperCase()}
                </div>
              )}
              <div className="flex flex-col overflow-hidden">
                <span className="truncate">{p.label}</span>
                {p.isShop && <span className="text-xs text-gray-400 font-normal">Shop</span>}
              </div>
            </button>
          ))}
        </div>
      </div>

      <div className="col-span-8 flex flex-col">
        <div className="px-4 py-3 border-b border-gray-100 flex items-center gap-3">
          {selectedPeer ? (
            <>
              {selectedPeer.avatarUrl ? (
                <img src={selectedPeer.avatarUrl} alt={selectedPeer.label} className="w-10 h-10 rounded-full object-cover border border-gray-200 bg-white" />
              ) : (
                <div className="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center text-gray-500 font-bold text-lg">
                  {selectedPeer.label.charAt(0).toUpperCase()}
                </div>
              )}
              <div className="flex flex-col">
                <span className="font-semibold text-gray-800">{selectedPeer.label}</span>
                {selectedPeer.isShop && <span className="text-xs text-gray-500 font-normal">Shop</span>}
              </div>
            </>
          ) : (
            <span className="font-semibold text-gray-800">Chọn người để bắt đầu chat</span>
          )}
        </div>

        <div className="flex-1 p-4 overflow-y-auto space-y-2 bg-gray-50">
          {currentMessages.length === 0 ? (
            <p className="text-sm text-gray-500">Chưa có tin nhắn.</p>
          ) : (
            currentMessages.map((m, i) => {
              const mine = m.senderId === me?.id;
              const avatar = mine ? me?.avatarUrl : selectedPeer?.avatarUrl;
              const label = mine ? me?.label : selectedPeer?.label;
              return (
                <div key={`${m.messageId ?? "msg"}-${i}`} className={`flex gap-2 ${mine ? "justify-end" : "justify-start"}`}>
                  {!mine && (
                    avatar ? (
                      <img src={avatar} alt={label || "User"} className="w-8 h-8 rounded-full object-cover border border-gray-200 mt-1 shrink-0 bg-white" />
                    ) : (
                      <div className="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center text-gray-500 font-bold mt-1 shrink-0 text-xs">
                        {(label || "U").charAt(0).toUpperCase()}
                      </div>
                    )
                  )}
                  <div className={`flex flex-col gap-1 max-w-[70%] ${mine ? "items-end" : "items-start"}`}>
                    <div className={`px-3 py-2 rounded-2xl text-sm ${mine ? "bg-red-500 text-white rounded-tr-sm" : "bg-white text-gray-800 border border-gray-200 rounded-tl-sm"}`}>
                      {m.content}
                    </div>
                    <span className="text-xs text-gray-500 px-1">
                      {formatTime(m.createdAt)}
                    </span>
                  </div>
                </div>
              );
            })
          )}
        </div>

        <div className="p-3 border-t border-gray-100 flex items-center gap-2">
          <input
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSend()}
            placeholder="Nhập tin nhắn..."
            className="flex-1 border border-gray-200 rounded-xl px-3 py-2 text-sm outline-none focus:border-red-300"
          />
          <button onClick={handleSend} className="px-3 py-2 rounded-xl bg-red-500 text-white hover:bg-red-600">
            <Send className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}