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

/** Decode the `role` claim out of a JWT. Returns null on any parse failure. */
function roleFromJwt(token: string | null | undefined): Role | null {
  if (!token) return null;
  const parts = token.split(".");
  if (parts.length < 2) return null;
  try {
    const payload = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const padded = payload + "=".repeat((4 - (payload.length % 4)) % 4);
    const json = JSON.parse(typeof atob === "function" ? atob(padded) : Buffer.from(padded, "base64").toString());
    const role = json.role;
    if (role === "USER" || role === "ORGANIZER" || role === "ADMIN" || role === "SUPER_ADMIN") return role;
    return null;
  } catch {
    return null;
  }
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  tokens: null,
  role: null,
  hydrated: false,

  setSession(tokens, user) {
    setItem("auth.jwt", tokens.accessToken);
    setItem("auth.refresh", tokens.refreshToken);
    set({ tokens, user: user ?? get().user, role: roleFromJwt(tokens.accessToken) });
  },

  setUser(user) {
    set({ user });
  },

  hydrate() {
    const jwt = getItem("auth.jwt");
    const refresh = getItem("auth.refresh");
    if (jwt && refresh) {
      set({
        tokens: { accessToken: jwt, refreshToken: refresh, userId: "hydrated" },
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
