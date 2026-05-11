/**
 * Registers the offline-shell service worker at /sw.js.
 *
 * Called from `web/app/providers.tsx` after `initMockApi()` resolves with **zero** active
 * MSW domains. Two service workers can't share the same scope, so when MSW is enabled it
 * keeps its slot and we don't try to install the PWA shell.
 *
 * On secure-context environments (https or localhost) this enables Add-to-Home-Screen on
 * Android Chrome and lets iOS Safari treat the page as a standalone app once the user
 * taps Share → Add to Home Screen.
 */
export async function registerPwa(): Promise<void> {
  if (typeof navigator === "undefined" || !("serviceWorker" in navigator)) return;
  // Skip in dev unless explicitly opted in — Next dev's HMR conflicts with cached chunks.
  if (process.env.NODE_ENV !== "production" && process.env.NEXT_PUBLIC_PWA_DEV !== "1") return;
  try {
    await navigator.serviceWorker.register("/sw.js", { scope: "/" });
  } catch (err) {
    console.warn("[pwa] service worker registration failed", err);
  }
}
