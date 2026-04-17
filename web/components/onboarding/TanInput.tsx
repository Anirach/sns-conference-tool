"use client";

import { useEffect, useRef, useState } from "react";
import { cn } from "@/lib/utils/cn";

interface TanInputProps {
  length?: number;
  onComplete: (code: string) => void;
  disabled?: boolean;
}

export function TanInput({ length = 6, onComplete, disabled }: TanInputProps) {
  const [vals, setVals] = useState<string[]>(Array(length).fill(""));
  const refs = useRef<(HTMLInputElement | null)[]>([]);

  useEffect(() => {
    refs.current[0]?.focus();
  }, []);

  const setAt = (i: number, v: string) => {
    const next = [...vals];
    next[i] = v;
    setVals(next);
    if (next.every((x) => x !== "")) onComplete(next.join(""));
  };

  return (
    <div className="flex justify-center gap-2">
      {vals.map((v, i) => (
        <input
          key={i}
          ref={(el) => {
            refs.current[i] = el;
          }}
          type="text"
          inputMode="numeric"
          maxLength={1}
          value={v}
          disabled={disabled}
          onChange={(e) => {
            const ch = e.target.value.replace(/\D/g, "").slice(-1);
            setAt(i, ch);
            if (ch && i < length - 1) refs.current[i + 1]?.focus();
          }}
          onKeyDown={(e) => {
            if (e.key === "Backspace" && !vals[i] && i > 0) refs.current[i - 1]?.focus();
          }}
          onPaste={(e) => {
            const txt = e.clipboardData.getData("text").replace(/\D/g, "").slice(0, length);
            if (!txt) return;
            e.preventDefault();
            const next = Array(length).fill("");
            for (let k = 0; k < txt.length; k++) next[k] = txt[k];
            setVals(next);
            refs.current[Math.min(txt.length, length - 1)]?.focus();
            if (next.every((x) => x !== "")) onComplete(next.join(""));
          }}
          className={cn(
            "h-14 w-11 rounded-sm bg-card text-center font-serif text-xl tabular-nums text-foreground hairline",
            "focus:border-brass-500 focus:outline-none focus:ring-1 focus:ring-brass-500/40"
          )}
        />
      ))}
    </div>
  );
}
