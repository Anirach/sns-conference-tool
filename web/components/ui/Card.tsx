"use client";

import { HTMLAttributes } from "react";
import { cn } from "@/lib/utils/cn";

export function Card({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("bg-card hairline", className)} {...rest} />;
}

export function CardHeader({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("flex flex-col gap-1 p-4 hairline-b", className)} {...rest} />;
}

export function CardTitle({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <h3 className={cn("font-serif text-lg text-foreground", className)} {...rest} />;
}

export function CardDescription({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <p className={cn("font-serif italic text-xs text-muted-foreground", className)} {...rest} />;
}

export function CardContent({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("p-4", className)} {...rest} />;
}

export function CardFooter({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("flex items-center gap-2 p-4 hairline-t", className)} {...rest} />;
}
