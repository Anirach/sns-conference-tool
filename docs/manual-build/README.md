# `User-Manual.pdf` build pipeline

This folder holds the source artefacts for [`docs/User-Manual.pdf`](../User-Manual.pdf).

## Layout

```
docs/
├── User-Manual.md          # plain-markdown source (web-friendly)
├── User-Manual.pdf         # rendered output (Editorial Ivory style)
├── manual-screenshots/     # captured at iPhone-14-Pro viewport (390×844)
└── manual-build/
    ├── manual.html         # styled HTML source for the PDF
    ├── capture.js          # Playwright script that produces the screenshots
    └── README.md           # this file
```

## Re-running the pipeline

Prerequisites:

- The full dev stack is up (`docker compose -f infra/docker-compose.dev.yml --profile backend --profile web up -d`).
- The seed has run (the demo accounts in §7 of the manual exist; check by hitting `/api/admin/ops/metrics` after a SUPER_ADMIN login).
- Node 22+ on the host, and `weasyprint` available either locally with all GTK deps OR via Docker.

### 1. Capture the screenshots

```bash
cd /tmp
npm i playwright
npx playwright install chromium

# pass the seeded NeurIPS event id so the admin session-detail capture targets a real event
EVID=$(docker exec sns-postgres psql -U conf -d conf -t -A \
  -c "SELECT event_id FROM events WHERE qr_code_plaintext='NEURIPS2026'")
NEURIPS_ID=$EVID node /Users/anirach/Code/SNS/docs/manual-build/capture.js
```

The script logs in as Alice (regular user) for the participant captures, then logs in as Alex (`you@example.com`, super-admin) for the admin captures. Output lands in `docs/manual-screenshots/`.

### 2. Render the PDF

The HTML source uses `display: table` for two-column layouts (weasyprint doesn't support CSS grid) and a `@page cover` rule for the unmarginated cover page.

```bash
mkdir -p /tmp/manual-build/manual-screenshots
cp docs/manual-build/manual.html /tmp/manual-build/manual.html
cp docs/manual-screenshots/*.png /tmp/manual-build/manual-screenshots/
docker run --rm -v /tmp/manual-build:/work -w /work \
  ghcr.io/weasyprint/weasyprint manual.html /work/User-Manual.pdf
cp /tmp/manual-build/User-Manual.pdf docs/User-Manual.pdf
```

The output is ~1.6 MB, 27 pages, A4. The Docker route avoids the macOS `libgobject` dependency
hell of installing weasyprint natively.

## Editing the manual

- **Text changes**: edit [`docs/User-Manual.md`](../User-Manual.md) (the web-readable source)
  AND [`manual.html`](manual.html) (the print source — they're maintained in parallel rather
  than auto-generated, since the HTML has print-specific layout decisions).
- **Adding a new screen**: capture in `capture.js`, then add a `<figure>` reference inside
  a `.split` block in `manual.html`.
- **Restyle**: edit the `<style>` block at the top of `manual.html`. weasyprint supports
  most modern CSS (flex, multi-col, paged-media `@page` and `@top-*` margin boxes) but not
  CSS grid — use `display: table` or floats instead.
