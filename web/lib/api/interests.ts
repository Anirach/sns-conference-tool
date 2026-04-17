import api from "./axios";
import type { Interest, InterestType } from "../fixtures/types";

export interface InterestCreate {
  type: InterestType;
  content: string;
}

export const interestsApi = {
  list: () => api.get<Interest[]>("/interests"),
  create: (body: InterestCreate) => api.post<Interest>("/interests", body),
  delete: (id: string) => api.delete<{ ok: true }>(`/interests/${id}`)
};
