# Production container for the Next.js frontend.
#
# Multi-stage build that produces a small image (~150 MB) running `node server.js` from
# Next's standalone output. Used by the `web-prod` profile in
# infra/docker-compose.dev.yml and by deploy-web.yml when shipping to staging / prod.

# ---- Stage 1: install all deps (incl. dev) so we can run `next build`.
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci --no-audit --no-fund

# ---- Stage 2: build the standalone bundle.
FROM node:22-alpine AS builder
WORKDIR /app
ENV NEXT_TELEMETRY_DISABLED=1
COPY --from=deps /app/node_modules ./node_modules
COPY . .
# Build-time env: the public-prefixed NEXT_PUBLIC_* vars are baked into the bundle.
# Override at build time, e.g. --build-arg NEXT_PUBLIC_MOCK_API=0
ARG NEXT_PUBLIC_MOCK_API=0
ARG NEXT_PUBLIC_API_BASE=/api
ENV NEXT_PUBLIC_MOCK_API=${NEXT_PUBLIC_MOCK_API}
ENV NEXT_PUBLIC_API_BASE=${NEXT_PUBLIC_API_BASE}
RUN npm run build

# ---- Stage 3: minimal runtime.
FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

# Run as a non-root user for safety.
RUN addgroup -S sns && adduser -S sns -G sns

# Copy only the artefacts produced by Next's standalone output.
COPY --from=builder --chown=sns:sns /app/.next/standalone ./
COPY --from=builder --chown=sns:sns /app/.next/static ./.next/static
COPY --from=builder --chown=sns:sns /app/public ./public

USER sns
EXPOSE 3000
CMD ["node", "server.js"]
