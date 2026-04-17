"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Radar, Users, MessageCircle, Settings } from "lucide-react";
import { cn } from "@/lib/utils/cn";

const tabs = [
  {
    to: "/events/join",
    label: "Discover",
    Icon: Radar,
    match: (p: string) => p === "/events/join" || p === "/" || /^\/events\/[^/]+$/.test(p)
  },
  {
    to: "/matches",
    label: "Fellows",
    Icon: Users,
    match: (p: string) =>
      p === "/matches" || /^\/matches\//.test(p) || /^\/events\/[^/]+\/vicinity/.test(p) || p === "/interests"
  },
  {
    to: "/chats",
    label: "Letters",
    Icon: MessageCircle,
    match: (p: string) => p === "/chats" || /^\/events\/[^/]+\/chat/.test(p) || p.startsWith("/profile")
  },
  {
    to: "/settings",
    label: "Study",
    Icon: Settings,
    match: (p: string) => p.startsWith("/settings")
  }
];

const HIDDEN_PATTERNS = [
  /^\/$/,
  /^\/register$/,
  /^\/verify$/,
  /^\/login$/,
  /^\/events\/[^/]+\/chat\/[^/]+$/,
  /^\/matches\/[^/]+$/
];

export function BottomTabBar() {
  const pathname = usePathname();
  if (HIDDEN_PATTERNS.some((re) => re.test(pathname))) return null;

  return (
    <nav
      className="sticky bottom-0 z-30 grid h-16 grid-cols-4 bg-background/95 backdrop-blur hairline-t"
      style={{ paddingBottom: "env(safe-area-inset-bottom)" }}
    >
      {tabs.map(({ to, label, Icon, match }) => {
        const active = match(pathname);
        return (
          <Link
            key={to}
            href={to}
            className={cn(
              "relative flex flex-col items-center justify-center gap-1 transition-colors",
              active ? "text-foreground" : "text-muted-foreground hover:text-foreground"
            )}
          >
            {active ? (
              <span className="absolute left-1/2 top-0 h-px w-8 -translate-x-1/2 bg-brass-500" />
            ) : null}
            <Icon className="h-[18px] w-[18px]" strokeWidth={active ? 1.8 : 1.4} />
            <span className="text-[9px] font-semibold uppercase tracking-[0.18em]">{label}</span>
          </Link>
        );
      })}
    </nav>
  );
}
