"use client";

import { HTMLAttributes } from "react";
import { X } from "lucide-react";
import { cn } from "@/lib/utils/cn";

type Variant = "default" | "accent" | "outline";

interface ChipProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: Variant;
  onRemove?: () => void;
  label?: string;
}

const variantStyles: Record<Variant, string> = {
  default: "text-foreground/70 hover:text-foreground",
  accent: "text-brass-600 hover:text-brass-700",
  outline: "text-foreground/70 border-b border-foreground/15 hover:border-brass-500"
};

export function Chip({ children, className, variant = "default", onRemove, label, ...rest }: ChipProps) {
  const content = label ?? children;
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 text-[10px] font-semibold uppercase tracking-[0.14em] transition-colors",
        variantStyles[variant],
        className
      )}
      {...rest}
    >
      {content}
      {onRemove ? (
        <button
          onClick={onRemove}
          aria-label="Remove"
          className="ml-0.5 opacity-60 hover:opacity-100"
          type="button"
        >
          <X className="h-3 w-3" strokeWidth={1.5} />
        </button>
      ) : null}
    </span>
  );
}

export const KeywordChip = Chip;
