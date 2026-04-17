import type { ConferenceEvent } from "./types";

const now = Date.now();
const h = 3600 * 1000;
const d = 24 * h;

export const events: ConferenceEvent[] = [
  {
    eventId: "evt-neurips-2026-bkk",
    eventName: "NeurIPS 2026 Bangkok",
    venue: "Queen Sirikit National Convention Center, Bangkok",
    expirationCode: new Date(now + 3 * d).toISOString(),
    qrCode: "NEURIPS2026",
    expired: false
  },
  {
    eventId: "evt-acl-2026-vienna",
    eventName: "ACL 2026 Vienna",
    venue: "Austria Center Vienna",
    expirationCode: new Date(now + 5 * d).toISOString(),
    qrCode: "ACL2026",
    expired: false
  },
  {
    eventId: "evt-icml-2025-montreal",
    eventName: "ICML 2025 Montreal",
    venue: "Palais des Congrès de Montréal",
    expirationCode: new Date(now - 30 * d).toISOString(),
    qrCode: "ICML2025",
    expired: true
  }
];

export const eventsByCode: Record<string, ConferenceEvent> = events.reduce(
  (acc, e) => ({ ...acc, [e.qrCode]: e }),
  {} as Record<string, ConferenceEvent>
);

export function findEvent(eventId: string): ConferenceEvent | undefined {
  return events.find((e) => e.eventId === eventId);
}
