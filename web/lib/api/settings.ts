import api from "./axios";

export interface UserSettings {
  pushMatches: boolean;
  pushChat: boolean;
  gpsConsent: boolean;
  keepRegister: boolean;
  language: "en" | "th" | "de";
}

export type UpdateUserSettings = Partial<UserSettings>;

export const settingsApi = {
  get: () => api.get<UserSettings>("/profile/settings"),
  update: (body: UpdateUserSettings) => api.put<UserSettings>("/profile/settings", body)
};
