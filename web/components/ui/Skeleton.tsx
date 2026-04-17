"use client";

import { cn } from "@/lib/utils/cn";
import { HTMLAttributes } from "react";

export function Skeleton({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn("animate-pulse bg-surface-sunken/80", className)}
      {...rest}
    />
  );
}
