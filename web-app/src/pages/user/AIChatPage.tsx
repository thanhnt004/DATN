import { useState, useRef, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";
import { Send, Bot, User, Trash2, Sparkles, ShoppingBag, Star, Loader2 } from "lucide-react";
import { sendChatMessage, deleteChatSession } from "../../api/chatbotApi";
import type { ChatResponse, ProductInfo } from "../../api/chatbotApi";
import toast from "react-hot-toast";

// ── Types ────────────────────────────────────────────────────────────────────
interface Message {
  id: string;
  role: "user" | "assistant";
  content: string;
  products?: ProductInfo[];
  timestamp: Date;
}

// ── Helpers ──────────────────────────────────────────────────────────────────
const fmtPrice = (n: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(n);

let msgId = 0;
const nextId = () => `msg-${++msgId}`;

// ── Component ────────────────────────────────────────────────────────────────
export default function AIChatPage() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [sessionId, setSessionId] = useState<string | undefined>();
  const bottomRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  // Auto-scroll to bottom
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // Auto-resize textarea
  useEffect(() => {
    if (inputRef.current) {
      inputRef.current.style.height = "auto";
      inputRef.current.style.height = `${Math.min(inputRef.current.scrollHeight, 120)}px`;
    }
  }, [input]);

  const handleSend = useCallback(async () => {
    const text = input.trim();
    if (!text || loading) return;

    const userMsg: Message = {
      id: nextId(),
      role: "user",
      content: text,
      timestamp: new Date(),
    };
    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    setLoading(true);

    try {
      const res: ChatResponse = await sendChatMessage({
        message: text,
        sessionId,
      });
      setSessionId(res.sessionId);

      const assistantMsg: Message = {
        id: nextId(),
        role: "assistant",
        content: res.reply,
        products: res.suggestedProducts ?? undefined,
        timestamp: new Date(),
      };
      setMessages((prev) => [...prev, assistantMsg]);
    } catch {
      toast.error("Không thể gửi tin nhắn. Vui lòng thử lại.");
    } finally {
      setLoading(false);
      inputRef.current?.focus();
    }
  }, [input, loading, sessionId]);

  const handleNewChat = async () => {
    if (sessionId) {
      try {
        await deleteChatSession(sessionId);
      } catch { /* ignore */ }
    }
    setMessages([]);
    setSessionId(undefined);
    inputRef.current?.focus();
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 flex flex-col h-[calc(100vh-120px)] max-h-[800px]">
      {/* ── Header ── */}
      <div className="flex items-center justify-between px-5 py-3.5 border-b border-gray-100">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-red-500 to-pink-500 flex items-center justify-center">
            <Sparkles className="w-4.5 h-4.5 text-white" />
          </div>
          <div>
            <h2 className="text-sm font-bold text-gray-900">AI Tư vấn sản phẩm</h2>
            <p className="text-[11px] text-gray-400">Hỗ trợ bởi Gemini AI</p>
          </div>
        </div>
        {messages.length > 0 && (
          <button
            onClick={handleNewChat}
            className="flex items-center gap-1.5 text-xs text-gray-400 hover:text-red-500 transition-colors px-2.5 py-1.5 rounded-lg hover:bg-red-50"
          >
            <Trash2 className="w-3.5 h-3.5" />
            Cuộc trò chuyện mới
          </button>
        )}
      </div>

      {/* ── Messages ── */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-4">
        {messages.length === 0 && (
          <div className="flex flex-col items-center justify-center h-full text-center gap-4 py-10">
            <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-red-100 to-pink-100 flex items-center justify-center">
              <Bot className="w-8 h-8 text-red-500" />
            </div>
            <div>
              <h3 className="text-base font-bold text-gray-800 mb-1">Xin chào! 👋</h3>
              <p className="text-sm text-gray-500 max-w-sm">
                Tôi là trợ lý AI của Sellico. Hãy hỏi tôi về sản phẩm bạn đang tìm kiếm,
                tôi sẽ tư vấn và gợi ý cho bạn!
              </p>
            </div>
            <div className="flex flex-wrap gap-2 justify-center mt-2">
              {[
                "Tư vấn laptop cho sinh viên",
                "Điện thoại dưới 10 triệu",
                "Tai nghe chống ồn tốt nhất",
              ].map((q) => (
                <button
                  key={q}
                  onClick={() => { setInput(q); inputRef.current?.focus(); }}
                  className="text-xs px-3 py-2 rounded-xl border border-gray-200 text-gray-600 hover:border-red-300 hover:text-red-600 hover:bg-red-50 transition-all"
                >
                  {q}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((msg) => (
          <div key={msg.id} className={`flex gap-2.5 ${msg.role === "user" ? "justify-end" : ""}`}>
            {msg.role === "assistant" && (
              <div className="w-7 h-7 rounded-lg bg-gradient-to-br from-red-500 to-pink-500 flex items-center justify-center shrink-0 mt-0.5">
                <Bot className="w-3.5 h-3.5 text-white" />
              </div>
            )}
            <div className={`max-w-[80%] ${msg.role === "user" ? "order-first" : ""}`}>
              <div
                className={`rounded-2xl px-4 py-2.5 text-sm leading-relaxed whitespace-pre-wrap ${
                  msg.role === "user"
                    ? "bg-red-500 text-white rounded-br-md"
                    : "bg-gray-100 text-gray-800 rounded-bl-md"
                }`}
              >
                {msg.content}
              </div>

              {/* Product cards */}
              {msg.products && msg.products.length > 0 && (
                <div className="mt-3 space-y-2">
                  <p className="text-xs font-semibold text-gray-500 flex items-center gap-1">
                    <ShoppingBag className="w-3 h-3" />
                    Sản phẩm gợi ý
                  </p>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    {msg.products.map((p) => (
                      <ProductCard key={p.id} product={p} />
                    ))}
                  </div>
                </div>
              )}
            </div>
            {msg.role === "user" && (
              <div className="w-7 h-7 rounded-lg bg-gray-200 flex items-center justify-center shrink-0 mt-0.5">
                <User className="w-3.5 h-3.5 text-gray-600" />
              </div>
            )}
          </div>
        ))}

        {loading && (
          <div className="flex gap-2.5">
            <div className="w-7 h-7 rounded-lg bg-gradient-to-br from-red-500 to-pink-500 flex items-center justify-center shrink-0">
              <Bot className="w-3.5 h-3.5 text-white" />
            </div>
            <div className="bg-gray-100 rounded-2xl rounded-bl-md px-4 py-3">
              <div className="flex items-center gap-2 text-sm text-gray-500">
                <Loader2 className="w-4 h-4 animate-spin" />
                Đang suy nghĩ...
              </div>
            </div>
          </div>
        )}

        <div ref={bottomRef} />
      </div>

      {/* ── Input ── */}
      <div className="border-t border-gray-100 px-4 py-3">
        <div className="flex items-end gap-2">
          <textarea
            ref={inputRef}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Nhập câu hỏi về sản phẩm..."
            rows={1}
            className="flex-1 resize-none rounded-xl border border-gray-200 px-4 py-2.5 text-sm focus:outline-none focus:border-red-400 bg-gray-50 placeholder:text-gray-400"
          />
          <button
            onClick={handleSend}
            disabled={!input.trim() || loading}
            className="shrink-0 w-10 h-10 rounded-xl bg-red-500 text-white flex items-center justify-center hover:bg-red-600 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
          >
            <Send className="w-4 h-4" />
          </button>
        </div>
        <p className="text-[10px] text-gray-400 mt-1.5 text-center">
          AI có thể đưa ra thông tin không chính xác. Hãy kiểm tra lại thông tin sản phẩm.
        </p>
      </div>
    </div>
  );
}

// ── Product Card ─────────────────────────────────────────────────────────────
function ProductCard({ product }: { product: ProductInfo }) {
  return (
    <Link
      to={`/products/${product.slug}`}
      className="flex gap-3 p-2.5 rounded-xl border border-gray-200 hover:border-red-300 hover:shadow-sm transition-all bg-white group"
    >
      <div className="w-16 h-16 rounded-lg bg-gray-100 overflow-hidden shrink-0">
        {product.imageUrl ? (
          <img src={product.imageUrl} alt={product.name} className="w-full h-full object-cover" />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-gray-300">
            <ShoppingBag className="w-6 h-6" />
          </div>
        )}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-xs font-semibold text-gray-800 line-clamp-2 group-hover:text-red-600 transition-colors">
          {product.name}
        </p>
        <p className="text-sm font-bold text-red-600 mt-1">{fmtPrice(product.price)}</p>
        {product.originalPrice && product.originalPrice > product.price && (
          <p className="text-[10px] text-gray-400 line-through">{fmtPrice(product.originalPrice)}</p>
        )}
        {product.ratingAvg != null && (
          <div className="flex items-center gap-1 mt-0.5">
            <Star className="w-3 h-3 text-yellow-400 fill-yellow-400" />
            <span className="text-[10px] text-gray-500">
              {product.ratingAvg.toFixed(1)} ({product.soldCount ?? 0} đã bán)
            </span>
          </div>
        )}
      </div>
    </Link>
  );
}
