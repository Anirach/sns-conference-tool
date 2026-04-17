import api from "./axios";
import type { ConferenceEvent, Match } from "../fixtures/types";

export interface JoinEventRequest {
  eventCode: string;
}

export interface JoinEventResponse {
  event: ConferenceEvent;
  joinedAt: string;
}

export interface VicinityResponse {
  radius: number;
  matches: Match[];
}

export interface LocationUpdate {
  lat: number;
  lon: number;
  accuracy: number;
  ts: string;
}

export const matchesApi = {
  get: (matchId: string) => api.get<Match>(`/matches/${matchId}`)
};

export const eventsApi = {
  listJoined: () => api.get<ConferenceEvent[]>("/events/joined"),
  get: (eventId: string) => api.get<ConferenceEvent>(`/events/${eventId}`),
  join: (body: JoinEventRequest) => api.post<JoinEventResponse>("/events/join", body),
  leave: (eventId: string) => api.post<{ ok: true }>(`/events/${eventId}/leave`),
  vicinity: (eventId: string, radius: number) =>
    api.get<VicinityResponse>(`/events/${eventId}/vicinity`, { params: { radius } }),
  updateLocation: (eventId: string, body: LocationUpdate) =>
    api.post<{ ok: true }>(`/events/${eventId}/location`, body),
  setRadius: (eventId: string, radius: number) =>
    api.put<{ ok: true }>(`/events/${eventId}/radius`, { radius })
};
