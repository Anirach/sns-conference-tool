"use client";

import { ButtonHTMLAttributes, forwardRef } from "react";
import { cn } from "@/lib/utils/cn";

type Variant = "primary" | "secondary" | "outline" | "ghost" | "danger" | "brass";
type Size = "sm" | "md" | "lg" | "icon";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
  fullWidth?: boolean;
}

const variantStyles: Record<Variant, string> = {
  primary: "bg-brand-500 text-background hover:bg-brand-700 disabled:bg-brand-200",
  secondary: "bg-surface-sunken text-foreground hover:bg-muted disabled:text-muted-foreground",
  outline: "bg-transparent text-foreground hairline hover:border-brass-500 hover:text-brass-500",
  ghost: "bg-transparent text-foreground hover:text-brass-500",
  danger: "bg-danger text-background hover:bg-danger/90",
  brass: "bg-brass-500 text-background hover:bg-brass-600"
};

const sizeStyles: Record<Size, string> = {
  sm: "h-9 px-3 text-xs uppercase tracking-[0.14em] font-semibold rounded-sm",
  md: "h-11 px-5 text-sm uppercase tracking-[0.14em] font-semibold rounded-sm",
  lg: "h-12 px-6 text-sm uppercase tracking-[0.18em] font-semibold rounded-sm",
  icon: "h-11 w-11 rounded-sm"
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { className, variant = "primary", size = "md", loading, disabled, children, fullWidth, ...rest },
  ref
) {
  return (
    <button
      ref={ref}
      disabled={disabled || loading}
      className={cn(
        "inline-flex items-center justify-center gap-2 transition-colors",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass-500 focus-visible:ring-offset-2 focus-visible:ring-offset-background",
        "disabled:cursor-not-allowed",
        variantStyles[variant],
        sizeStyles[size],
        fullWidth && "w-full",
        className
      )}
      {...rest}
    >
      {loading ? (
        <span className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
      ) : null}
      {children}
    </button>
  );
});
