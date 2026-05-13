"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import Link from "next/link";
import {
  Bell,
  ChevronRight,
  Database,
  HardDrive,
  Heart,
  Info,
  Link2,
  Lock,
  LogOut,
  RotateCcw,
  Trash2,
  User as UserIcon
} from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { UserAvatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Toggle } from "@/components/ui/Toggle";
import { useToast } from "@/components/ui/Toast";
import { accountApi } from "@/lib/api/account";
import { adminApi } from "@/lib/api/admin";
import { authApi } from "@/lib/api/auth";
import { exportApi } from "@/lib/api/export";
import { profileApi } from "@/lib/api/profile";
import { settingsApi, type UserSettings } from "@/lib/api/settings";
import { useAuthStore, useIsAdmin } from "@/lib/state/authStore";

interface Row {
  icon: React.ReactNode;
  label: string;
  href?: string;
  onClick?: () => void;
  trailing?: React.ReactNode;
  danger?: boolean;
}

function Section({ title, rows }: { title: string; rows: Row[] }) {
  return (
    <div>
      <p className="eyebrow mb-3 text-brass-500">{title}</p>
      <div className="bg-card hairline">
        {rows.map((r, i) => {
          const content = (
            <>
              <span
                className={`grid h-8 w-8 place-items-center hairline ${
                  r.danger ? "bg-danger/10 text-danger" : "bg-brand-50 text-brand-500"
                }`}
              >
                {r.icon}
              </span>
              <span className="flex-1 font-serif text-sm text-foreground">{r.label}</span>
              {r.trailing ?? <ChevronRight className="h-4 w-4 text-muted-foreground" strokeWidth={1.5} />}
            </>
          );
          const cls = `flex w-full items-center gap-3 px-4 py-3.5 text-left transition-colors hover:bg-surface-muted ${
            i > 0 ? "hairline-t" : ""
          } ${r.danger ? "text-danger" : ""}`;
          if (r.href) {
            return (
              <Link key={i} href={r.href} className={cls}>
                {content}
              </Link>
            );
          }
          return (
            <button key={i} onClick={r.onClick} type="button" className={cls}>
              {content}
            </button>
          );
        })}
      </div>
    </div>
  );
}

