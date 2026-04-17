"use client";

import { cn } from "@/lib/utils/cn";

export function SimilarityBar({ value, className }: { value: number; className?: string }) {
  const pct = Math.min(100, Math.max(0, Math.round(value * 100)));
  return (
    <div className={cn("w-full", className)}>
      <div className="relative h-px w-full bg-foreground/10">
        <div
          className="absolute inset-y-0 left-0 bg-brand-500 transition-all"
          style={{ width: `${pct}%`, height: "2px", top: "-0.5px" }}
        />
      </div>
    </div>
  );
}
