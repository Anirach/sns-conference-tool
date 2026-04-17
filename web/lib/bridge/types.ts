export interface BridgeMessage<T = unknown> {
  id: string;
  type: string;
  payload: T;
}

export interface BridgeResponse<T = unknown> {
  ok: boolean;
  data?: T;
  error?: string;
}

export interface QrScanResult {
  eventCode: string;
}

export interface FilePickResult {
  path: string;
  name: string;
  sizeBytes: number;
  previewBase64: string | null;
}

export interface SnsLoginResult {
  provider: "facebook" | "linkedin";
  accessToken: string;
  providerUserId: string;
}

export interface AppInfo {
  version: string;
  platform: "android" | "ios" | "web";
  deviceModel: string;
}
