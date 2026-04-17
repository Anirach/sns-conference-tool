"use client";

import { cn } from "@/lib/utils/cn";

interface VicinityRadiusSelectorProps {
  value: 20 | 50 | 100;
  onChange: (value: 20 | 50 | 100) => void;
}

const OPTIONS: Array<{ value: 20 | 50 | 100; label: string; description: string }> = [
  { value: 20, label: "20 m", description: "Same room" },
  { value: 50, label: "50 m", description: "Same floor" },
  { value: 100, label: "100 m", description: "Same building" }
];

export function VicinityRadiusSelector({ value, onChange }: VicinityRadiusSelectorProps) {
  return (
    <div className="flex flex-col gap-2">
      <div className="text-sm font-medium text-gray-700">Vicinity radius</div>
      <div className="grid grid-cols-3 gap-2">
        {OPTIONS.map((o) => {
          const active = o.value === value;
          return (
            <button
              key={o.value}
              type="button"
              onClick={() => onChange(o.value)}
              className={cn(
                "flex flex-col items-center justify-center rounded-xl border px-3 py-2.5 text-sm transition-colors",
                active
                  ? "border-brand-500 bg-brand-50 text-brand-700"
                  : "border-gray-200 bg-white text-gray-600 hover:border-gray-300"
              )}
              aria-pressed={active}
            >
              <span className="text-base font-semibold">{o.label}</span>
              <span className="text-[11px] uppercase tracking-wide">{o.description}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
