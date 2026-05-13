"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { eventsApi } from "@/lib/api/events";

export type LocationStreamStatus =
  | "idle"
  | "prompting"
  | "granted"
  | "denied"
  | "unsupported"
  | "error";

export interface LocationStreamHandle {
  status: LocationStreamStatus;
  lastFixAt: number | null;
  retry: () => void;
}

interface Options {
  enabled?: boolean;
  /** Heartbeat cadence in ms. Backend throttle drops < 30s AND < 10m, so 30s
   *  is the floor that still bumps `last_update` for a stationary user. */
  heartbeatMs?: number;
}

/**
 * Stream the user's location to `/api/events/{id}/location` while mounted.
 *
 * Why: `VicinityService` filters on `last_position IS NOT NULL` and
 * `last_update > now() - INTERVAL '5 minutes'`. Without this hook running, a
 * real attendee is invisible to every other attendee regardless of radius.
 *
 * Shape: `watchPosition` for fresh fixes + a 30s heartbeat that re-posts the
 * latest known position so a stationary user doesn't age out of the freshness
 * window. The backend's 30s/10m throttle drops redundant fixes silently.
 */
export function useEventLocationStream(
  eventId: string | undefined,
  opts: Options = {}
): LocationStreamHandle {
  const { enabled = true, heartbeatMs = 30_000 } = opts;
  const [status, setStatus] = useState<LocationStreamStatus>("idle");
  const [lastFixAt, setLastFixAt] = useState<number | null>(null);

  const latestFixRef = useRef<GeolocationPosition | null>(null);
  const watchIdRef = useRef<number | null>(null);
  const heartbeatRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const retryNonce = useRef(0);
  const [retryToken, setRetryToken] = useState(0);

  const retry = useCallback(() => {
    retryNonce.current += 1;
    setRetryToken(retryNonce.current);
  }, []);

  useEffect(() => {
    if (!enabled || !eventId) return;

    if (typeof window === "undefined" || !("geolocation" in navigator)) {
      setStatus("unsupported");
      return;
    }

    let cancelled = false;

    const post = (pos: GeolocationPosition) => {
      latestFixRef.current = pos;
      setLastFixAt(Date.now());
      eventsApi
        .updateLocation(eventId, {
          lat: pos.coords.latitude,
          lon: pos.coords.longitude,
          accuracyMeters: Number.isFinite(pos.coords.accuracy) ? pos.coords.accuracy : null
        })
        .catch(() => {
          // Silent — the heartbeat will retry on next tick. Surface errors
          // only if every POST fails (out of scope; not actionable for users).
        });
    };

    const onSuccess = (pos: GeolocationPosition) => {
      if (cancelled) return;
      setStatus("granted");
      post(pos);
    };

    const onError = (err: GeolocationPositionError) => {
      if (cancelled) return;
      if (err.code === err.PERMISSION_DENIED) {
        setStatus("denied");
      } else {
        setStatus("error");
      }
      // Stop the watch on error; heartbeat will skip without a fix.
      if (watchIdRef.current !== null) {
        navigator.geolocation.clearWatch(watchIdRef.current);
        watchIdRef.current = null;
      }
    };

    setStatus("prompting");
    // Initial getCurrentPosition triggers the permission prompt and yields a
    // fix immediately rather than waiting on watchPosition's first cadence.
    navigator.geolocation.getCurrentPosition(onSuccess, onError, {
      enableHighAccuracy: true,
      maximumAge: 10_000,
      timeout: 30_000
    });

    watchIdRef.current = navigator.geolocation.watchPosition(onSuccess, onError, {
      enableHighAccuracy: true,
      maximumAge: 10_000,
      timeout: 30_000
    });

    heartbeatRef.current = setInterval(() => {
      const fix = latestFixRef.current;
      if (!fix) return;
      post(fix);
    }, heartbeatMs);

    return () => {
      cancelled = true;
      if (watchIdRef.current !== null) {
        navigator.geolocation.clearWatch(watchIdRef.current);
        watchIdRef.current = null;
      }
      if (heartbeatRef.current !== null) {
        clearInterval(heartbeatRef.current);
        heartbeatRef.current = null;
      }
    };
  }, [eventId, enabled, heartbeatMs, retryToken]);

  return { status, lastFixAt, retry };
}
