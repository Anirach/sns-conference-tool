"use client";

import { ReactNode } from "react";

interface StatCardProps {
  label: string;
  value: ReactNode;
  hint?: ReactNode;
  accent?: "default" | "warn" | "danger";
}

export function StatCard({ label, value, hint, accent = "default" }: StatCardProps) {
  const accentColor =
    accent === "danger"
      ? "text-red-700"
      : accent === "warn"
      ? "text-amber-700"
      : "text-foreground";
  return (
    <div className="hairline rounded-md bg-surface px-5 py-4">
      <p className="eyebrow text-brass-500">{label}</p>
      <p className={`mt-2 font-serif text-3xl tabular-nums ${accentColor}`}>{value}</p>
      {hint ? <p className="mt-1 text-xs text-foreground/60">{hint}</p> : null}
    </div>
  );
}
