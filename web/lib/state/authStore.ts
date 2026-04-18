"use client";

import { create } from "zustand";
import { bridge } from "../bridge/client";
import type { AuthTokens, Role, User } from "../fixtures/types";

interface AuthState {
  user: User | null;
  tokens: AuthTokens | null;
  role: Role | null;
  hydrated: boolean;
  setSession: (tokens: AuthTokens, user?: User) => Promise<void>;
  setUser: (user: User) => void;
  hydrate: () => Promise<void>;
  signOut: () => Promise<void>;
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

  async setSession(tokens, user) {
    await bridge.call("storage.set", { key: "jwt", value: tokens.accessToken }).catch(() => null);
    await bridge.call("storage.set", { key: "refresh", value: tokens.refreshToken }).catch(() => null);
    set({ tokens, user: user ?? get().user, role: roleFromJwt(tokens.accessToken) });
  },

  setUser(user) {
    set({ user });
  },

  async hydrate() {
    const jwt = await bridge.call<string | null>("storage.get", { key: "jwt" }).catch(() => null);
    const refresh = await bridge.call<string | null>("storage.get", { key: "refresh" }).catch(() => null);
    if (jwt && refresh) {
      set({
        tokens: { accessToken: jwt, refreshToken: refresh, userId: "hydrated" },
        role: roleFromJwt(jwt)
      });
    }
    set({ hydrated: true });
  },

  async signOut() {
    await bridge.call("storage.delete", { key: "jwt" }).catch(() => null);
    await bridge.call("storage.delete", { key: "refresh" }).catch(() => null);
    set({ tokens: null, user: null, role: null });
  }
}));

/** True when the signed-in user has any admin authority — used to gate the /admin route group. */
export function useIsAdmin(): boolean {
  const role = useAuthStore((s) => s.role);
  return role === "ADMIN" || role === "SUPER_ADMIN";
}
