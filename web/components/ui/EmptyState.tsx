"use client";

import { LucideIcon } from "lucide-react";
import { ReactNode } from "react";
import { Button } from "./Button";

interface EmptyStateProps {
  icon?: LucideIcon | ReactNode;
  title: string;
  description?: string;
  ctaLabel?: string;
  onCta?: () => void;
  action?: ReactNode;
  className?: string;
  eyebrow?: string;
}

function renderIcon(icon: EmptyStateProps["icon"]): ReactNode {
  if (!icon) return null;
  if (typeof icon === "function") {
    const Icon = icon as LucideIcon;
    return <Icon className="h-7 w-7" strokeWidth={1.4} />;
  }
  return icon;
}

export function EmptyState({
  icon,
  title,
  description,
  ctaLabel,
  onCta,
  action,
  eyebrow = "Nota Bene"
}: EmptyStateProps) {
  const rendered = renderIcon(icon);
  return (
    <div className="flex flex-col items-center justify-center px-6 py-16 text-center">
      {rendered ? (
        <div className="mb-6 grid h-16 w-16 place-items-center text-brass-500 hairline">
          {rendered}
        </div>
      ) : null}
      <p className="eyebrow mb-2 text-brass-500">{eyebrow}</p>
      <h3 className="font-serif text-2xl text-foreground">{title}</h3>
      {description ? (
        <p className="mt-2 max-w-xs text-sm leading-relaxed text-muted-foreground">{description}</p>
      ) : null}
      {ctaLabel && onCta ? (
        <Button onClick={onCta} className="mt-6">
          {ctaLabel}
        </Button>
      ) : null}
      {action ? <div className="mt-6">{action}</div> : null}
    </div>
  );
}
