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
