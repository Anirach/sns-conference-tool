"use client";

import { useRouter } from "next/navigation";
import { ReactNode, useEffect } from "react";
import { useAuthStore, useIsAdmin } from "@/lib/state/authStore";

/**
 * Client-side guard. The JWT lives in bridge secure storage (not a cookie), so a server-side
 * guard isn't reachable — the next request from the browser would arrive without credentials.
 * We hydrate from secure storage on first mount and redirect non-admins out before rendering.
 */
export default function AdminLayout({ children }: { children: ReactNode }) {
  const router = useRouter();
  const isAdmin = useIsAdmin();
  const hydrated = useAuthStore((s) => s.hydrated);
  const tokens = useAuthStore((s) => s.tokens);

  useEffect(() => {
    // Trigger hydrate once on first mount — nothing else in the app does it for us, and a fresh
    // tab visit to /admin needs the stored JWT before the role guard can read the role claim.
    if (!hydrated) {
      void useAuthStore.getState().hydrate();
    }
  }, [hydrated]);

  useEffect(() => {
    if (!hydrated) return;
    if (!tokens) {
      router.replace("/login?redirect=/admin");
      return;
    }
    if (!isAdmin) {
      router.replace("/");
    }
  }, [hydrated, tokens, isAdmin, router]);

  if (!hydrated) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-surface-sunken">
        <p className="eyebrow text-brass-500">Loading the registry…</p>
      </div>
    );
  }
  if (!tokens || !isAdmin) {
    return null;
  }

  // No wrapper — each /admin/* page composes its own AppShell so the participant
  // BottomTabBar (with the Registry tab highlighted) stays visible inside admin.
  return <>{children}</>;
}
