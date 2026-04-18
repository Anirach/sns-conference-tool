"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { adminApi, type AdminUserSummary } from "@/lib/api/admin";
import type { Role } from "@/lib/fixtures/types";
import { AdminTable, type ColumnDef } from "@/components/admin/Table";
import { Input } from "@/components/ui/Input";

export default function AdminUsersListPage() {
  const router = useRouter();
  const [page, setPage] = useState(0);
  const [q, setQ] = useState("");
  const [role, setRole] = useState<Role | "">("");
  const [status, setStatus] = useState<"" | "active" | "suspended" | "deleted">("");

  const { data, isLoading } = useQuery({
    queryKey: ["admin", "users", page, q, role, status],
    queryFn: async () => (await adminApi.users.list(page, 25, q || undefined, role || undefined, status || undefined)).data
  });

  const cols: ColumnDef<AdminUserSummary>[] = [
    {
      key: "name",
      header: "Fellow",
      cell: (r) => (
        <div>
          <p className="font-serif text-base">
            {r.firstName ?? ""} {r.lastName ?? ""}
          </p>
          <p className="text-xs text-foreground/60">{r.email}</p>
        </div>
      )
    },
    { key: "inst", header: "Institution", cell: (r) => r.institution ?? "—" },
    {
      key: "role",
      header: "Role",
      cell: (r) =>
        r.role === "USER" ? (
          <span className="text-xs text-foreground/40">user</span>
        ) : (
          <span className="text-xs text-brand-700">{r.role.toLowerCase().replace("_", " ")}</span>
        )
    },
    {
      key: "status",
      header: "Status",
      cell: (r) =>
        r.deleted ? (
          <span className="text-xs text-red-700">deleted</span>
        ) : r.suspended ? (
          <span className="text-xs text-amber-700">suspended</span>
        ) : (
          <span className="text-xs text-brand-700">active</span>
        )
    },
    {
      key: "joined",
      header: "Joined",
      align: "right",
      cell: (r) => (
        <span className="text-xs tabular-nums text-foreground/60">
          {new Date(r.createdAt).toLocaleDateString()}
        </span>
      )
    }
  ];

  return (
    <div>
      <header className="mb-6">
        <p className="eyebrow text-brass-500">The Roster</p>
        <h2 className="mt-2 font-serif text-3xl">Fellows</h2>
      </header>

      <div className="mb-4 grid gap-3 md:grid-cols-[2fr_1fr_1fr]">
        <Input
          placeholder="Search by email…"
          value={q}
          onChange={(e) => {
            setQ(e.target.value);
            setPage(0);
          }}
        />
        <select
          value={role}
          onChange={(e) => {
            setRole(e.target.value as Role | "");
            setPage(0);
          }}
          className="hairline h-11 rounded-sm bg-card px-3 text-sm"
        >
          <option value="">All roles</option>
          <option value="USER">User</option>
          <option value="ORGANIZER">Organizer</option>
          <option value="ADMIN">Admin</option>
          <option value="SUPER_ADMIN">Super admin</option>
        </select>
        <select
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as "" | "active" | "suspended" | "deleted");
            setPage(0);
          }}
          className="hairline h-11 rounded-sm bg-card px-3 text-sm"
        >
          <option value="">Any status</option>
          <option value="active">Active</option>
          <option value="suspended">Suspended</option>
          <option value="deleted">Deleted</option>
        </select>
      </div>

      <AdminTable
        columns={cols}
        rows={data?.items ?? []}
        total={data?.total ?? 0}
        page={page}
        size={25}
        loading={isLoading}
        rowKey={(r) => r.userId}
        onPageChange={setPage}
        onRowClick={(r) => router.push(`/admin/users/${r.userId}`)}
      />
    </div>
  );
}
