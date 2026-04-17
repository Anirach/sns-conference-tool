"use client";

import * as DialogPrimitive from "@radix-ui/react-dialog";
import { X } from "lucide-react";
import { ReactNode } from "react";
import { cn } from "@/lib/utils/cn";

interface DialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title?: string;
  eyebrow?: string;
  description?: string;
  children: ReactNode;
  footer?: ReactNode;
}

export function Dialog({ open, onOpenChange, title, eyebrow, description, children, footer }: DialogProps) {
  return (
    <DialogPrimitive.Root open={open} onOpenChange={onOpenChange}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-foreground/40 data-[state=open]:animate-in data-[state=open]:fade-in-0" />
        <DialogPrimitive.Content
          className={cn(
            "fixed left-1/2 top-1/2 z-50 w-[calc(100vw-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2",
            "bg-background p-6 shadow-xl hairline"
          )}
        >
          {eyebrow ? <p className="eyebrow mb-1 text-brass-500">{eyebrow}</p> : null}
          {title ? (
            <DialogPrimitive.Title className="font-serif text-xl leading-tight text-foreground">
              {title}
            </DialogPrimitive.Title>
          ) : null}
          {description ? (
            <DialogPrimitive.Description className="mt-2 font-serif italic text-sm text-muted-foreground">
              {description}
            </DialogPrimitive.Description>
          ) : null}
          <div className="mt-5">{children}</div>
          {footer ? <div className="mt-6 flex justify-end gap-2">{footer}</div> : null}
          <DialogPrimitive.Close className="absolute right-3 top-3 p-1 text-foreground/50 hover:text-brass-500">
            <X className="h-4 w-4" />
            <span className="sr-only">Close</span>
          </DialogPrimitive.Close>
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  );
}
