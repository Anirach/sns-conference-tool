import api from "./axios";
import type { User, SnsLink } from "../fixtures/types";

export interface ProfileUpdate {
  firstName: string;
  lastName: string;
  academicTitle: string;
  institution: string;
  profilePictureUrl?: string | null;
}

export const profileApi = {
  get: () => api.get<User>("/profile"),
  update: (body: ProfileUpdate) => api.put<User>("/profile", body),
  listSns: () => api.get<SnsLink[]>("/sns"),
  linkSns: (body: { provider: "FACEBOOK" | "LINKEDIN"; accessToken: string; providerUserId: string }) =>
    api.post<SnsLink>("/sns/link", body),
  unlinkSns: (provider: "FACEBOOK" | "LINKEDIN") => api.delete<{ ok: true }>(`/sns/${provider}`)
};
