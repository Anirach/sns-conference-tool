/**
 * Thin localStorage wrapper used for auth tokens and user-settings JSON.
 *
 * Why not raw localStorage everywhere? — Two reasons:
 * 1. SSR safety: Next.js server-renders pages where `window` is undefined; this helper
 *    silently no-ops in that environment instead of throwing.
 * 2. Namespacing: every key is prefixed `sns.` so we never clash with anything else
 *    a user's browser might have stashed at the same origin.
 *
 * If we ever decide to move tokens behind a service worker (httpOnly-cookie style) we
 * only have to swap this one module — every caller already goes through it.
 */

const PREFIX = "sns.";

function k(key: string): string {
  return PREFIX + key;
}

export function getItem(key: string): string | null {
  if (typeof window === "undefined") return null;
  try {
    return window.localStorage.getItem(k(key));
  } catch {
    return null;
  }
}

export function setItem(key: string, value: string): void {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(k(key), value);
  } catch {
    /* quota exceeded / private mode — silently drop */
  }
}

export function removeItem(key: string): void {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.removeItem(k(key));
  } catch {
    /* ignore */
  }
}
