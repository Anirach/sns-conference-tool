import axios, { AxiosError, AxiosRequestConfig } from "axios";
import { getItem, removeItem, setItem } from "../native/storage";

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE ?? "/api",
  timeout: 10000,
  headers: { "Content-Type": "application/json" }
});

api.interceptors.request.use((cfg) => {
  // Don't attach bearer to auth endpoints. A stale JWT from a previous backend (different
  // signing keypair after a restart, expired, etc.) makes the resource-server filter reject
  // the request with 401 *before* the permitAll login controller runs — the user sees their
  // valid credentials get refused for no apparent reason.
  if (cfg.url && /^\/?auth\//.test(cfg.url)) return cfg;
  const jwt = getItem("auth.jwt");
  if (jwt) cfg.headers.set("Authorization", `Bearer ${jwt}`);
  return cfg;
});

interface RetryableConfig extends AxiosRequestConfig {
  __retried?: boolean;
}

/**
 * Drops the locally-stored session, then forces a hard nav to /login. We use location
 * (not next/router) because we're inside an interceptor — the user may not be on a page
 * that has access to the router, and we want any in-flight Tanstack Query state cleared.
 */
function clearSessionAndRedirect() {
  removeItem("auth.jwt");
  removeItem("auth.refresh");
  if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
    window.location.assign("/login");
  }
}

api.interceptors.response.use(
  (r) => r,
  async (err: AxiosError) => {
    const original = err.config as RetryableConfig | undefined;
    if (err.response?.status !== 401 || !original || original.__retried) {
      throw err;
    }
    const refresh = getItem("auth.refresh");
    if (!refresh) {
      // Nothing to refresh with — the user just needs to log in.
      clearSessionAndRedirect();
      throw err;
    }
    try {
      const { data } = await axios.post(`${api.defaults.baseURL}/auth/refresh`, { refresh });
      setItem("auth.jwt", data.accessToken);
      setItem("auth.refresh", data.refreshToken);
      original.__retried = true;
      original.headers = { ...original.headers, Authorization: `Bearer ${data.accessToken}` };
      return api(original);
    } catch {
      // Refresh itself failed (token revoked, backend keypair rotated, etc.). Stranded
      // tokens loop the user through 401 → refresh-400 forever; clear them and bounce
      // them to /login so the very next interaction starts a fresh session.
      clearSessionAndRedirect();
      throw err;
    }
  }
);

export default api;
