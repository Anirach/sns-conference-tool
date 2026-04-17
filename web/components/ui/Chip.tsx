"use client";

import { HTMLAttributes } from "react";
import { cn } from "@/lib/utils/cn";

type Variant = "default" | "brand" | "success" | "warning";

const styles: Record<Variant, string> = {
  default: "bg-gray-100 text-gray-700",
  brand: "bg-brand-50 text-brand-700",
  success: "bg-green-50 text-green-700",
  warning: "bg-amber-50 text-amber-700"
};

interface ChipProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: Variant;
}

export function Chip({ className, variant = "default", ...rest }: ChipProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium",
        styles[variant],
        className
      )}
      {...rest}
    />
  );
}
