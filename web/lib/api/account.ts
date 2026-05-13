import api from "./axios";

export const accountApi = {
  /** Soft-delete the current user. The hard-delete cron sweeps 30 days later. */
  softDelete: () => api.delete<{ ok: true; status: string }>("/users/me")
};
