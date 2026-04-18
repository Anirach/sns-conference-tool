"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { adminApi } from "@/lib/api/admin";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";

function defaultExpiry(): string {
  const d = new Date();
  d.setDate(d.getDate() + 3);
  d.setSeconds(0, 0);
  return d.toISOString().slice(0, 16);
}

export default function AdminEventNewPage() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [venue, setVenue] = useState("");
  const [qrCodePlaintext, setQr] = useState("");
  const [expiry, setExpiry] = useState(defaultExpiry());
  const [centroidLat, setLat] = useState<string>("");
  const [centroidLon, setLon] = useState<string>("");
  const [error, setError] = useState<string | null>(null);

  const create = useMutation({
    mutationFn: () =>
      adminApi.events
        .create({
          name,
          venue,
          qrCodePlaintext,
          expirationCode: new Date(expiry).toISOString(),
          centroidLat: centroidLat ? Number(centroidLat) : null,
          centroidLon: centroidLon ? Number(centroidLon) : null
        })
        .then((r) => r.data),
    onSuccess: (created) => router.push(`/admin/events/${created.eventId}`),
    onError: (e: { response?: { status?: number; data?: { detail?: string } } }) => {
      setError(
        e?.response?.data?.detail ??
          (e?.response?.status === 409 ? "QR code already in use" : "Could not create session")
      );
    }
  });

  return (
    <div className="max-w-2xl">
      <header className="mb-6">
        <p className="eyebrow text-brass-500">In residence</p>
        <h2 className="mt-2 font-serif text-3xl">New session</h2>
        <p className="mt-1 font-serif text-sm italic text-muted-foreground">
          Coin a cipher, fix the venue, set the adjournment.
        </p>
      </header>

      <form
        className="flex flex-col gap-4"
        onSubmit={(e) => {
          e.preventDefault();
          setError(null);
          create.mutate();
        }}
      >
        <Input label="Session name" value={name} onChange={(e) => setName(e.target.value)} required />
        <Input label="Venue" value={venue} onChange={(e) => setVenue(e.target.value)} required />
        <Input
          label="Cipher (QR plaintext)"
          value={qrCodePlaintext}
          onChange={(e) => setQr(e.target.value.toUpperCase())}
          hint="Short, all caps. e.g. NEURIPS2027"
          required
        />
        <Input
          label="Adjourns at"
          type="datetime-local"
          value={expiry}
          onChange={(e) => setExpiry(e.target.value)}
          required
        />
        <div className="grid grid-cols-2 gap-4">
          <Input
            label="Centroid latitude"
            type="number"
            step="0.0001"
            value={centroidLat}
            onChange={(e) => setLat(e.target.value)}
            hint="Optional. Used for the heatmap centre."
          />
          <Input
            label="Centroid longitude"
            type="number"
            step="0.0001"
            value={centroidLon}
            onChange={(e) => setLon(e.target.value)}
          />
        </div>

        {error ? <p className="text-sm text-red-700">{error}</p> : null}

        <div className="mt-3 flex gap-3">
          <Button type="submit" variant="primary" loading={create.isPending}>
            Create session
          </Button>
          <Button type="button" variant="ghost" onClick={() => router.back()}>
            Cancel
          </Button>
        </div>
      </form>
    </div>
  );
}
