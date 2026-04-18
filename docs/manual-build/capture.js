// Capture every screen of the SNS app for the User Manual PDF.
// Re-uses in-app navigation (Next Link clicks) post-login so the Zustand auth state
// stays in memory and the BottomTabBar shows the Registry tab for admins.
const { chromium } = require('playwright');
const path = require('path');

const OUT = '/Users/anirach/Code/SNS/docs/manual-screenshots';
const BASE = 'http://localhost:3000';
const VIEWPORT = { width: 390, height: 844 };
const ADMIN = { email: 'you@example.com', password: 'Demo!2026' };
const USER  = { email: 'alice.smith@mit.edu', password: 'Demo!2026' };
const NEURIPS_ID = process.env.NEURIPS_ID;

async function shot(page, name) {
  const file = path.join(OUT, `${name}.png`);
  await page.screenshot({ path: file, fullPage: true });
  console.log('  →', file);
}

async function login(page, creds) {
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await page.fill('input[name="email"]', creds.email);
  await page.fill('input[name="password"]', creds.password);
  await page.click('button[type="submit"]');
  await page.waitForURL(/\/events\/join/, { timeout: 10_000 });
  await page.waitForTimeout(800);
}

async function clickLink(page, href) {
  // Click the first <a href={href}> on the page — keeps Zustand state alive.
  await page.locator(`a[href="${href}"]`).first().click();
  await page.waitForLoadState('networkidle').catch(() => {});
  await page.waitForTimeout(800);
}

async function clearStorage(page) {
  await page.evaluate(() => { localStorage.clear(); sessionStorage.clear(); });
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: VIEWPORT });
  const page = await context.newPage();

  console.log('## Public ##');
  await page.goto(`${BASE}/`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(400);
  await shot(page, '01-welcome');

  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(400);
  await shot(page, '02-login');

  await page.goto(`${BASE}/register`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(400);
  await shot(page, '03-register');

  console.log('## Participant (Alice) ##');
  await login(page, USER);
  await shot(page, '10-discover');

  // Alice is already a NeurIPS participant. Drill into the vicinity page directly.
  if (NEURIPS_ID) {
    await page.goto(`${BASE}/events/${NEURIPS_ID}/vicinity`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(2500);
    await shot(page, '11-fellows-live');
  }
  // Also capture the historical /matches index.
  await page.goto(`${BASE}/matches`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1500);
  await shot(page, '11b-matches-history');

  // Letters — Alice has a thread with Alex (the seeded chat).
  await page.goto(`${BASE}/chats`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1500);
  await shot(page, '12-letters');

  // Open the first thread.
  const firstThread = page.locator('a[href*="/events/"][href*="/chat/"]').first();
  if (await firstThread.count()) {
    await firstThread.click();
    await page.waitForTimeout(1500);
    await shot(page, '12b-chat');
  }

  await page.goto(`${BASE}/profile`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);
  await shot(page, '13-profile');

  await page.goto(`${BASE}/interests`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);
  await shot(page, '14-interests');

  await page.goto(`${BASE}/settings`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);
  await shot(page, '15-settings');

  await clearStorage(page);

  console.log('## Admin (Alex) ##');
  await login(page, ADMIN);
  await shot(page, '20-discover-admin');

  // Use in-app link clicks so the in-memory role stays alive — the BottomTabBar reads
  // useIsAdmin() from the Zustand store; a hard page.goto would empty it on this fresh tab.
  await clickLink(page, '/admin');
  await page.waitForTimeout(2000);
  await shot(page, '30-admin-overview');

  await clickLink(page, '/admin/events');
  await page.waitForTimeout(1500);
  await shot(page, '31-admin-sessions');

  if (NEURIPS_ID) {
    // Click the NeurIPS row.
    await page.locator('tr', { hasText: 'NeurIPS 2026 Bangkok' }).first().click().catch(() => {});
    await page.waitForTimeout(2500);
    await shot(page, '32-admin-session-detail');
  }

  // Session detail has no AdminSectionNav (it's a showBack subpage). Use page.goto —
  // /admin/layout still calls hydrate() so the bottom-tab Registry will appear.
  await page.goto(`${BASE}/admin/events/new`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1200);
  await shot(page, '33-admin-session-new');

  await page.goto(`${BASE}/admin/users`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1500);
  await shot(page, '34-admin-fellows');

  // Click the first row → fellow dossier.
  const firstRow = page.locator('table tbody tr').first();
  if (await firstRow.count()) {
    await firstRow.click();
    await page.waitForTimeout(2200);
    await shot(page, '35-admin-fellow-dossier');
  }

  await page.goto(`${BASE}/admin/audit`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1500);
  await shot(page, '36-admin-ledger');

  await page.goto(`${BASE}/admin/ops`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1500);
  await shot(page, '37-admin-apparatus');

  // Bonus: participant home with the Registry tab visible. Click Discover bottom tab from
  // /admin/ops so we stay in the Next router (preserves in-memory role state).
  await clickLink(page, '/events/join');
  await page.waitForTimeout(1500);
  await shot(page, '40-admin-on-participant-home');

  await browser.close();
  console.log('done.');
})().catch((e) => { console.error(e); process.exit(1); });
