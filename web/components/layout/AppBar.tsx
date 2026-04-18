"use client";

import { ChevronLeft } from "lucide-react";
import { useRouter } from "next/navigation";
import { ReactNode } from "react";

interface AppBarProps {
  title: string;
  eyebrow?: string;
  showBack?: boolean;
  trailing?: ReactNode;
  onBack?: () => void;
}

export function AppBar({ title, eyebrow, showBack, trailing, onBack }: AppBarProps) {
  const router = useRouter();
  return (
    <header className="sticky top-0 z-30 bg-background/90 backdrop-blur hairline-b">
      <div className="flex h-14 items-center justify-between gap-2 px-5">
        <div className="flex min-w-0 items-center gap-2">
          {showBack ? (
            <button
              onClick={() => (onBack ? onBack() : router.back())}
              className="-ml-2 p-2 text-foreground/70 transition-colors hover:text-brass-500"
              aria-label="Back"
              type="button"
            >
              <ChevronLeft className="h-5 w-5" strokeWidth={1.5} />
            </button>
          ) : null}
          <div className="min-w-0">
            {eyebrow ? <p className="eyebrow mb-0.5 leading-none text-brass-500">{eyebrow}</p> : null}
            <h1 className="truncate font-serif text-lg leading-none text-foreground">{title}</h1>
          </div>
        </div>
        <div className="flex items-center gap-1">{trailing}</div>
      </div>
    </header>
  );
}
