"use client";

import { Send } from "lucide-react";
import { FormEvent, useState } from "react";
import { Button } from "@/components/ui/Button";
import { cn } from "@/lib/utils/cn";

interface MessageInputProps {
  onSend: (content: string) => void;
  disabled?: boolean;
}

export function MessageInput({ onSend, disabled }: MessageInputProps) {
  const [value, setValue] = useState("");

  function submit(e: FormEvent) {
    e.preventDefault();
    const trimmed = value.trim();
    if (!trimmed) return;
    onSend(trimmed);
    setValue("");
  }

  return (
    <form
      onSubmit={submit}
      className="flex items-center gap-2 bg-background p-3 hairline-t"
      style={{ paddingBottom: "max(0.75rem, env(safe-area-inset-bottom))" }}
    >
      <input
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder="Message…"
        aria-label="Message"
        disabled={disabled}
        className={cn(
          "h-11 flex-1 rounded-2xl bg-surface-sunken px-4 font-serif text-sm text-foreground placeholder:text-foreground/30",
          "focus:outline-none focus:ring-1 focus:ring-brass-500/40"
        )}
      />
      <Button
        type="submit"
        size="icon"
        className="h-11 w-11 rounded-full p-0"
        disabled={disabled || !value.trim()}
        aria-label="Send"
      >
        <Send className="h-4 w-4" strokeWidth={1.5} />
      </Button>
    </form>
  );
}
