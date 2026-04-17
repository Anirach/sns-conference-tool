"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import {
  Bell,
  ChevronRight,
  Database,
  Globe,
  HardDrive,
  Heart,
  Info,
  Link2,
  Lock,
  LogOut,
  RotateCcw,
  User as UserIcon
} from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { UserAvatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Toggle } from "@/components/ui/Toggle";
import { useToast } from "@/components/ui/Toast";
import { bridge } from "@/lib/bridge/client";
import { useAuthStore } from "@/lib/state/authStore";
import { currentUser } from "@/lib/fixtures/users";
import type { UserSettings } from "@/lib/fixtures/types";

const SETTINGS_KEY = "settings";
const DEFAULTS: UserSettings = {
  pushMatches: true,
  pushChat: true,
  gpsConsent: true,
  localStorageOptIn: false,
  language: "en"
};

interface Row {
  icon: React.ReactNode;
  label: string;
  onClick?: () => void;
  trailing?: React.ReactNode;
  danger?: boolean;
}

function Section({ title, rows }: { title: string; rows: Row[] }) {
  return (
    <div>
      <p className="eyebrow mb-3 text-brass-500">{title}</p>
      <div className="bg-card hairline">
        {rows.map((r, i) => (
          <button
            key={i}
            onClick={r.onClick}
            type="button"
            className={`flex w-full items-center gap-3 px-4 py-3.5 text-left transition-colors hover:bg-surface-muted ${
              i > 0 ? "hairline-t" : ""
            } ${r.danger ? "text-danger" : ""}`}
          >
            <span
              className={`grid h-8 w-8 place-items-center hairline ${
                r.danger ? "bg-danger/10 text-danger" : "bg-brand-50 text-brand-500"
              }`}
            >
              {r.icon}
            </span>
            <span className="flex-1 font-serif text-sm text-foreground">{r.label}</span>
            {r.trailing ?? <ChevronRight className="h-4 w-4 text-muted-foreground" strokeWidth={1.5} />}
          </button>
        ))}
      </div>
    </div>
  );
}

export default function SettingsPage() {
  const router = useRouter();
  const { toast } = useToast();
  const signOut = useAuthStore((s) => s.signOut);
  const [settings, setSettings] = useState<UserSettings>(DEFAULTS);

  useEffect(() => {
    bridge
      .call<string | null>("storage.get", { key: SETTINGS_KEY })
      .then((raw) => {
        if (raw) {
          try {
            setSettings({ ...DEFAULTS, ...JSON.parse(raw) });
          } catch {
            /* ignore */
          }
        }
      })
      .catch(() => null);
  }, []);

  function update<K extends keyof UserSettings>(key: K, value: UserSettings[K]) {
    const next = { ...settings, [key]: value };
    setSettings(next);
    bridge.call("storage.set", { key: SETTINGS_KEY, value: JSON.stringify(next) }).catch(() => null);
  }

  async function onSignOut() {
    await signOut();
    toast({ title: "Adjourned" });
    router.push("/");
  }

  return (
    <AppShell title="Study" eyebrow="Settings">
      <div className="flex-1 space-y-6 px-5 pt-5 pb-8">
        <button
          type="button"
          onClick={() => router.push("/profile")}
          className="flex w-full items-center gap-4 bg-card p-4 text-left hairline hover:bg-surface-muted"
        >
          <UserAvatar
            firstName={currentUser.firstName}
            lastName={currentUser.lastName}
            src={currentUser.profilePictureUrl}
            size={56}
          />
          <div className="min-w-0 flex-1">
            <p className="eyebrow text-brass-500">{currentUser.academicTitle || "Fellow"}</p>
            <p className="mt-1 truncate font-serif text-base leading-tight text-foreground">
              {currentUser.firstName} {currentUser.lastName}
            </p>
            <p className="truncate font-serif text-xs italic text-muted-foreground">
              {currentUser.institution}
            </p>
          </div>
          <span className="eyebrow text-brass-500">Edit</span>
        </button>

        <Section
          title="Account"
          rows={[
            {
              icon: <UserIcon className="h-4 w-4" strokeWidth={1.5} />,
              label: "Edit particulars",
              onClick: () => router.push("/profile")
            },
            {
              icon: <Heart className="h-4 w-4" strokeWidth={1.5} />,
              label: "Edit inquiries",
              onClick: () => router.push("/interests")
            },
            {
              icon: <Link2 className="h-4 w-4" strokeWidth={1.5} />,
              label: "Linked societies",
              onClick: () => router.push("/profile/sns")
            }
          ]}
        />

        <div>
          <p className="eyebrow mb-3 text-brass-500">Correspondence</p>
          <div className="bg-card hairline">
            <Row
              label="Letters from new fellows"
              description="Push when someone matches with you."
              trailing={
                <Toggle
                  checked={settings.pushMatches}
                  onCheckedChange={(v) => update("pushMatches", v)}
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
                  checked={settings.pushChat}
                  onCheckedChange={(v) => update("pushChat", v)}
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
                  checked={settings.gpsConsent}
                  onCheckedChange={(v) => update("gpsConsent", v)}
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
                  checked={settings.localStorageOptIn}
                  onCheckedChange={(v) => update("localStorageOptIn", v)}
                  ariaLabel="Local storage opt-in"
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
                onClick={() => update("language", lang)}
                className={`rounded-sm bg-card py-2.5 font-serif text-sm transition-colors hairline ${
                  settings.language === lang
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
            {
              icon: <HardDrive className="h-4 w-4" strokeWidth={1.5} />,
              label: "Export my dossier",
              onClick: () => toast({ title: "Export will be posted (mock)" })
            },
            {
              icon: <Database className="h-4 w-4" strokeWidth={1.5} />,
              label: "Local register",
              onClick: () => toast({ title: "Opened (mock)" })
            },
            {
              icon: <Bell className="h-4 w-4" strokeWidth={1.5} />,
              label: "Notifications",
              onClick: () => toast({ title: "Already here" })
            }
          ]}
        />

        <Section
          title="About"
          rows={[
            {
              icon: <Info className="h-4 w-4" strokeWidth={1.5} />,
              label: "Version v0.1.0",
              trailing: <span />
            },
            {
              icon: <Lock className="h-4 w-4" strokeWidth={1.5} />,
              label: "Privacy policy",
              onClick: () => toast({ title: "Demo only" })
            },
            {
              icon: <Globe className="h-4 w-4" strokeWidth={1.5} />,
              label: "Reset demo data",
              onClick: () =>
                typeof window !== "undefined" ? window.location.reload() : toast({ title: "Reloaded" })
            },
            {
              icon: <RotateCcw className="h-4 w-4" strokeWidth={1.5} />,
              label: "Adjourn account",
              danger: true,
              onClick: onSignOut,
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
