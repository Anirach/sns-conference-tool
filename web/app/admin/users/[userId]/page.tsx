"use client";

import { useParams, useRouter } from "next/navigation";
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { adminApi } from "@/lib/api/admin";
import type { Role } from "@/lib/fixtures/types";
import { AppShell } from "@/components/layout/AppShell";
import { StatCard } from "@/components/admin/StatCard";
import { Button } from "@/components/ui/Button";

export default function AdminUserDossierPage() {
  const { userId } = useParams<{ userId: string }>();
  const router = useRouter();
  const qc = useQueryClient();
  const [pending, setPending] = useState<string | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ["admin", "user", userId],
    queryFn: async () => (await adminApi.users.dossier(userId)).data
  });

  const refresh = () => qc.invalidateQueries({ queryKey: ["admin", "user", userId] });

  const suspend = useMutation({ mutationFn: () => adminApi.users.suspend(userId), onSuccess: refresh });
  const unsuspend = useMutation({ mutationFn: () => adminApi.users.unsuspend(userId), onSuccess: refresh });
  const softDelete = useMutation({ mutationFn: () => adminApi.users.softDelete(userId), onSuccess: refresh });
  const hardDelete = useMutation({
    mutationFn: () => adminApi.users.hardDelete(userId),
    onSuccess: () => router.push("/admin/users")
  });
  const changeRole = useMutation({
    mutationFn: (role: Role) => adminApi.users.changeRole(userId, role),
    onSuccess: refresh
  });

  if (isLoading || !data) {
    return (
      <AppShell title="Fellow" eyebrow="The Registry" showBack>
        <div className="flex-1 px-5 pt-10 text-center">
          <p className="eyebrow text-brass-500">Loading dossier…</p>
        </div>
      </AppShell>
    );
  }

  return (
    <AppShell title="Fellow" eyebrow="The Registry" showBack>
      <div className="flex-1 px-5 pt-6 pb-8">
        <header className="mb-5 hairline-b pb-5">
          <p className="eyebrow text-brass-500">Fellow</p>
          <h2 className="mt-2 font-serif text-3xl leading-tight">
            <span className="italic">
              {data.firstName ?? ""} {data.lastName ?? ""}
            </span>
          </h2>
          <p className="mt-1 font-serif text-sm italic text-muted-foreground">
            {data.email} · {data.academicTitle ?? "—"} · {data.institution ?? "—"}
          </p>
        </header>

        <section className="mb-6 grid grid-cols-2 gap-3">
          <StatCard
            label="Status"
            value={data.deleted ? "deleted" : data.suspended ? "suspended" : "active"}
            accent={data.deleted ? "danger" : data.suspended ? "warn" : "default"}
          />
          <StatCard label="Role" value={data.role.toLowerCase().replace("_", " ")} />
          <StatCard label="Interests" value={data.interests.length} />
          <StatCard label="Affinities" value={data.matchCount} />
          <StatCard label="Messages" value={data.chatMessageCount} />
          <StatCard label="Sessions" value={data.events.length} />
        </section>

        <section className="mb-6">
          <h3 className="eyebrow mb-3 text-brass-500">Role</h3>
          <div className="flex flex-wrap gap-2">
            {(["USER", "ORGANIZER", "ADMIN", "SUPER_ADMIN"] as Role[]).map((r) => (
              <button
                key={r}
                type="button"
                disabled={r === data.role || changeRole.isPending}
                onClick={() => changeRole.mutate(r)}
                className={`eyebrow rounded-full border px-3 py-1 transition-colors ${
                  r === data.role
                    ? "border-brass-500 bg-brass-500/10 text-brass-700"
                    : "border-border/60 text-foreground/60 hover:text-foreground"
                } disabled:opacity-50`}
              >
                {r.toLowerCase().replace("_", " ")}
              </button>
            ))}
          </div>
          {changeRole.isError ? (
            <p className="mt-2 text-xs text-red-700">
              Cannot change role (super admin restricted, or last super admin).
            </p>
          ) : null}
        </section>

        <section className="mb-6">
          <h3 className="eyebrow mb-3 text-brass-500">Interests</h3>
          <ul className="hairline divide-y divide-border/60 rounded-md bg-surface">
            {data.interests.map((i) => (
              <li key={i.interestId} className="px-4 py-3">
                <p className="font-serif text-sm">{i.content}</p>
                <p className="mt-1 text-xs text-foreground/60">
                  {i.keywords.slice(0, 6).join(" · ")}
                </p>
              </li>
            ))}
            {data.interests.length === 0 ? (
              <li className="px-4 py-6 text-center text-sm text-foreground/40">No interests</li>
            ) : null}
          </ul>
        </section>

        <section className="mb-6">
          <h3 className="eyebrow mb-3 text-brass-500">Sessions joined</h3>
          <ul className="hairline divide-y divide-border/60 rounded-md bg-surface">
            {data.events.map((e) => (
              <li key={e.eventId} className="flex items-center justify-between px-4 py-3">
                <span className="font-serif text-sm">{e.name}</span>
                <span className="text-xs tabular-nums text-foreground/60">
                  {e.selectedRadius} m · {new Date(e.joinedAt).toLocaleDateString()}
                </span>
              </li>
            ))}
            {data.events.length === 0 ? (
              <li className="px-4 py-6 text-center text-sm text-foreground/40">No sessions</li>
            ) : null}
          </ul>
        </section>

        <section className="mb-8">
          <h3 className="eyebrow mb-3 text-brass-500">Recent ledger entries</h3>
          <ul className="hairline divide-y divide-border/60 rounded-md bg-surface text-sm">
            {data.recentAudit.map((a) => (
              <li key={a.id} className="flex items-center justify-between px-4 py-2">
                <code className="text-xs">{a.action}</code>
                <span className="text-xs tabular-nums text-foreground/60">
                  {new Date(a.createdAt).toLocaleString()}
                </span>
              </li>
            ))}
            {data.recentAudit.length === 0 ? (
              <li className="px-4 py-6 text-center text-foreground/40">No entries</li>
            ) : null}
          </ul>
        </section>

        <section className="flex flex-col gap-2">
          {data.suspended ? (
            <Button variant="outline" loading={unsuspend.isPending} onClick={() => unsuspend.mutate()} fullWidth>
              Reinstate
            </Button>
          ) : (
            <Button variant="outline" loading={suspend.isPending} onClick={() => suspend.mutate()} fullWidth>
              Suspend
            </Button>
          )}
          {!data.deleted ? (
            <Button variant="outline" loading={softDelete.isPending} onClick={() => softDelete.mutate()} fullWidth>
              Soft delete
            </Button>
          ) : null}
          <Button
            variant="danger"
            loading={hardDelete.isPending}
            onClick={() => {
              if (pending !== "hard") return setPending("hard");
              hardDelete.mutate();
            }}
            fullWidth
          >
            {pending === "hard" ? "Confirm hard delete" : "Hard delete"}
          </Button>
        </section>
      </div>
    </AppShell>
  );
}
