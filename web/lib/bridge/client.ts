import { browserBridgeMock } from "./mock";

declare global {
  interface Window {
    FlutterBridge?: { postMessage: (msg: string) => void };
    __bridgeResolve?: Record<string, (raw: unknown) => void>;
  }
}

type BridgePayload = Record<string, unknown>;

class BridgeClient {
  private idCounter = 0;

  constructor() {
    if (typeof window !== "undefined") {
      window.__bridgeResolve = window.__bridgeResolve ?? {};
    }
  }

  private nextId(): string {
    return `b_${Date.now()}_${++this.idCounter}`;
  }

  get available(): boolean {
    return typeof window !== "undefined" && !!window.FlutterBridge;
  }

  async call<T = unknown>(type: string, payload: BridgePayload = {}, timeoutMs = 15000): Promise<T> {
    if (!this.available) {
      const handlers = browserBridgeMock as unknown as Record<
        string,
        (p: BridgePayload) => Promise<unknown>
      >;
      const handler = handlers[type];
      if (!handler) {
        throw new Error(`Browser bridge-mock has no handler for: ${type}`);
      }
      return (await handler(payload)) as T;
    }

    const id = this.nextId();
    return new Promise<T>((resolve, reject) => {
      const timer = setTimeout(() => {
        delete window.__bridgeResolve![id];
        reject(new Error(`Bridge timeout: ${type}`));
      }, timeoutMs);

      window.__bridgeResolve![id] = (raw) => {
        clearTimeout(timer);
        delete window.__bridgeResolve![id];
        const r = raw as { ok: boolean; data?: T; error?: string };
        r.ok ? resolve(r.data as T) : reject(new Error(r.error ?? "Bridge error"));
      };

      window.FlutterBridge!.postMessage(JSON.stringify({ id, type, payload }));
    });
  }
}

export const bridge = new BridgeClient();
