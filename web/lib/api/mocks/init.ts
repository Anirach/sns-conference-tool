export async function initMockApi(): Promise<void> {
  if (typeof window === "undefined") return;
  if (process.env.NEXT_PUBLIC_MOCK_API !== "1") return;
  if ((window as unknown as { __mswReady?: boolean }).__mswReady) return;

  const { worker } = await import("./browser");
  await worker.start({
    serviceWorker: { url: "/mockServiceWorker.js" },
    onUnhandledRequest: "bypass",
    quiet: true
  });
  (window as unknown as { __mswReady?: boolean }).__mswReady = true;
}
