"use client";

import { forwardRef, InputHTMLAttributes, TextareaHTMLAttributes } from "react";
import { cn } from "@/lib/utils/cn";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  hint?: string;
  error?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { className, label, hint, error, id, ...rest },
  ref
) {
  const inputId = id ?? rest.name;
  return (
    <div className="flex flex-col gap-1.5">
      {label ? (
        <label htmlFor={inputId} className="eyebrow text-brass-500">
          {label}
        </label>
      ) : null}
      <input
        ref={ref}
        id={inputId}
        className={cn(
          "h-11 rounded-sm bg-card px-3.5 font-serif text-base text-foreground placeholder:text-foreground/30",
          "hairline",
          "focus-visible:border-brass-500 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-brass-500/40",
          error && "border-danger focus-visible:border-danger focus-visible:ring-danger/40",
          className
        )}
        {...rest}
      />
      {error ? (
        <p className="text-xs text-danger" role="alert">
          {error}
        </p>
      ) : hint ? (
        <p className="text-xs italic text-muted-foreground">{hint}</p>
      ) : null}
    </div>
  );
});

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  hint?: string;
  error?: string;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea(
  { className, label, hint, error, id, ...rest },
  ref
) {
  const inputId = id ?? rest.name;
  return (
    <div className="flex flex-col gap-1.5">
      {label ? (
        <label htmlFor={inputId} className="eyebrow text-brass-500">
          {label}
        </label>
      ) : null}
      <textarea
        ref={ref}
        id={inputId}
        className={cn(
          "min-h-[96px] rounded-sm bg-card px-3.5 py-2.5 font-serif text-base text-foreground placeholder:text-foreground/30",
          "hairline",
          "focus-visible:border-brass-500 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-brass-500/40",
          error && "border-danger",
          className
        )}
        {...rest}
      />
      {error ? (
        <p className="text-xs text-danger" role="alert">
          {error}
        </p>
      ) : hint ? (
        <p className="text-xs italic text-muted-foreground">{hint}</p>
      ) : null}
    </div>
  );
});
