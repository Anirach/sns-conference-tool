"use client";

import { cn } from "@/lib/utils/cn";

type Radius = 20 | 50 | 100;

interface VicinityRadiusSelectorProps {
  value: Radius;
  onChange: (value: Radius) => void;
  className?: string;
}

const OPTIONS: Radius[] = [20, 50, 100];

export function VicinityRadiusSelector({ value, onChange, className }: VicinityRadiusSelectorProps) {
  return (
    <div
      role="radiogroup"
      aria-label="Search radius"
      className={cn("inline-flex rounded-full bg-surface-sunken p-1", className)}
    >
      {OPTIONS.map((r) => {
        const active = r === value;
        return (
          <button
            key={r}
            role="radio"
            aria-checked={active}
            onClick={() => onChange(r)}
            type="button"
            className={cn(
              "rounded-full px-4 py-1.5 text-sm font-semibold tabular-nums transition-all",
              active ? "bg-background text-brand-700 shadow-sm" : "text-muted-foreground hover:text-foreground"
            )}
          >
            {r} m
          </button>
        );
      })}
    </div>
  );
}

export const RadiusSelector = VicinityRadiusSelector;
