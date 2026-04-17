"use client";

import { Send } from "lucide-react";
import { FormEvent, useState } from "react";
import { Button } from "@/components/ui/Button";

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
      className="flex items-center gap-2 border-t border-gray-200 bg-white p-3 pb-[calc(0.75rem+env(safe-area-inset-bottom))]"
    >
      <input
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder="Type a message"
        aria-label="Message"
        disabled={disabled}
        className="h-11 flex-1 rounded-full border border-gray-300 bg-gray-50 px-4 text-sm focus-visible:border-brand-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500/30"
      />
      <Button
        type="submit"
        size="md"
        className="h-11 w-11 rounded-full p-0"
        disabled={disabled || !value.trim()}
        aria-label="Send"
      >
        <Send className="h-4 w-4" />
      </Button>
    </form>
  );
}
