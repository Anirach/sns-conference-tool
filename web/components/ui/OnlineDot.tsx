"use client";

import { cn } from "@/lib/utils/cn";

export function OnlineDot({ online, className }: { online: boolean; className?: string }) {
  return (
    <span
      aria-label={online ? "in attendance" : "absent"}
      className={cn(
        "inline-block h-2 w-2 rounded-full ring-2 ring-background",
        online ? "bg-success animate-pulse-halo" : "bg-foreground/20",
        className
      )}
    />
  );
}
