import { ShoppingBag, Ticket, FileText, MessageSquare } from "lucide-react";

function ComingSoon({ icon: Icon, title, desc }: { icon: React.FC<React.SVGProps<SVGSVGElement>>; title: string; desc: string }) {
  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm">
      <div className="flex items-center gap-3 px-6 py-5 border-b border-gray-50">
        <div className="w-9 h-9 bg-red-100 rounded-xl flex items-center justify-center">
          <Icon className="text-red-600" style={{ width: 18, height: 18 }} />
        </div>
        <h1 className="font-black text-gray-900 text-lg">{title}</h1>
      </div>
      <div className="flex flex-col items-center justify-center py-20 gap-4 text-center px-6">
        <div className="w-16 h-16 bg-gray-100 rounded-2xl flex items-center justify-center">
          <Icon className="text-gray-300" style={{ width: 32, height: 32 }} />
        </div>
        <p className="font-bold text-gray-600">{desc}</p>
        <span className="text-xs font-bold px-3 py-1.5 bg-red-100 text-red-600 rounded-full">
          Sắp ra mắt
        </span>
      </div>
    </div>
  );
}

export function OrdersPage() {
  return <ComingSoon icon={ShoppingBag} title="Lịch sử mua hàng" desc="Tất cả đơn hàng của bạn sẽ hiện thị tại đây" />;
}

export function VouchersPage() {
  return <ComingSoon icon={Ticket} title="Kho voucher" desc="Các mã giảm giá và ưu đãi của bạn sẽ hiện thị tại đây" />;
}

export function TermsPage() {
  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm">
      <div className="flex items-center gap-3 px-6 py-5 border-b border-gray-50">
        <div className="w-9 h-9 bg-red-100 rounded-xl flex items-center justify-center">
          <FileText className="text-red-600" style={{ width: 18, height: 18 }} />
        </div>
        <h1 className="font-black text-gray-900 text-lg">Điều khoản sử dụng</h1>
      </div>
      <div className="px-6 py-8 space-y-5 text-sm text-gray-600 leading-relaxed max-w-2xl">
        {[
          { title: "1. Chấp nhận điều khoản", body: "Khi sử dụng dịch vụ của Sellico, bạn đồng ý tuân thủ các điều khoản và điều kiện được nêu trong tài liệu này." },
          { title: "2. Tài khoản người dùng", body: "Bạn chịu trách nhiệm bảo mật thông tin đăng nhập và mọi hoạt động xảy ra dưới tài khoản của mình." },
          { title: "3. Quyền riêng tư", body: "Chúng tôi cam kết bảo vệ thông tin cá nhân của bạn theo chính sách bảo mật của Sellico." },
          { title: "4. Chính sách thanh toán", body: "Tất cả giao dịch được xử lý an toàn. Sellico không lưu trữ thông tin thẻ tín dụng của bạn." },
          { title: "5. Giải quyết tranh chấp", body: "Mọi tranh chấp phát sinh sẽ được giải quyết theo quy định của pháp luật Việt Nam." },
        ].map(({ title, body }) => (
          <div key={title}>
            <h3 className="font-black text-gray-800 mb-1">{title}</h3>
            <p>{body}</p>
          </div>
        ))}
      </div>
    </div>
  );
}

export function SupportPage() {
  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm">
      <div className="flex items-center gap-3 px-6 py-5 border-b border-gray-50">
        <div className="w-9 h-9 bg-red-100 rounded-xl flex items-center justify-center">
          <MessageSquare className="text-red-600" style={{ width: 18, height: 18 }} />
        </div>
        <h1 className="font-black text-gray-900 text-lg">Góp ý - Phản hồi - Hỗ trợ</h1>
      </div>
      <div className="px-6 py-8 space-y-4 max-w-xl">
        <p className="text-sm text-gray-500">Chúng tôi luôn lắng nghe ý kiến của bạn để cải thiện dịch vụ.</p>
        <div className="space-y-1.5">
          <label className="text-xs font-bold text-gray-500 uppercase tracking-widest">Loại phản hồi</label>
          <select className="w-full px-4 py-2.5 rounded-xl border-2 border-gray-100 bg-gray-50 text-sm font-semibold outline-none focus:border-red-400 transition-all">
            <option>Góp ý cải tiến sản phẩm</option>
            <option>Báo lỗi hệ thống</option>
            <option>Hỗ trợ đơn hàng</option>
            <option>Khiếu nại</option>
            <option>Khác</option>
          </select>
        </div>
        <div className="space-y-1.5">
          <label className="text-xs font-bold text-gray-500 uppercase tracking-widest">Nội dung</label>
          <textarea
            rows={5}
            placeholder="Mô tả chi tiết vấn đề hoặc góp ý của bạn..."
            className="w-full px-4 py-3 rounded-xl border-2 border-gray-100 bg-gray-50 text-sm font-semibold outline-none focus:border-red-400 transition-all resize-none"
          />
        </div>
        <button className="px-6 py-2.5 bg-red-600 hover:bg-red-700 text-white text-sm font-bold rounded-xl transition-all">
          Gửi phản hồi
        </button>
        <p className="text-xs text-gray-400 pt-1">
          Hoặc liên hệ trực tiếp qua email: <span className="font-bold text-gray-600">support@sellico.vn</span>
        </p>
      </div>
    </div>
  );
}
