"use client";

import { MapPin } from "lucide-react";
import { Button } from "@/components/ui/Button";
import type { LocationStreamStatus } from "@/lib/hooks/useEventLocationStream";

interface Props {
  status: LocationStreamStatus;
  onRetry: () => void;
}

export function LocationPermissionBanner({ status, onRetry }: Props) {
  if (status !== "denied" && status !== "unsupported") return null;

  const supported = status === "denied";
  return (
    <div className="hairline bg-surface-sunken/60 p-4">
      <div className="flex items-start gap-3">
        <div className="grid h-9 w-9 shrink-0 place-items-center text-brass-500 hairline">
          <MapPin className="h-4 w-4" strokeWidth={1.5} />
        </div>
        <div className="min-w-0 flex-1">
          <p className="eyebrow text-brass-500">Location required</p>
          <p className="mt-1 font-serif text-sm text-foreground/80">
            {supported
              ? "Enable location in your browser to see fellows nearby. Without it, this session looks empty."
              : "This browser doesn’t support location. Open the session on a phone or a recent desktop browser to see fellows nearby."}
          </p>
          {supported ? (
            <Button variant="outline" size="sm" onClick={onRetry} className="mt-3">
              Enable location
            </Button>
          ) : null}
        </div>
      </div>
    </div>
  );
}
