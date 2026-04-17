"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Bell, MapPin, Database, Globe, LogOut } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Toggle } from "@/components/ui/Toggle";
import { Button } from "@/components/ui/Button";
import { useToast } from "@/components/ui/Toast";
import { bridge } from "@/lib/bridge/client";
import { useAuthStore } from "@/lib/state/authStore";
import type { UserSettings } from "@/lib/fixtures/types";

const SETTINGS_KEY = "settings";
const DEFAULTS: UserSettings = {
  pushMatches: true,
  pushChat: true,
  gpsConsent: true,
  localStorageOptIn: false,
  language: "en"
};

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
    toast({ title: "Signed out" });
    router.push("/");
  }

  return (
    <AppShell title="Settings">
      <div className="flex flex-col gap-4">
        <SettingGroup title="Notifications" icon={<Bell className="h-4 w-4" />}>
          <Row
            label="New match"
            description="Push notifications when someone with similar interests is nearby."
          >
            <Toggle checked={settings.pushMatches} onCheckedChange={(v) => update("pushMatches", v)} ariaLabel="Match notifications" />
          </Row>
          <Row label="Chat messages" description="Alert me when I get a new message.">
            <Toggle checked={settings.pushChat} onCheckedChange={(v) => update("pushChat", v)} ariaLabel="Chat notifications" />
          </Row>
        </SettingGroup>

        <SettingGroup title="Privacy" icon={<MapPin className="h-4 w-4" />}>
          <Row
            label="Share location during events"
            description="Required for vicinity matching. Deleted automatically when the event ends."
          >
            <Toggle checked={settings.gpsConsent} onCheckedChange={(v) => update("gpsConsent", v)} ariaLabel="GPS consent" />
          </Row>
          <Row
            label="Store matches on this device"
            description="Keep a local copy of people you matched with, so you can revisit after the event."
          >
            <Toggle checked={settings.localStorageOptIn} onCheckedChange={(v) => update("localStorageOptIn", v)} ariaLabel="Local storage opt-in" />
          </Row>
        </SettingGroup>

        <SettingGroup title="Language" icon={<Globe className="h-4 w-4" />}>
          <div className="grid grid-cols-3 gap-2 p-4">
            {(["en", "th", "de"] as const).map((lang) => (
              <button
                key={lang}
                type="button"
                onClick={() => update("language", lang)}
                className={
                  "rounded-xl border px-3 py-2 text-sm transition-colors " +
                  (settings.language === lang
                    ? "border-brand-500 bg-brand-50 text-brand-700"
                    : "border-gray-200 bg-white text-gray-600 hover:border-gray-300")
                }
              >
                {lang === "en" ? "English" : lang === "th" ? "ไทย" : "Deutsch"}
              </button>
            ))}
          </div>
        </SettingGroup>

        <SettingGroup title="Data" icon={<Database className="h-4 w-4" />}>
          <Row
            label="Export my data"
            description="Get a ZIP with your profile, interests, matches, and chats."
          >
            <Button
              size="sm"
              variant="secondary"
              onClick={() => toast({ title: "Export will be emailed (mock)", variant: "success" })}
            >
              Export
            </Button>
          </Row>
          <Row label="Delete my account" description="Removes your data after 30 days.">
            <Button
              size="sm"
              variant="secondary"
              className="text-red-700"
              onClick={() => toast({ title: "Confirm via email (mock)" })}
            >
              Delete
            </Button>
          </Row>
        </SettingGroup>

        <Button variant="secondary" onClick={onSignOut} className="gap-2 text-red-700">
          <LogOut className="h-4 w-4" />
          Sign out
        </Button>
      </div>
    </AppShell>
  );
}

function SettingGroup({ title, icon, children }: { title: string; icon: React.ReactNode; children: React.ReactNode }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-sm font-semibold uppercase tracking-wide text-gray-500">
          {icon}
          {title}
        </CardTitle>
      </CardHeader>
      <CardContent className="divide-y divide-gray-100 p-0">{children}</CardContent>
    </Card>
  );
}

function Row({
  label,
  description,
  children
}: {
  label: string;
  description?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="flex items-center justify-between gap-4 p-4">
      <div className="min-w-0 flex-1">
        <div className="text-sm font-medium text-gray-900">{label}</div>
        {description ? <div className="mt-0.5 text-xs text-gray-500">{description}</div> : null}
      </div>
      {children}
    </div>
  );
}
