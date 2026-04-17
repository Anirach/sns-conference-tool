"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Check, Facebook, Linkedin } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { Button } from "@/components/ui/Button";
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
    <AppShell title="Linked Societies" eyebrow="Correspondence" showBack>
      <div className="flex-1 px-5 pt-5 pb-8">
        <header className="mb-5 hairline-b pb-5">
          <p className="eyebrow text-brass-500">Nota Bene</p>
          <h2 className="mt-2 font-serif text-lg leading-snug text-foreground">
            Link a society to auto-fill your <span className="italic">particulars</span>.
          </h2>
          <p className="mt-1 font-serif text-xs italic text-muted-foreground">
            We only read the scopes we need. Linking is optional and reversible.
          </p>
        </header>

        <div>
          <ProviderRow
            name="Facebook"
            icon={<Facebook className="h-4 w-4" strokeWidth={1.5} />}
            scopes="public_profile · email"
            linked={linkedProviders.has("FACEBOOK")}
            onLink={() => linkMut.mutate("FACEBOOK")}
            onUnlink={() => unlinkMut.mutate("FACEBOOK")}
            busy={linkMut.isPending || unlinkMut.isPending}
          />
          <ProviderRow
            name="LinkedIn"
            icon={<Linkedin className="h-4 w-4" strokeWidth={1.5} />}
            scopes="r_liteprofile · r_emailaddress"
            linked={linkedProviders.has("LINKEDIN")}
            onLink={() => linkMut.mutate("LINKEDIN")}
            onUnlink={() => unlinkMut.mutate("LINKEDIN")}
            busy={linkMut.isPending || unlinkMut.isPending}
          />
        </div>
      </div>
    </AppShell>
  );
}

function ProviderRow({
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
    <div className="flex items-start gap-3 py-4 hairline-b">
      <span className="grid h-9 w-9 place-items-center bg-brand-50 text-brand-500 hairline">{icon}</span>
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <p className="font-serif text-base text-foreground">{name}</p>
          {linked ? (
            <span className="eyebrow inline-flex items-center gap-1 text-brass-600">
              <Check className="h-3 w-3" strokeWidth={1.5} /> Linked
            </span>
          ) : null}
        </div>
        <p className="mt-0.5 font-serif text-xs italic text-muted-foreground">{scopes}</p>
      </div>
      {linked ? (
        <Button size="sm" variant="outline" onClick={onUnlink} disabled={busy}>
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
