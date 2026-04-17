import type { AppInfo, FilePickResult, QrScanResult, SnsLoginResult } from "./types";

const STORAGE_PREFIX = "sns.bridge.mock.";

function storageKey(k: string) {
  return `${STORAGE_PREFIX}${k}`;
}

export const browserBridgeMock = {
  async "gps.start"(_p: { intervalSec?: number; minMoveMeters?: number }) {
    console.info("[bridge-mock] gps.start (no-op in browser)");
    return { started: true };
  },

  async "gps.stop"() {
    console.info("[bridge-mock] gps.stop");
    return { stopped: true };
  },

  async "qr.scan"(): Promise<QrScanResult> {
    const input = typeof window !== "undefined" ? window.prompt("Enter demo event code:", "NEURIPS2026") : "NEURIPS2026";
    return { eventCode: (input ?? "NEURIPS2026").trim() };
  },

  async "file.pickArticle"(): Promise<FilePickResult> {
    return {
      path: "browser-mock://sample.pdf",
      name: "sample-article.pdf",
      sizeBytes: 245_760,
      previewBase64: null
    };
  },

  async "storage.get"(p: { key: string }): Promise<string | null> {
    if (typeof window === "undefined") return null;
    return window.sessionStorage.getItem(storageKey(p.key));
  },

  async "storage.set"(p: { key: string; value: string }): Promise<{ ok: true }> {
    if (typeof window !== "undefined") {
      window.sessionStorage.setItem(storageKey(p.key), p.value);
    }
    return { ok: true };
  },

  async "storage.delete"(p: { key: string }): Promise<{ ok: true }> {
    if (typeof window !== "undefined") {
      window.sessionStorage.removeItem(storageKey(p.key));
    }
    return { ok: true };
  },

  async "localdb.matches.list"() {
    return [];
  },

  async "localdb.matches.save"() {
    return { ok: true };
  },

  async "sns.login"(p: { provider: "facebook" | "linkedin" }): Promise<SnsLoginResult> {
    await new Promise((r) => setTimeout(r, 800));
    return {
      provider: p.provider,
      accessToken: `mock_oauth_token_${p.provider}_${Date.now()}`,
      providerUserId: `mock_${p.provider}_uid`
    };
  },

  async "push.requestPermission"() {
    return { granted: true };
  },

  async "push.token"() {
    return { token: "mock-fcm-token-browser" };
  },

  async "app.info"(): Promise<AppInfo> {
    return {
      version: "0.1.0-dev",
      platform: "web",
      deviceModel: navigator.userAgent
    };
  }
} as const;

export type BridgeMockType = typeof browserBridgeMock;
