"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Facebook, Linkedin, Check } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { Button } from "@/components/ui/Button";
import { Chip } from "@/components/ui/Chip";
import { useToast } from "@/components/ui/Toast";
import { profileApi } from "@/lib/api/profile";
import { bridge } from "@/lib/bridge/client";
import type { SnsLink, SnsProvider } from "@/lib/fixtures/types";
import type { SnsLoginResult } from "@/lib/bridge/types";

export default function SnsLinksPage() {
  const qc = useQueryClient();
  const { toast } = useToast();

  const { data: links = [] } = useQuery<SnsLink[]>({
    queryKey: ["sns"],
    queryFn: async () => (await profileApi.listSns()).data
  });

  const linkMut = useMutation({
    mutationFn: async (provider: SnsProvider) => {
      const provLower = provider.toLowerCase() as "facebook" | "linkedin";
      const result = await bridge.call<SnsLoginResult>("sns.login", { provider: provLower });
      return (
        await profileApi.linkSns({
          provider,
          accessToken: result.accessToken,
          providerUserId: result.providerUserId
        })
      ).data;
    },
    onSuccess: (link) => {
      qc.setQueryData<SnsLink[]>(["sns"], (prev = []) => {
        const without = prev.filter((l) => l.provider !== link.provider);
        return [...without, link];
      });
      toast({ title: `Linked ${link.provider.toLowerCase()}`, variant: "success" });
    },
    onError: () => toast({ title: "Linking failed", variant: "error" })
  });

  const unlinkMut = useMutation({
    mutationFn: async (provider: SnsProvider) => {
      await profileApi.unlinkSns(provider);
      return provider;
    },
    onSuccess: (provider) => {
      qc.setQueryData<SnsLink[]>(["sns"], (prev = []) => prev.filter((l) => l.provider !== provider));
      toast({ title: `Unlinked ${provider.toLowerCase()}` });
    }
  });

  const linkedProviders = new Set(links.map((l) => l.provider));

  return (
    <AppShell title="Linked accounts" showBack>
      <p className="mb-4 text-sm text-gray-600">
        Link your social accounts to auto-fill your profile. We only read the scopes we need — see the consent
        screen for details. Linking is optional and reversible.
      </p>
      <div className="flex flex-col gap-3">
        <ProviderCard
          name="Facebook"
          icon={<Facebook className="h-5 w-5" />}
          scopes="public_profile, email"
          linked={linkedProviders.has("FACEBOOK")}
          onLink={() => linkMut.mutate("FACEBOOK")}
          onUnlink={() => unlinkMut.mutate("FACEBOOK")}
          busy={linkMut.isPending || unlinkMut.isPending}
        />
        <ProviderCard
          name="LinkedIn"
          icon={<Linkedin className="h-5 w-5" />}
          scopes="r_liteprofile, r_emailaddress"
          linked={linkedProviders.has("LINKEDIN")}
          onLink={() => linkMut.mutate("LINKEDIN")}
          onUnlink={() => unlinkMut.mutate("LINKEDIN")}
          busy={linkMut.isPending || unlinkMut.isPending}
        />
      </div>
    </AppShell>
  );
}

function ProviderCard({
  name,
  icon,
  scopes,
  linked,
  onLink,
  onUnlink,
  busy
}: {
  name: string;
  icon: React.ReactNode;
  scopes: string;
  linked: boolean;
  onLink: () => void;
  onUnlink: () => void;
  busy: boolean;
}) {
  return (
    <div className="flex items-start gap-3 rounded-2xl border border-gray-200 bg-white p-4">
      <div className="rounded-lg bg-gray-100 p-2 text-gray-700">{icon}</div>
      <div className="flex-1">
        <div className="flex items-center gap-2">
          <div className="font-semibold">{name}</div>
          {linked ? (
            <Chip variant="success" className="gap-1">
              <Check className="h-3 w-3" /> Linked
            </Chip>
          ) : null}
        </div>
        <div className="mt-0.5 text-xs text-gray-500">Scopes: {scopes}</div>
      </div>
      {linked ? (
        <Button size="sm" variant="secondary" onClick={onUnlink} disabled={busy}>
          Unlink
        </Button>
      ) : (
        <Button size="sm" onClick={onLink} disabled={busy}>
          Link
        </Button>
      )}
    </div>
  );
}
