import { defineConfig, devices } from "@playwright/test";

const PORT = Number(process.env.PORT ?? 3000);
const BASE_URL = process.env.BASE_URL ?? `http://localhost:${PORT}`;

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI ? [["github"], ["html", { open: "never" }]] : "list",
  use: {
    baseURL: BASE_URL,
    trace: "on-first-retry",
    video: "retain-on-failure"
  },
  projects: [
    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
        viewport: { width: 430, height: 932 }
      }
    }
  ],
  webServer: process.env.PLAYWRIGHT_SKIP_WEBSERVER
    ? undefined
    : {
        command: "pnpm dev",
        url: BASE_URL,
        reuseExistingServer: !process.env.CI,
        stdout: "pipe",
        stderr: "pipe",
        timeout: 120_000,
        env: {
          NEXT_PUBLIC_MOCK_API: "1"
        }
      }
});
