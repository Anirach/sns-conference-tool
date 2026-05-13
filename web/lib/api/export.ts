import api from "./axios";

export const exportApi = {
  /** Streams the GDPR ZIP. Returns a Blob the caller saves via URL.createObjectURL. */
  download: () => api.get<Blob>("/users/me/export", { responseType: "blob" })
};
