import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import { Toaster } from "react-hot-toast";
import MainLayout from "./layouts/MainLayout";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import VerifyEmailPage from "./pages/VerifyEmailPage";
import OAuthCallbackPage from "./pages/OAuthCallbackPage";
import UserLayout from "./pages/user/UserLayout";
import ProfilePage from "./pages/user/ProfilePage";
import AddressesPage from "./pages/user/AddressesPage";
import { NotificationsPage, NotificationSettingsPage } from "./pages/user/NotificationPages";
import { TermsPage, SupportPage } from "./pages/user/PlaceholderPages";
import SellerRegisterPage from "./pages/user/SellerRegisterPage";
import AdminLayout from "./pages/admin/AdminLayout";
import AdminDashboardPage from "./pages/admin/AdminDashboardPage";
import AdminUsersPage from "./pages/admin/AdminUsersPage";
import AdminSellersPage from "./pages/admin/AdminSellersPage";
import AdminBannersPage from "./pages/admin/AdminBannersPage";
import AdminCategoriesPage from "./pages/admin/AdminCategoriesPage";
import AdminRolesPage from "./pages/admin/AdminRolesPage";
import AdminCouponsPage from "./pages/admin/AdminCouponsPage";
import AdminCampaignsPage from "./pages/admin/AdminCampaignsPage";
import AdminOptionsPage from "./pages/admin/AdminOptionsPage";
import AdminProductsPage from "./pages/admin/AdminProductsPage";
import SellerLayout from "./pages/seller/SellerLayout";
import SellerDashboardPage from "./pages/seller/SellerDashboardPage";
import SellerShopPage from "./pages/seller/SellerShopPage";
import SellerProductsPage from "./pages/seller/SellerProductsPage";
import SellerAddProductPage from "./pages/seller/SellerAddProductPage";
import SellerEditProductPage from "./pages/seller/SellerEditProductPage";
import SellerOrdersPage from "./pages/seller/SellerOrdersPage";
import SellerReviewsPage from "./pages/seller/SellerReviewsPage";
import SellerCouponsPage from "./pages/seller/SellerCouponsPage";
import SellerRevenuePage from "./pages/seller/SellerRevenuePage";
import SellerDocumentsPage from "./pages/seller/SellerDocumentsPage";
import SellerBankAccountPage from "./pages/seller/SellerBankAccountPage";
import SellerOptionsPage from "./pages/seller/SellerOptionsPage";
import ResponsiveProductPage from "./pages/ResponsiveProductPage";
import CartPage from "./pages/CartPage";
import CheckoutPage from "./pages/CheckoutPage";
import SearchPage from "./pages/SearchPage";
import ShopPage from "./pages/ShopPage";
import PaymentResultPage from "./pages/PaymentResultPage";
import BuyerOrdersPage from "./pages/user/BuyerOrdersPage";
import OrderDetailPage from "./pages/user/OrderDetailPage";
import BuyerVouchersPage from "./pages/user/BuyerVouchersPage";
import FollowingPage from "./pages/user/FollowingPage";
import AIChatPage from "./pages/user/AIChatPage";
import ChatPage from "./pages/shared/ChatPage";
import AuthGuard from "./components/AuthGuard";
import ScrollToTop from "./components/ScrollToTop";

function App() {
  return (
    <Router>
      <ScrollToTop />
      <Toaster position="top-right" toastOptions={{ duration: 4000, style: { fontWeight: 600, fontSize: '14px' } }} />
      <Routes>
        {/* Public routes (no shared layout) */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route path="/oauth/callback" element={<OAuthCallbackPage />} />

        {/* User account section — protected, own layout with sidebar */}
        <Route path="/user" element={<AuthGuard><UserLayout /></AuthGuard>}>
          <Route index element={<Navigate to="/user/profile" replace />} />
          <Route path="profile" element={<ProfilePage />} />
          <Route path="addresses" element={<AddressesPage />} />
          <Route path="notifications" element={<NotificationsPage />} />
          <Route path="notification-settings" element={<NotificationSettingsPage />} />
          <Route path="orders" element={<BuyerOrdersPage />} />
          <Route path="orders/:orderId" element={<OrderDetailPage />} />
          <Route path="vouchers" element={<BuyerVouchersPage />} />
          <Route path="following" element={<FollowingPage />} />
          <Route path="ai-chat" element={<AIChatPage />} />
          <Route path="messages" element={<ChatPage mode="buyer" />} />
          <Route path="terms" element={<TermsPage />} />
          <Route path="support" element={<SupportPage />} />
          <Route path="sell" element={<SellerRegisterPage />} />
        </Route>

        {/* Admin section — protected with ADMIN role */}
        <Route path="/admin" element={<AuthGuard roles={["ADMIN"]}><AdminLayout /></AuthGuard>}>
          <Route index element={<AdminDashboardPage />} />
          <Route path="users" element={<AdminUsersPage />} />
          <Route path="sellers" element={<AdminSellersPage />} />
          <Route path="banners" element={<AdminBannersPage />} />
          <Route path="categories" element={<AdminCategoriesPage />} />
          <Route path="options" element={<AdminOptionsPage />} />
          <Route path="products" element={<AdminProductsPage />} />
          <Route path="roles" element={<AdminRolesPage />} />
          <Route path="coupons" element={<AdminCouponsPage />} />
          <Route path="campaigns" element={<AdminCampaignsPage />} />
        </Route>

        {/* Seller section — protected with SELLER role */}
        <Route path="/seller" element={<AuthGuard roles={["SELLER"]}><SellerLayout /></AuthGuard>}>
          <Route index element={<SellerDashboardPage />} />
          <Route path="shop" element={<SellerShopPage />} />
          <Route path="products" element={<SellerProductsPage />} />
          <Route path="products/new" element={<SellerAddProductPage />} />
          <Route path="products/:id/edit" element={<SellerEditProductPage />} />
          <Route path="options" element={<SellerOptionsPage />} />
          <Route path="orders" element={<SellerOrdersPage />} />
          <Route path="reviews" element={<SellerReviewsPage />} />
          <Route path="coupons" element={<SellerCouponsPage />} />
          <Route path="revenue" element={<SellerRevenuePage />} />
          <Route path="documents" element={<SellerDocumentsPage />} />
          <Route path="bank" element={<SellerBankAccountPage />} />
          <Route path="messages" element={<ChatPage mode="seller" />} />
        </Route>

        {/* Routes with MainLayout (Header + Footer) */}
        <Route path="/" element={<MainLayout />}>
          <Route index element={<HomePage />} />
          <Route path="product/:id" element={<ResponsiveProductPage />} />
          <Route path="shop/:sellerId" element={<ShopPage />} />
          <Route path="cart" element={<CartPage />} />
          <Route path="checkout" element={<CheckoutPage />} />
          <Route path="search" element={<SearchPage />} />
          <Route path="payment/result" element={<PaymentResultPage />} />
          <Route path="*" element={<div className="text-center py-20 text-4xl font-black text-gray-900 leading-tight">404 - Oops! Nothing here.</div>} />
        </Route>
      </Routes>
    </Router>
  );
}

export default App;
