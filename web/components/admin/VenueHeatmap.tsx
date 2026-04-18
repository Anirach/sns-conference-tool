"use client";

import type { AdminHeatmapPoint } from "@/lib/api/admin";

interface VenueHeatmapProps {
  points: AdminHeatmapPoint[];
  centroidLat?: number | null;
  centroidLon?: number | null;
  size?: number;
}

/**
 * Static SVG scatter centred on the venue centroid (or the geometric mean of the points if no
 * centroid is set). Coordinates are projected with a small flat-Earth approximation around the
 * centre — accurate to within a few metres at venue scale, no map tiles needed.
 */
export function VenueHeatmap({ points, centroidLat, centroidLon, size = 320 }: VenueHeatmapProps) {
  if (!points.length) {
    return (
      <div
        className="grid place-items-center hairline rounded-md bg-surface text-sm text-foreground/40"
        style={{ height: size }}
      >
        No location fixes yet
      </div>
    );
  }

  const lats = points.map((p) => p.lat);
  const lons = points.map((p) => p.lon);
  const cLat = centroidLat ?? lats.reduce((a, b) => a + b, 0) / lats.length;
  const cLon = centroidLon ?? lons.reduce((a, b) => a + b, 0) / lons.length;

  // Local linearisation: 1° lat ≈ 111 km; 1° lon ≈ 111 km × cos(lat).
  const metersPerLatDeg = 111_000;
  const metersPerLonDeg = 111_000 * Math.cos((cLat * Math.PI) / 180);

  const offsets = points.map((p) => ({
    x: (p.lon - cLon) * metersPerLonDeg,
    y: (p.lat - cLat) * metersPerLatDeg
  }));
  const maxAbs = Math.max(20, ...offsets.flatMap((o) => [Math.abs(o.x), Math.abs(o.y)]));
  const scale = (size / 2 - 16) / maxAbs;

  const ringMeters = [10, 25, 50];

  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="hairline rounded-md bg-surface">
      {/* Concentric rings */}
      {ringMeters.map((r) => (
        <circle
          key={r}
          cx={size / 2}
          cy={size / 2}
          r={r * scale}
          fill="none"
          stroke="currentColor"
          strokeOpacity={0.08}
        />
      ))}
      {/* Crosshair */}
      <line
        x1={size / 2}
        y1={8}
        x2={size / 2}
        y2={size - 8}
        stroke="currentColor"
        strokeOpacity={0.05}
      />
      <line
        x1={8}
        y1={size / 2}
        x2={size - 8}
        y2={size / 2}
        stroke="currentColor"
        strokeOpacity={0.05}
      />
      {/* Centroid */}
      <circle cx={size / 2} cy={size / 2} r={4} className="fill-brass-500" />
      {/* Participants */}
      {offsets.map((o, i) => (
        <circle
          key={i}
          cx={size / 2 + o.x * scale}
          cy={size / 2 - o.y * scale}
          r={3.5}
          className="fill-brand-500"
          fillOpacity={0.7}
        />
      ))}
      {/* Ring labels */}
      {ringMeters.map((r) => (
        <text
          key={`label-${r}`}
          x={size / 2 + r * scale + 4}
          y={size / 2 + 3}
          fontSize={9}
          className="fill-foreground/40"
        >
          {r} m
        </text>
      ))}
    </svg>
  );
}
