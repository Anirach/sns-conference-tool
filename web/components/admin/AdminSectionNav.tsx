"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils/cn";

interface Section {
  href: string;
  label: string;
}

const SECTIONS: Section[] = [
  { href: "/admin", label: "Overview" },
  { href: "/admin/events", label: "Sessions" },
  { href: "/admin/users", label: "Fellows" },
  { href: "/admin/audit", label: "Ledger" },
  { href: "/admin/ops", label: "Apparatus" }
];

export function AdminSectionNav() {
  const pathname = usePathname();
  const isActive = (href: string) =>
    href === "/admin" ? pathname === "/admin" : pathname?.startsWith(href);

  return (
    <nav className="mb-5 flex gap-2 overflow-x-auto pb-1 hairline-b">
      {SECTIONS.map((s) => (
        <Link
          key={s.href}
          href={s.href}
          className={cn(
            "eyebrow whitespace-nowrap rounded-full border px-3 py-1 transition-colors",
            isActive(s.href)
              ? "border-brass-500 bg-brass-500/10 text-brass-700"
              : "border-border/60 text-foreground/60 hover:text-foreground"
          )}
        >
          {s.label}
        </Link>
      ))}
    </nav>
  );
}