export default function SettingsPage() {
  const router = useRouter();
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const tokens = useAuthStore((s) => s.tokens);
  const signOut = useAuthStore((s) => s.signOut);
  const isAdmin = useIsAdmin();

  const { data: profile, isLoading: profileLoading } = useQuery({
    queryKey: ["profile"],
    queryFn: async () => (await profileApi.get()).data
  });

  const { data: settings } = useQuery({
    queryKey: ["profile", "settings"],
    queryFn: async () => (await settingsApi.get()).data
  });

  const updateSettings = useMutation({
    mutationFn: (patch: Partial<UserSettings>) => settingsApi.update(patch),
    onMutate: async (patch) => {
      // Optimistic — toggles feel instant; rollback on failure.
      await queryClient.cancelQueries({ queryKey: ["profile", "settings"] });
      const prev = queryClient.getQueryData<UserSettings>(["profile", "settings"]);
      if (prev) queryClient.setQueryData<UserSettings>(["profile", "settings"], { ...prev, ...patch });
      return { prev };
    },
    onError: (_e, _patch, ctx) => {
      if (ctx?.prev) queryClient.setQueryData(["profile", "settings"], ctx.prev);
      toast({ title: "Couldn’t save preference" });
    },
    onSuccess: (res) => queryClient.setQueryData(["profile", "settings"], res.data)
  });

  function set<K extends keyof UserSettings>(key: K, value: UserSettings[K]) {
    updateSettings.mutate({ [key]: value } as Partial<UserSettings>);
  }

  async function onSignOut() {
    try {
      if (tokens?.refreshToken) {
        await authApi.logout(tokens.refreshToken);
      }
    } catch {
      // Backend may already have revoked the token — sign out locally regardless.
    }
    signOut();
    toast({ title: "Adjourned" });
    router.push("/");
  }

  async function onExport() {
    try {
      const res = await exportApi.download();
      const blob = res.data instanceof Blob ? res.data : new Blob([res.data]);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `sns-dossier-${new Date().toISOString().slice(0, 10)}.zip`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch {
      toast({ title: "Export failed" });
    }
  }

  async function onSoftDelete() {
    if (!confirm("Delete your account? Your data is removed after 30 days.")) return;
    try {
      await accountApi.softDelete();
      signOut();
      toast({ title: "Account scheduled for deletion" });
      router.push("/");
    } catch {
      toast({ title: "Couldn’t delete account" });
    }
  }

  async function onResetDemo() {
    if (!confirm("Reset demo data? Every demo fellow’s profile, interests, matches, and chats will be re-seeded from scratch.")) {
      return;
    }
    try {
      await adminApi.dev.resetDemo();
      toast({ title: "Demo data reset" });
      // The actor’s row was just deleted and re-seeded; sign out so the next login mints
      // a fresh JWT under the new user_id.
      signOut();
      router.push("/login");
    } catch {
      toast({ title: "Reset unavailable (prod mode)" });
    }
  }

  const s = settings ?? { pushMatches: true, pushChat: true, gpsConsent: true, keepRegister: false, language: "en" as const };

  return (
    <AppShell title="Study" eyebrow="Settings">
      <div className="flex-1 space-y-6 px-5 pt-5 pb-8">
        <button
          type="button"
          onClick={() => router.push("/profile")}
          className="flex w-full items-center gap-4 bg-card p-4 text-left hairline hover:bg-surface-muted"
        >
          <UserAvatar
            firstName={profile?.firstName ?? ""}
            lastName={profile?.lastName ?? ""}
            src={profile?.profilePictureUrl}
            size={56}
          />
          <div className="min-w-0 flex-1">
            <p className="eyebrow text-brass-500">{profile?.academicTitle || "Fellow"}</p>
            <p className="mt-1 truncate font-serif text-base leading-tight text-foreground">
              {profileLoading ? "Loading…" : `${profile?.firstName ?? ""} ${profile?.lastName ?? ""}`.trim() || "Your profile"}
            </p>
            <p className="truncate font-serif text-xs italic text-muted-foreground">
              {profile?.institution || profile?.email || ""}
            </p>
          </div>
          <span className="eyebrow text-brass-500">Edit</span>
        </button>

        <Section
          title="Account"
          rows={[
            { icon: <UserIcon className="h-4 w-4" strokeWidth={1.5} />, label: "Edit particulars", href: "/profile" },
            { icon: <Heart className="h-4 w-4" strokeWidth={1.5} />, label: "Edit inquiries", href: "/interests" },
            { icon: <Link2 className="h-4 w-4" strokeWidth={1.5} />, label: "Linked societies", href: "/profile/sns" }
          ]}
        />

        <div id="correspondence">
          <p className="eyebrow mb-3 text-brass-500">Correspondence</p>
          <div className="bg-card hairline">
            <Row
              label="Letters from new fellows"
              description="Push when someone matches with you."
              trailing={
                <Toggle
                  checked={s.pushMatches}
                  onCheckedChange={(v) => set("pushMatches", v)}
                  ariaLabel="Match notifications"
                />
              }
            />
            <Row
              divider
              label="New correspondence"
              description="Push when someone writes to you."
              trailing={
                <Toggle
                  checked={s.pushChat}
                  onCheckedChange={(v) => set("pushChat", v)}
                  ariaLabel="Chat notifications"
                />
              }
            />
          </div>
        </div>

        <div>
          <p className="eyebrow mb-3 text-brass-500">Privacy</p>
          <div className="bg-card hairline">
            <Row
              label="Share location during sessions"
              description="Required for proximity matching. Deleted when the session adjourns."
              trailing={
                <Toggle
                  checked={s.gpsConsent}
                  onCheckedChange={(v) => set("gpsConsent", v)}
                  ariaLabel="GPS consent"
                />
              }
            />
            <Row
              divider
              label="Keep a personal register"
              description="Store matches on this device to revisit after the session."
              trailing={
                <Toggle
                  checked={s.keepRegister}
                  onCheckedChange={(v) => set("keepRegister", v)}
                  ariaLabel="Local register opt-in"
                />
              }
            />
          </div>
        </div>

        <div>
          <p className="eyebrow mb-3 text-brass-500">Language</p>
          <div className="grid grid-cols-3 gap-2">
            {(["en", "th", "de"] as const).map((lang) => (
              <button
                key={lang}
                type="button"
                onClick={() => set("language", lang)}
                className={`rounded-sm bg-card py-2.5 font-serif text-sm transition-colors hairline ${
                  s.language === lang
                    ? "border-brass-500 bg-brass-50 text-brass-700"
                    : "text-foreground/60 hover:text-foreground"
                }`}
              >
                {lang === "en" ? "English" : lang === "th" ? "ไทย" : "Deutsch"}
              </button>
            ))}
          </div>
        </div>

        <Section
          title="Data"
          rows={[
            { icon: <HardDrive className="h-4 w-4" strokeWidth={1.5} />, label: "Export my dossier", onClick: onExport },
            { icon: <Database className="h-4 w-4" strokeWidth={1.5} />, label: "Local register", href: "/me/register" },
            { icon: <Bell className="h-4 w-4" strokeWidth={1.5} />, label: "Notifications", href: "#correspondence" }
          ]}
        />

        <Section
          title="About"
          rows={[
            { icon: <Info className="h-4 w-4" strokeWidth={1.5} />, label: "Version v0.1.0", trailing: <span /> },
            { icon: <Lock className="h-4 w-4" strokeWidth={1.5} />, label: "Privacy policy", href: "/privacy" },
            ...(isAdmin
              ? [
                  {
                    icon: <RotateCcw className="h-4 w-4" strokeWidth={1.5} />,
                    label: "Reset demo data",
                    onClick: onResetDemo
                  } as Row
                ]
              : []),
            {
              icon: <Trash2 className="h-4 w-4" strokeWidth={1.5} />,
              label: "Delete my account",
              danger: true,
              onClick: onSoftDelete,
              trailing: <span />
            }
          ]}
        />

        <Button variant="outline" onClick={onSignOut} className="gap-2 text-danger" fullWidth>
          <LogOut className="h-4 w-4" strokeWidth={1.5} />
          Sign out
        </Button>
      </div>
    </AppShell>
  );
}

function Row({
  label,
  description,
  trailing,
  divider
}: {
  label: string;
  description?: string;
  trailing?: React.ReactNode;
  divider?: boolean;
}) {
  return (
    <div className={`flex items-center gap-4 px-4 py-3.5 ${divider ? "hairline-t" : ""}`}>
      <div className="min-w-0 flex-1">
        <p className="font-serif text-sm text-foreground">{label}</p>
        {description ? (
          <p className="mt-0.5 font-serif text-xs italic text-muted-foreground">{description}</p>
        ) : null}
      </div>
      {trailing}
    </div>
  );
}
