import type { ChatMessage, Match } from "../fixtures/types";

export interface WsChatFrame {
  destination: string;
  body: ChatMessage;
}

export interface WsMatchFrame {
  destination: string;
  body: Match;
}

export type WsFrame = WsChatFrame | WsMatchFrame;

export type ChatListener = (msg: ChatMessage) => void;
export type MatchListener = (match: Match) => void;
