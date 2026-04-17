"use client";

export function TypingIndicator() {
  return (
    <div className="inline-flex w-fit items-center gap-1 rounded-2xl bg-surface-sunken px-3 py-2 animate-fade-in-up">
      {[0, 1, 2].map((i) => (
        <span
          key={i}
          className="h-1.5 w-1.5 rounded-full bg-muted-foreground/70 animate-typing-bounce"
          style={{ animationDelay: `${i * 0.15}s` }}
        />
      ))}
    </div>
  );
}
