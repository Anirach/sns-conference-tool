"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { ArrowLeft, CalendarDays, MessageCircle, Users, User as UserIcon } from "lucide-react";
import { ReactNode } from "react";
import { cn } from "@/lib/utils/cn";

interface AppShellProps {
  title?: string;
  subtitle?: string;
  showBack?: boolean;
  right?: ReactNode;
  hideTabs?: boolean;
  children: ReactNode;
}

const TABS = [
  { href: "/events/join", label: "Events", icon: CalendarDays },
  { href: "/interests", label: "Interests", icon: Users },
  { href: "/profile", label: "Profile", icon: UserIcon }
];

export function AppShell({ title, subtitle, showBack, right, hideTabs, children }: AppShellProps) {
  const router = useRouter();
  const pathname = usePathname();

  return (
    <div className="flex min-h-dvh flex-col bg-gray-50">
      {title ? (
        <header className="sticky top-0 z-30 border-b border-gray-200 bg-white/90 pt-safe-top backdrop-blur">
          <div className="mx-auto flex max-w-2xl items-center gap-3 px-4 py-3">
            {showBack ? (
              <button
                type="button"
                onClick={() => router.back()}
                aria-label="Back"
                className="-ml-1 rounded-full p-1.5 text-gray-600 hover:bg-gray-100"
              >
                <ArrowLeft className="h-5 w-5" />
              </button>
            ) : null}
            <div className="min-w-0 flex-1">
              <h1 className="truncate text-base font-semibold text-gray-900">{title}</h1>
              {subtitle ? <p className="truncate text-xs text-gray-500">{subtitle}</p> : null}
            </div>
            {right}
          </div>
        </header>
      ) : null}

      <main className="flex-1">
        <div className="mx-auto w-full max-w-2xl px-4 py-4 pb-28">{children}</div>
      </main>

      {!hideTabs ? (
        <nav className="fixed bottom-0 left-0 right-0 z-30 border-t border-gray-200 bg-white/95 pb-safe-bottom backdrop-blur">
          <div className="mx-auto flex max-w-2xl items-stretch justify-around">
            {TABS.map((t) => {
              const active = pathname.startsWith(t.href);
              const Icon = t.icon;
              return (
                <Link
                  key={t.href}
                  href={t.href}
                  className={cn(
                    "flex flex-1 flex-col items-center gap-0.5 py-2.5 text-[11px]",
                    active ? "text-brand-600" : "text-gray-500 hover:text-gray-700"
                  )}
                >
                  <Icon className="h-5 w-5" />
                  {t.label}
                </Link>
              );
            })}
            <Link
              href="/settings"
              className={cn(
                "flex flex-1 flex-col items-center gap-0.5 py-2.5 text-[11px]",
                pathname.startsWith("/settings") ? "text-brand-600" : "text-gray-500 hover:text-gray-700"
              )}
            >
              <MessageCircle className="h-5 w-5" />
              Settings
            </Link>
          </div>
        </nav>
      ) : null}
    </div>
  );
}
