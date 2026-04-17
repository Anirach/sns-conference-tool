import type { Match } from "./types";
import { otherUsers } from "./users";

const KEYWORD_POOL = [
  "graph-neural-networks",
  "drug-target-interaction",
  "transformers",
  "attention",
  "long-context",
  "federated-learning",
  "privacy",
  "differential-privacy",
  "heterogeneous-graphs",
  "explainability",
  "knowledge-graphs",
  "contrastive-learning",
  "self-supervised-learning",
  "reinforcement-learning",
  "diffusion-models",
  "multi-modal",
  "vision-language-models",
  "retrieval-augmented-generation",
  "mixture-of-experts",
  "efficient-attention"
];

function seededPick(seed: number, pool: string[], k: number): string[] {
  const arr = [...pool];
  const picked: string[] = [];
  let s = seed;
  for (let i = 0; i < k && arr.length; i++) {
    s = (s * 9301 + 49297) % 233280;
    const idx = s % arr.length;
    picked.push(arr.splice(idx, 1)[0]);
  }
  return picked;
}

const NEURIPS_ID = "evt-neurips-2026-bkk";
const ACL_ID = "evt-acl-2026-vienna";

// 15 matches in NeurIPS
export const neuripsMatches: Match[] = otherUsers.slice(0, 15).map((u, i) => {
  const commonCount = 2 + (i % 5);
  const similarity = 0.42 + ((i * 37) % 45) / 100;
  const distance = 8 + ((i * 13) % 90);
  return {
    matchId: `m-neurips-${u.userId}`,
    eventId: NEURIPS_ID,
    otherUserId: u.userId,
    name: `${u.firstName} ${u.lastName}`,
    title: u.academicTitle,
    institution: u.institution,
    profilePictureUrl: u.profilePictureUrl,
    commonKeywords: seededPick(i + 1, KEYWORD_POOL, commonCount),
    similarity: Math.min(0.95, Number(similarity.toFixed(2))),
    mutual: i % 3 === 0,
    distanceMeters: distance
  };
});

// 6 matches in ACL
export const aclMatches: Match[] = otherUsers.slice(3, 9).map((u, i) => {
  const commonCount = 2 + (i % 4);
  const similarity = 0.38 + ((i * 53) % 50) / 100;
  return {
    matchId: `m-acl-${u.userId}`,
    eventId: ACL_ID,
    otherUserId: u.userId,
    name: `${u.firstName} ${u.lastName}`,
    title: u.academicTitle,
    institution: u.institution,
    profilePictureUrl: u.profilePictureUrl,
    commonKeywords: seededPick(i + 50, KEYWORD_POOL, commonCount),
    similarity: Math.min(0.92, Number(similarity.toFixed(2))),
    mutual: i % 2 === 1,
    distanceMeters: 15 + ((i * 19) % 80)
  };
});

export const allMatches: Match[] = [...neuripsMatches, ...aclMatches];

export function matchesForEvent(eventId: string, radiusMeters: number): Match[] {
  return allMatches
    .filter((m) => m.eventId === eventId && m.distanceMeters <= radiusMeters)
    .sort((a, b) => b.similarity - a.similarity);
}

export function findMatchPair(eventId: string, otherUserId: string): Match | undefined {
  return allMatches.find((m) => m.eventId === eventId && m.otherUserId === otherUserId);
}

export function findMatchById(matchId: string): Match | undefined {
  return allMatches.find((m) => m.matchId === matchId);
}
