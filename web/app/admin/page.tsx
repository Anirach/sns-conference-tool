"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { adminApi } from "@/lib/api/admin";
import { AppShell } from "@/components/layout/AppShell";
import { AdminSectionNav } from "@/components/admin/AdminSectionNav";
import { StatCard } from "@/components/admin/StatCard";

export default function AdminOverviewPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin", "metrics"],
    queryFn: async () => (await adminApi.ops.metrics()).data,
    refetchInterval: 30_000
  });

  return (
    <AppShell title="Registry" eyebrow="The Registry">
      <div className="flex-1 px-5 pt-6 pb-8">
        <AdminSectionNav />

        <header className="mb-6 hairline-b pb-5">
          <p className="eyebrow text-brass-500">Vol. I — Issue I</p>
          <h2 className="mt-2 font-serif text-3xl leading-tight">
            <span className="italic">Overview</span>
          </h2>
          <p className="mt-1 font-serif text-sm italic text-muted-foreground">
            The state of the registry, refreshed every half-minute.
          </p>
        </header>

        {isError ? (
          <div className="hairline rounded-md bg-surface px-5 py-4 text-sm text-red-700">
            Could not load metrics.
          </div>
        ) : null}

        <section className="mb-8">
          <h3 className="eyebrow mb-3 text-brass-500">Fellows</h3>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <StatCard label="Total" value={isLoading ? "…" : data?.users.total ?? 0} />
            <StatCard label="Active" value={isLoading ? "…" : data?.users.active ?? 0} />
            <StatCard
              label="Suspended"
              value={isLoading ? "…" : data?.users.suspended ?? 0}
              accent={data && data.users.suspended > 0 ? "warn" : "default"}
            />
            <StatCard label="Deleted (24h)" value={isLoading ? "…" : data?.users.deleted24h ?? 0} />
          </div>
        </section>

        <section className="mb-8">
          <h3 className="eyebrow mb-3 text-brass-500">Sessions</h3>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <StatCard label="Active" value={isLoading ? "…" : data?.events.active ?? 0} />
            <StatCard label="Expired" value={isLoading ? "…" : data?.events.expired ?? 0} />
            <StatCard label="Affinities" value={isLoading ? "…" : data?.matches.total ?? 0} />
            <StatCard
              label="New (24h)"
              value={isLoading ? "…" : data?.matches.created24h ?? 0}
            />
          </div>
        </section>

        <section className="mb-8">
          <h3 className="eyebrow mb-3 text-brass-500">Apparatus</h3>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <StatCard
              label="Outbox pending"
              value={isLoading ? "…" : data?.outbox.pending ?? 0}
              accent={data && data.outbox.pending > 50 ? "warn" : "default"}
            />
            <StatCard
              label="Outbox failed"
              value={isLoading ? "…" : data?.outbox.failed ?? 0}
              accent={data && data.outbox.failed > 0 ? "danger" : "default"}
            />
            <StatCard
              label="Delivered (24h)"
              value={isLoading ? "…" : data?.outbox.delivered24h ?? 0}
            />
            <StatCard label="Audit (24h)" value={isLoading ? "…" : data?.audit24h ?? 0} />
          </div>
        </section>

        <section className="mt-8 grid gap-3 md:grid-cols-2">
          <Link
            href="/admin/events"
            className="hairline rounded-md bg-surface px-5 py-4 transition-colors hover:bg-surface-muted"
          >
            <p className="eyebrow text-brass-500">Sessions</p>
            <p className="mt-1 font-serif text-lg">Manage events &amp; ciphers →</p>
          </Link>
          <Link
            href="/admin/users"
            className="hairline rounded-md bg-surface px-5 py-4 transition-colors hover:bg-surface-muted"
          >
            <p className="eyebrow text-brass-500">Fellows</p>
            <p className="mt-1 font-serif text-lg">Browse &amp; moderate users →</p>
          </Link>
        </section>
      </div>
    </AppShell>
  );
}
