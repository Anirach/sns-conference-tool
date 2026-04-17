export type UUID = string;

export interface User {
  userId: UUID;
  email: string;
  firstName: string;
  lastName: string;
  academicTitle: string | null;
  institution: string | null;
  profilePictureUrl: string | null;
}

export type InterestType = "TEXT" | "ARTICLE_LOCAL" | "ARTICLE_LINK";

export interface Interest {
  interestId: UUID;
  userId: UUID;
  type: InterestType;
  content: string;
  extractedKeywords: string[];
  createdAt: string;
}

export interface ConferenceEvent {
  eventId: UUID;
  eventName: string;
  venue: string;
  expirationCode: string;
  qrCode: string;
  expired: boolean;
}

export interface Participation {
  userId: UUID;
  eventId: UUID;
  selectedRadius: 20 | 50 | 100;
  joinedAt: string;
}

export interface Match {
  matchId: UUID;
  eventId: UUID;
  otherUserId: UUID;
  name: string;
  title: string | null;
  institution: string | null;
  profilePictureUrl: string | null;
  commonKeywords: string[];
  similarity: number;
  mutual: boolean;
  distanceMeters: number;
}

export interface ChatMessage {
  messageId: UUID;
  eventId: UUID;
  fromUserId: UUID;
  toUserId: UUID;
  content: string;
  readFlag: boolean;
  createdAt: string;
}

export interface ChatThread {
  threadId: string;
  eventId: UUID;
  otherUserId: UUID;
  otherName: string;
  otherTitle: string | null;
  otherInstitution: string | null;
  otherPictureUrl: string | null;
  lastMessagePreview: string;
  lastMessageAt: string;
  lastFromMe: boolean;
  unread: number;
}

export type SnsProvider = "FACEBOOK" | "LINKEDIN";

export interface SnsLink {
  provider: SnsProvider;
  providerUserId: string;
  linkedAt: string;
}

export interface UserSettings {
  pushMatches: boolean;
  pushChat: boolean;
  gpsConsent: boolean;
  localStorageOptIn: boolean;
  language: "en" | "th" | "de";
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  userId: UUID;
}
