"use client";

import { create } from "zustand";
import { getItem, removeItem, setItem } from "../native/storage";
import type { AuthTokens, Role, User } from "../fixtures/types";

interface AuthState {
  user: User | null;
  tokens: AuthTokens | null;
  role: Role | null;
  hydrated: boolean;
  setSession: (tokens: AuthTokens, user?: User) => void;
  setUser: (user: User) => void;
  hydrate: () => void;
  signOut: () => void;
}

/** Decode JWT claims client-side. Validation happens at the resource server; this is purely
 *  to surface the user_id + role to UI code without an extra round-trip. Returns null on any
 *  parse failure. */
function decodeJwt(token: string | null | undefined): Record<string, unknown> | null {
  if (!token) return null;
  const parts = token.split(".");
  if (parts.length < 2) return null;
  try {
    const payload = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const padded = payload + "=".repeat((4 - (payload.length % 4)) % 4);
    return JSON.parse(typeof atob === "function" ? atob(padded) : Buffer.from(padded, "base64").toString());
  } catch {
    return null;
  }
}

function roleFromJwt(token: string | null | undefined): Role | null {
  const json = decodeJwt(token);
  const role = json?.role;
  if (role === "USER" || role === "ORGANIZER" || role === "ADMIN" || role === "SUPER_ADMIN") return role;
  return null;
}

function subFromJwt(token: string | null | undefined): string | null {
  const json = decodeJwt(token);
  return typeof json?.sub === "string" ? json.sub : null;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  tokens: null,
  role: null,
  hydrated: false,

  setSession(tokens, user) {
    setItem("auth.jwt", tokens.accessToken);
    setItem("auth.refresh", tokens.refreshToken);
    // Backend response carries userId; keep it but also trust the JWT sub claim if the
    // server omitted it (defensive — they should always match).
    const userId = tokens.userId || subFromJwt(tokens.accessToken) || "";
    set({ tokens: { ...tokens, userId }, user: user ?? get().user, role: roleFromJwt(tokens.accessToken) });
  },

  setUser(user) {
    set({ user });
  },

  hydrate() {
    const jwt = getItem("auth.jwt");
    const refresh = getItem("auth.refresh");
    if (jwt && refresh) {
      // Recover the real user_id from the JWT subject instead of leaving it as a placeholder —
      // ChatWindow.isMine and any other "is this me?" check needs a real UUID after a page reload.
      const userId = subFromJwt(jwt) ?? "";
      set({
        tokens: { accessToken: jwt, refreshToken: refresh, userId },
        role: roleFromJwt(jwt)
      });
    }
    set({ hydrated: true });
  },

  signOut() {
    removeItem("auth.jwt");
    removeItem("auth.refresh");
    set({ tokens: null, user: null, role: null });
  }
}));

/** True when the signed-in user has any admin authority — used to gate the /admin route group. */
export function useIsAdmin(): boolean {
  const role = useAuthStore((s) => s.role);
  return role === "ADMIN" || role === "SUPER_ADMIN";
}

/** Real UUID of the signed-in user, derived from tokens.userId (set on login, refreshed from
 *  the JWT sub claim on hydrate). Returns null when not signed in. */
export function useCurrentUserId(): string | null {
  return useAuthStore((s) => s.tokens?.userId || null);
}
