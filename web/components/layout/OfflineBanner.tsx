"use client";

import { WifiOff } from "lucide-react";
import { useEventStore } from "@/lib/state/eventStore";

export function OfflineBanner() {
  const online = useEventStore((s) => s.online);
  if (online) return null;
  return (
    <div className="sticky top-0 z-40 flex items-center justify-center gap-2 bg-amber-500 px-3 py-1.5 text-xs font-medium text-white">
      <WifiOff className="h-3.5 w-3.5" />
      You are offline — showing cached data
    </div>
  );
}
