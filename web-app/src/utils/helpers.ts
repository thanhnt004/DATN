/**
 * Decode JWT payload without external library.
 * Returns null if token is invalid.
 */
export function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split(".");
    if (parts.length !== 3) return null;
    const payload = parts[1];
    const decoded = atob(payload.replace(/-/g, "+").replace(/_/g, "/"));
    return JSON.parse(decoded);
  } catch {
    return null;
  }
}

/**
 * Extract roles from a Keycloak JWT token.
 * Keycloak stores roles in `realm_access.roles`.
 */
export function getRolesFromToken(token: string): string[] {
  const payload = decodeJwtPayload(token);
  if (!payload) return [];
  const realmAccess = payload.realm_access as { roles?: string[] } | undefined;
  return realmAccess?.roles ?? [];
}

/**
 * Check if token has specific role.
 */
export function hasRole(token: string, role: string): boolean {
  return getRolesFromToken(token).includes(role);
}

/**
 * Format price to Vietnamese currency.
 */
export function formatPrice(price: number | string | null | undefined): string {
  const value = Number(price ?? 0);
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(Number.isNaN(value) ? 0 : value);
}

/**
 * Format date string to Vietnamese locale.
 */
export function formatDate(dateStr: string | null | undefined): string {
  if (!dateStr) return "";
  const date = new Date(dateStr);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleDateString("vi-VN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
}

/**
 * Format date+time string.
 */
export function formatDateTime(dateStr: string): string {
  return new Date(dateStr).toLocaleString("vi-VN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

/**
 * Truncate text with ellipsis.
 */
export function truncate(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text;
  return text.slice(0, maxLength) + "...";
}
