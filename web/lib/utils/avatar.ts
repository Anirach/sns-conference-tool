const PALETTE = [
  "hsl(168 38% 22%)",
  "hsl(38 35% 49%)",
  "hsl(168 46% 14%)",
  "hsl(38 42% 28%)",
  "hsl(167 32% 38%)",
  "hsl(38 38% 38%)",
  "hsl(168 38% 8%)"
];

export function colorFromName(name: string): string {
  let h = 0;
  for (let i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) >>> 0;
  return PALETTE[h % PALETTE.length];
}

export function initials(first: string, last: string): string {
  return `${first[0] ?? ""}${last[0] ?? ""}`.toUpperCase();
}

const AVATAR_POOL = [
  "/avatars/alice.jpg",
  "/avatars/ben.jpg",
  "/avatars/carla.jpg",
  "/avatars/dmitri.jpg",
  "/avatars/elin.jpg",
  "/avatars/farid.jpg",
  "/avatars/grace.jpg",
  "/avatars/hiro.jpg",
  "/avatars/jordan.jpg"
];

const AVATAR_OVERRIDES: Record<string, string> = {
  "u-you-0001": "/avatars/jordan.jpg",
  "u-0002": "/avatars/alice.jpg",
  "u-0003": "/avatars/hiro.jpg",
  "u-0004": "/avatars/elin.jpg",
  "u-0005": "/avatars/hiro.jpg",
  "u-0006": "/avatars/carla.jpg",
  "u-0007": "/avatars/farid.jpg",
  "u-0008": "/avatars/grace.jpg",
  "u-0009": "/avatars/ben.jpg",
  "u-0010": "/avatars/elin.jpg",
  "u-0011": "/avatars/farid.jpg",
  "u-0012": "/avatars/grace.jpg",
  "u-0013": "/avatars/dmitri.jpg",
  "u-0014": "/avatars/alice.jpg",
  "u-0015": "/avatars/hiro.jpg",
  "u-0016": "/avatars/carla.jpg",
  "u-0017": "/avatars/ben.jpg",
  "u-0018": "/avatars/grace.jpg",
  "u-0019": "/avatars/dmitri.jpg",
  "u-0020": "/avatars/hiro.jpg"
};

export function portraitUrlFor(seed: string): string {
  const override = AVATAR_OVERRIDES[seed];
  if (override) return override;
  let h = 0;
  for (let i = 0; i < seed.length; i++) h = (h * 31 + seed.charCodeAt(i)) >>> 0;
  return AVATAR_POOL[h % AVATAR_POOL.length];
}
