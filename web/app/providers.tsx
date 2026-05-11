"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactNode, useEffect, useState } from "react";
import { ToastProvider } from "@/components/ui/Toast";
import { initMockApi } from "@/lib/api/mocks/init";
import { registerPwa } from "@/lib/pwa/register";
import { useAuthStore } from "@/lib/state/authStore";

export function Providers({ children }: { children: ReactNode }) {
  const [client] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 30_000,
            refetchOnWindowFocus: false,
            retry: 1
          }
        }
      })
  );
  const [mswReady, setMswReady] = useState(process.env.NEXT_PUBLIC_MOCK_API !== "1");

  useEffect(() => {
    // Hydrate auth from localStorage so the bottom tab bar's useIsAdmin() resolves on first
    // paint (not just inside /admin where the layout used to trigger it).
    useAuthStore.getState().hydrate();

    initMockApi()
      .catch((err) => console.warn("MSW init failed; running without mocks", err))
      .finally(() => {
        setMswReady(true);
        // Register the offline app shell SW only when MSW isn't using its slot. The two
        // service workers can't coexist on the same scope.
        if (process.env.NEXT_PUBLIC_MOCK_API !== "1") {
          void registerPwa();
        }
      });
  }, []);

  if (!mswReady) {
    return (
      <div className="flex min-h-dvh items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-brand-500 border-t-transparent" />
      </div>
    );
  }

  return (
    <QueryClientProvider client={client}>
      <ToastProvider>{children}</ToastProvider>
    </QueryClientProvider>
  );
}
