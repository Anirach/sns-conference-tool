import api from "./axios";
import type { Role, UUID } from "../fixtures/types";

// ---------------- shared ----------------

export interface AdminPage<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

// ---------------- events ----------------

export interface AdminEventSummary {
  eventId: UUID;
  name: string;
  venue: string;
  qrCode: string;
  expirationCode: string;
  expired: boolean;
  participantCount: number;
}

export interface AdminEventDetail extends AdminEventSummary {
  centroidLat: number | null;
  centroidLon: number | null;
  matchCount: number;
  messageCount: number;
}

export interface AdminEventCreate {
  name: string;
  venue: string;
  qrCodePlaintext: string;
  expirationCode: string;
  centroidLat?: number | null;
  centroidLon?: number | null;
}

export interface AdminEventUpdate {
  name: string;
  venue: string;
  expirationCode: string;
  centroidLat?: number | null;
  centroidLon?: number | null;
}

export interface AdminParticipant {
  userId: UUID;
  firstName: string | null;
  lastName: string | null;
  institution: string | null;
  lastLat: number | null;
  lastLon: number | null;
  lastUpdate: string | null;
  selectedRadius: number;
}

export interface AdminHeatmapPoint {
  lat: number;
  lon: number;
  lastUpdate: string;
}

// ---------------- users ----------------

export interface AdminUserSummary {
  userId: UUID;
  email: string;
  firstName: string | null;
  lastName: string | null;
  institution: string | null;
  role: Role;
  suspended: boolean;
  deleted: boolean;
  createdAt: string;
}

export interface AdminInterestSummary {
  interestId: UUID;
  type: "TEXT" | "ARTICLE_LOCAL" | "ARTICLE_LINK";
  content: string;
  keywords: string[];
  createdAt: string;
}

export interface AdminJoinedEvent {
  eventId: UUID;
  name: string;
  joinedAt: string;
  selectedRadius: number;
}

export interface AdminAuditEntry {
  id: UUID;
  actorUserId: UUID | null;
  action: string;
  resourceType: string | null;
  resourceId: string | null;
  payload: string | null;
  createdAt: string;
}

export interface AdminUserDossier {
  userId: UUID;
  email: string;
  role: Role;
  suspended: boolean;
  deleted: boolean;
  createdAt: string;
  suspendedAt: string | null;
  deletedAt: string | null;
  firstName: string | null;
  lastName: string | null;
  academicTitle: string | null;
  institution: string | null;
  profilePictureUrl: string | null;
  interests: AdminInterestSummary[];
  events: AdminJoinedEvent[];
  matchCount: number;
  chatMessageCount: number;
  deviceCount: number;
  snsLinkCount: number;
  recentAudit: AdminAuditEntry[];
}

// ---------------- ops ----------------

export interface AdminOutboxRow {
  outboxId: UUID;
  userId: UUID;
  kind: string;
  status: "PENDING" | "DELIVERED" | "FAILED";
  attempts: number;
  lastError: string | null;
  createdAt: string;
  deliveredAt: string | null;
}

export interface AdminOpsMetrics {
  users: { total: number; active: number; suspended: number; deleted24h: number };
  events: { active: number; expired: number };
  outbox: { pending: number; failed: number; delivered24h: number };
  matches: { total: number; created24h: number };
  audit24h: number;
}

// ---------------- API surface ----------------

const qs = (params: Record<string, string | number | undefined | null>) => {
  const usp = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== "") usp.set(k, String(v));
  }
  return usp.toString();
};

export const adminApi = {
  events: {
    list: (page = 0, size = 20, q?: string) =>
      api.get<AdminPage<AdminEventSummary>>(`/admin/events?${qs({ page, size, q })}`),
    get: (eventId: UUID) => api.get<AdminEventDetail>(`/admin/events/${eventId}`),
    create: (body: AdminEventCreate) => api.post<AdminEventDetail>(`/admin/events`, body),
    update: (eventId: UUID, body: AdminEventUpdate) => api.put<AdminEventDetail>(`/admin/events/${eventId}`, body),
    delete: (eventId: UUID) => api.delete<void>(`/admin/events/${eventId}`),
    participants: (eventId: UUID, page = 0, size = 50) =>
      api.get<AdminPage<AdminParticipant>>(`/admin/events/${eventId}/participants?${qs({ page, size })}`),
    heatmap: (eventId: UUID) => api.get<AdminHeatmapPoint[]>(`/admin/events/${eventId}/heatmap`)
  },
  users: {
    list: (page = 0, size = 25, q?: string, role?: Role | "", status?: string) =>
      api.get<AdminPage<AdminUserSummary>>(`/admin/users?${qs({ page, size, q, role, status })}`),
    dossier: (userId: UUID) => api.get<AdminUserDossier>(`/admin/users/${userId}`),
    suspend: (userId: UUID) => api.post<void>(`/admin/users/${userId}/suspend`),
    unsuspend: (userId: UUID) => api.post<void>(`/admin/users/${userId}/unsuspend`),
    changeRole: (userId: UUID, role: Role) => api.post<void>(`/admin/users/${userId}/role`, { role }),
    softDelete: (userId: UUID) => api.delete<void>(`/admin/users/${userId}`),
    hardDelete: (userId: UUID) => api.delete<void>(`/admin/users/${userId}?hard=true`)
  },
  audit: {
    search: (page = 0, size = 50, actor?: UUID, action?: string, since?: string, until?: string) =>
      api.get<AdminPage<AdminAuditEntry>>(`/admin/audit?${qs({ page, size, actor, action, since, until })}`)
  },
  ops: {
    outbox: (page = 0, size = 50, status?: "PENDING" | "DELIVERED" | "FAILED" | "") =>
      api.get<AdminPage<AdminOutboxRow>>(`/admin/ops/outbox?${qs({ page, size, status })}`),
    retry: (outboxId: UUID) => api.post<void>(`/admin/ops/outbox/${outboxId}/retry`),
    metrics: () => api.get<AdminOpsMetrics>(`/admin/ops/metrics`)
  }
};
