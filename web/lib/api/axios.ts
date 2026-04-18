import axios, { AxiosError, AxiosRequestConfig } from "axios";
import { bridge } from "../bridge/client";

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE ?? "/api",
  timeout: 10000,
  headers: { "Content-Type": "application/json" }
});

api.interceptors.request.use(async (cfg) => {
  // Don't attach bearer to auth endpoints. A stale JWT from a previous backend (different
  // signing keypair after a restart, expired, etc.) makes the resource-server filter reject
  // the request with 401 *before* the permitAll login controller runs — the user sees their
  // valid credentials get refused for no apparent reason.
  if (cfg.url && /^\/?auth\//.test(cfg.url)) return cfg;
  try {
    const jwt = await bridge.call<string | null>("storage.get", { key: "jwt" });
    if (jwt) {
      cfg.headers.set("Authorization", `Bearer ${jwt}`);
    }
  } catch {
    /* bridge unavailable / no jwt yet */
  }
  return cfg;
});

interface RetryableConfig extends AxiosRequestConfig {
  __retried?: boolean;
}

api.interceptors.response.use(
  (r) => r,
  async (err: AxiosError) => {
    const original = err.config as RetryableConfig | undefined;
    if (err.response?.status !== 401 || !original || original.__retried) {
      throw err;
    }
    try {
      const refresh = await bridge.call<string | null>("storage.get", { key: "refresh" });
      if (!refresh) throw err;
      const { data } = await axios.post(`${api.defaults.baseURL}/auth/refresh`, { refresh });
      await bridge.call("storage.set", { key: "jwt", value: data.accessToken });
      await bridge.call("storage.set", { key: "refresh", value: data.refreshToken });
      original.__retried = true;
      original.headers = { ...original.headers, Authorization: `Bearer ${data.accessToken}` };
      return api(original);
    } catch {
      throw err;
    }
  }
);

export default api;
