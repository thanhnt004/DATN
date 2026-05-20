import { useSelector } from "react-redux";
import type { RootState } from "../store/store";
import { getRolesFromToken } from "../utils/helpers";
import { useMemo } from "react";

/**
 * Hook to get current user's roles from JWT token.
 */
export function useAuth() {
  const { accessToken, isAuthenticated, sessionRestored } = useSelector((s: RootState) => s.auth);

  const roles = useMemo(() => {
    if (!accessToken) return [] as string[];
    const r = getRolesFromToken(accessToken);
    console.log('[useAuth] decoded roles from JWT:', r);
    return r;
  }, [accessToken]);

  console.log('[useAuth] state:', { isAuthenticated, sessionRestored, hasToken: !!accessToken, roles });

  const isAdmin = roles.includes("ADMIN");
  const isSeller = roles.includes("SELLER");
  const isBuyer = roles.includes("BUYER");

  return {
    accessToken,
    isAuthenticated,
    sessionRestored,
    roles,
    isAdmin,
    isSeller,
    isBuyer,
  };
}
