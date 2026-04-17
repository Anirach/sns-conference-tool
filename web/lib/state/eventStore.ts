"use client";

import { create } from "zustand";
import type { ConferenceEvent } from "../fixtures/types";

type Radius = 20 | 50 | 100;

interface EventState {
  activeEvent: ConferenceEvent | null;
  radius: Radius;
  gpsActive: boolean;
  online: boolean;
  setActiveEvent: (e: ConferenceEvent | null) => void;
  setRadius: (r: Radius) => void;
  setGpsActive: (on: boolean) => void;
  setOnline: (online: boolean) => void;
}

export const useEventStore = create<EventState>((set) => ({
  activeEvent: null,
  radius: 50,
  gpsActive: false,
  online: true,
  setActiveEvent: (activeEvent) => set({ activeEvent }),
  setRadius: (radius) => set({ radius }),
  setGpsActive: (gpsActive) => set({ gpsActive }),
  setOnline: (online) => set({ online })
}));
