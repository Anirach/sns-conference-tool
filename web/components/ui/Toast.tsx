"use client";

import * as ToastPrimitive from "@radix-ui/react-toast";
import { createContext, ReactNode, useCallback, useContext, useRef, useState } from "react";
import { X } from "lucide-react";
import { cn } from "@/lib/utils/cn";

interface ToastItem {
  id: number;
  title?: string;
  description?: string;
  variant?: "default" | "success" | "error";
}

interface ToastContext {
  toast: (t: Omit<ToastItem, "id">) => void;
}

const Ctx = createContext<ToastContext | null>(null);

export function useToast(): ToastContext {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useToast must be used inside ToastProvider");
  return ctx;
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([]);
  const nextId = useRef(1);

  const toast = useCallback((t: Omit<ToastItem, "id">) => {
    const id = nextId.current++;
    setItems((prev) => [...prev, { ...t, id }]);
  }, []);

  const remove = (id: number) => setItems((prev) => prev.filter((x) => x.id !== id));

  return (
    <Ctx.Provider value={{ toast }}>
      <ToastPrimitive.Provider swipeDirection="right">
        {children}
        {items.map((item) => (
          <ToastPrimitive.Root
            key={item.id}
            duration={4000}
            onOpenChange={(open) => !open && remove(item.id)}
            className={cn(
              "flex items-start gap-3 bg-background p-4 shadow-lg hairline",
              item.variant === "success" && "border-brass-500",
              item.variant === "error" && "border-danger"
            )}
          >
            <div className="flex-1">
              {item.title ? (
                <ToastPrimitive.Title className="font-serif text-sm text-foreground">
                  {item.title}
                </ToastPrimitive.Title>
              ) : null}
              {item.description ? (
                <ToastPrimitive.Description className="mt-0.5 font-serif italic text-xs text-muted-foreground">
                  {item.description}
                </ToastPrimitive.Description>
              ) : null}
            </div>
            <ToastPrimitive.Close className="text-foreground/50 hover:text-foreground">
              <X className="h-4 w-4" />
            </ToastPrimitive.Close>
          </ToastPrimitive.Root>
        ))}
        <ToastPrimitive.Viewport className="fixed bottom-4 right-4 z-[60] flex w-[360px] max-w-[calc(100vw-2rem)] flex-col gap-2 outline-none" />
      </ToastPrimitive.Provider>
    </Ctx.Provider>
  );
}
