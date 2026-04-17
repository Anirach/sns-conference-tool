"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactNode, useEffect, useState } from "react";
import { BridgeProvider } from "@/components/bridge/BridgeProvider";
import { ToastProvider } from "@/components/ui/Toast";
import { initMockApi } from "@/lib/api/mocks/init";

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
    initMockApi()
      .catch((err) => console.warn("MSW init failed; running without mocks", err))
      .finally(() => setMswReady(true));
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
      <BridgeProvider>
        <ToastProvider>{children}</ToastProvider>
      </BridgeProvider>
    </QueryClientProvider>
  );
}
