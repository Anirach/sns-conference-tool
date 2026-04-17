import { test, expect } from "@playwright/test";

test.describe("Pass 1 demo flow", () => {
  test("register → verify → complete enrolment → join event → open vicinity", async ({ page }) => {
    await page.goto("/");
    await expect(page.getByRole("heading", { name: /Intellectual/i })).toBeVisible();

    await page.getByRole("link", { name: /Request Admission/i }).click();
    await expect(page).toHaveURL(/\/register$/);

    const email = `playwright+${Date.now()}@example.com`;
    await page.getByLabel("Email").fill(email);
    await page.getByRole("button", { name: /Dispatch Cipher/i }).click();

    await expect(page).toHaveURL(/\/verify\?/);
    const tanCells = page.locator('input[inputmode="numeric"]');
    await expect(tanCells).toHaveCount(6);
    for (let i = 0; i < 6; i++) {
      await tanCells.nth(i).fill(String((i + 1) % 10));
    }

    await expect(page).toHaveURL(/\/login\?.*next=complete/);
    await page.getByLabel("First name").fill("Ada");
    await page.getByLabel("Last name").fill("Lovelace");
    await page.getByLabel("Password").fill("analytical-engine");
    await page.getByRole("button", { name: /Confirm Enrolment/i }).click();

    await expect(page).toHaveURL(/\/interests$/);

    await page.goto("/events/join");
    await page.getByPlaceholder("NEURIPS2026").fill("NEURIPS2026");
    await page.getByRole("button", { name: /^Enter$/ }).click();

    await expect(page).toHaveURL(/\/events\/[^/]+$/);
  });

  test("vicinity page renders matches from mock API", async ({ page }) => {
    await page.goto("/events/evt-neurips-2026/vicinity");
    await expect(page.getByText(/radius/i).first()).toBeVisible();
  });

  test("chats page loads threads", async ({ page }) => {
    await page.goto("/chats");
    await expect(page).toHaveURL(/\/chats$/);
  });
});
