"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { ReactNode } from "react";
import {
  LayoutDashboard,
  CalendarDays,
  Users,
  ScrollText,
  Cpu,
  ArrowLeft,
  LogOut
} from "lucide-react";
import { useAuthStore } from "@/lib/state/authStore";

interface NavItem {
  href: string;
  label: string;
  icon: typeof LayoutDashboard;
}

const NAV: NavItem[] = [
  { href: "/admin", label: "Overview", icon: LayoutDashboard },
  { href: "/admin/events", label: "Sessions", icon: CalendarDays },
  { href: "/admin/users", label: "Fellows", icon: Users },
  { href: "/admin/audit", label: "Ledger", icon: ScrollText },
  { href: "/admin/ops", label: "Apparatus", icon: Cpu }
];

export function AdminShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const role = useAuthStore((s) => s.role);
  const user = useAuthStore((s) => s.user);
  const signOut = useAuthStore((s) => s.signOut);

  const isActive = (href: string) =>
    href === "/admin" ? pathname === "/admin" : pathname?.startsWith(href);

  return (
    <div className="flex min-h-screen bg-surface-sunken text-foreground">
      {/* sidebar */}
      <aside className="hidden w-64 shrink-0 flex-col border-r border-border/60 bg-surface md:flex">
        <div className="px-6 py-6 hairline-b">
          <p className="eyebrow text-brass-500">The Registry</p>
          <h1 className="mt-1 font-serif text-2xl">Apparatus</h1>
        </div>
        <nav className="flex-1 px-3 py-4">
          {NAV.map((item) => {
            const Icon = item.icon;
            const active = isActive(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`mb-1 flex items-center gap-3 rounded px-3 py-2 text-sm transition-colors ${
                  active
                    ? "bg-brand-500/10 text-brand-700"
                    : "text-foreground/60 hover:bg-surface-muted hover:text-foreground"
                }`}
              >
                <Icon className="h-4 w-4" strokeWidth={1.4} />
                <span>{item.label}</span>
              </Link>
            );
          })}
        </nav>
        <div className="px-6 py-5 hairline-t">
          {user ? (
            <p className="text-sm text-foreground/80">
              {user.firstName} {user.lastName}
            </p>
          ) : null}
          <p className="eyebrow mt-1 text-brass-500">{role?.toLowerCase().replace("_", " ")}</p>
          <div className="mt-4 flex flex-col gap-2 text-xs">
            <Link
              href="/"
              className="inline-flex items-center gap-1 text-foreground/60 hover:text-foreground"
            >
              <ArrowLeft className="h-3.5 w-3.5" /> Return to participant app
            </Link>
            <button
              type="button"
              onClick={async () => {
                await signOut();
                router.push("/login");
              }}
              className="inline-flex items-center gap-1 text-foreground/60 hover:text-foreground"
            >
              <LogOut className="h-3.5 w-3.5" /> Sign out
            </button>
          </div>
        </div>
      </aside>

      {/* mobile top nav (sidebar collapses below md) */}
      <div className="flex min-h-screen flex-1 flex-col">
        <header className="md:hidden hairline-b bg-surface px-5 py-4">
          <p className="eyebrow text-brass-500">The Registry</p>
          <h1 className="mt-1 font-serif text-lg">Apparatus</h1>
          <nav className="mt-3 flex gap-3 overflow-x-auto pb-1 text-xs">
            {NAV.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className={`whitespace-nowrap rounded-full border px-3 py-1 transition-colors ${
                  isActive(item.href)
                    ? "border-brand-500 bg-brand-500/10 text-brand-700"
                    : "border-border/60 text-foreground/60"
                }`}
              >
                {item.label}
              </Link>
            ))}
          </nav>
        </header>
        <main className="flex-1 px-6 py-6 md:px-10 md:py-10">{children}</main>
      </div>
    </div>
  );
}
