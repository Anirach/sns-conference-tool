"use client";

import { createContext, ReactNode, useContext, useEffect, useState } from "react";
import { bridge } from "@/lib/bridge/client";

interface BridgeContextValue {
  available: boolean;
  platform: "android" | "ios" | "web";
}

const Ctx = createContext<BridgeContextValue>({ available: false, platform: "web" });

export function useBridge(): BridgeContextValue {
  return useContext(Ctx);
}

export function BridgeProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<BridgeContextValue>({ available: false, platform: "web" });

  useEffect(() => {
    const available = bridge.available;
    bridge
      .call<{ platform: "android" | "ios" | "web" }>("app.info")
      .then((info) => setState({ available, platform: info.platform }))
      .catch(() => setState({ available, platform: "web" }));
  }, []);

  return <Ctx.Provider value={state}>{children}</Ctx.Provider>;
}
