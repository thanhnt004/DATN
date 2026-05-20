import { Navigate, useLocation } from "react-router-dom";
import { useSelector } from "react-redux";
import type { RootState } from "../store/store";
import { useAuth } from "../hooks/useAuth";
import { Loader2 } from "lucide-react";

interface Props {
  children: React.ReactNode;
  /** Required roles — user must have at least one */
  roles?: string[];
}

/**
 * Route guard: redirects to /login if not authenticated,
 * or shows 403 if missing required role.
 */
export default function AuthGuard({ children, roles }: Props) {
  const { isAuthenticated, roles: userRoles } = useAuth();
  const sessionRestored = useSelector((s: RootState) => s.auth.sessionRestored);
  const location = useLocation();

  console.log('[AuthGuard]', { path: location.pathname, sessionRestored, isAuthenticated, userRoles, requiredRoles: roles });

  // Wait for the initial refresh-token attempt to finish before deciding
  if (!sessionRestored) {
    console.log('[AuthGuard] waiting for session restore...');
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Loader2 className="w-8 h-8 animate-spin text-red-500" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (roles && roles.length > 0) {
    const hasRequiredRole = roles.some((r) => userRoles.includes(r));
    if (!hasRequiredRole) {
      return (
        <div className="flex items-center justify-center min-h-screen">
          <div className="text-center">
            <h1 className="text-6xl font-bold text-gray-300 mb-4">403</h1>
            <p className="text-xl text-gray-600 mb-6">Bạn không có quyền truy cập trang này</p>
            <a href="/" className="text-orange-500 hover:text-orange-600 font-medium">
              Quay về trang chủ
            </a>
          </div>
        </div>
      );
    }
  }

  return <>{children}</>;
}
