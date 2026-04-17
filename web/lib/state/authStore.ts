"use client";

import { create } from "zustand";
import { bridge } from "../bridge/client";
import type { AuthTokens, User } from "../fixtures/types";

interface AuthState {
  user: User | null;
  tokens: AuthTokens | null;
  hydrated: boolean;
  setSession: (tokens: AuthTokens, user?: User) => Promise<void>;
  setUser: (user: User) => void;
  hydrate: () => Promise<void>;
  signOut: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  tokens: null,
  hydrated: false,

  async setSession(tokens, user) {
    await bridge.call("storage.set", { key: "jwt", value: tokens.accessToken }).catch(() => null);
    await bridge.call("storage.set", { key: "refresh", value: tokens.refreshToken }).catch(() => null);
    set({ tokens, user: user ?? get().user });
  },

  setUser(user) {
    set({ user });
  },

  async hydrate() {
    const jwt = await bridge.call<string | null>("storage.get", { key: "jwt" }).catch(() => null);
    const refresh = await bridge.call<string | null>("storage.get", { key: "refresh" }).catch(() => null);
    if (jwt && refresh) {
      set({ tokens: { accessToken: jwt, refreshToken: refresh, userId: "hydrated" } });
    }
    set({ hydrated: true });
  },

  async signOut() {
    await bridge.call("storage.delete", { key: "jwt" }).catch(() => null);
    await bridge.call("storage.delete", { key: "refresh" }).catch(() => null);
    set({ tokens: null, user: null });
  }
}));
