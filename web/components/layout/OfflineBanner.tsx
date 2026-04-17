"use client";

import { WifiOff } from "lucide-react";
import { useEventStore } from "@/lib/state/eventStore";

export function OfflineBanner() {
  const online = useEventStore((s) => s.online);
  if (online) return null;
  return (
    <div className="sticky top-0 z-40 flex items-center justify-center gap-2 bg-brass-500 px-3 py-1.5 eyebrow text-background">
      <WifiOff className="h-3 w-3" strokeWidth={1.5} />
      Offline — showing cached data
    </div>
  );
}
