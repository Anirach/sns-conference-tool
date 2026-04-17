import type { Interest } from "./types";
import { CURRENT_USER_ID } from "./users";

const iso = (daysAgo: number) =>
  new Date(Date.now() - daysAgo * 24 * 3600 * 1000).toISOString();

export const interestsForCurrentUser: Interest[] = [
  {
    interestId: "int-0001",
    userId: CURRENT_USER_ID,
    type: "TEXT",
    content:
      "I work on graph neural networks for drug–target interaction prediction, focusing on heterogeneous graphs and explainability.",
    extractedKeywords: [
      "graph-neural-networks",
      "drug-target-interaction",
      "heterogeneous-graphs",
      "explainability",
      "pharmaceutical-ai"
    ],
    createdAt: iso(14)
  },
  {
    interestId: "int-0002",
    userId: CURRENT_USER_ID,
    type: "ARTICLE_LINK",
    content: "https://arxiv.org/abs/2305.12345",
    extractedKeywords: [
      "transformers",
      "attention",
      "long-context",
      "efficient-attention",
      "linear-attention"
    ],
    createdAt: iso(6)
  },
  {
    interestId: "int-0003",
    userId: CURRENT_USER_ID,
    type: "ARTICLE_LOCAL",
    content: "s3://mock/articles/federated-learning-survey.pdf",
    extractedKeywords: [
      "federated-learning",
      "privacy",
      "distributed-training",
      "differential-privacy",
      "model-aggregation"
    ],
    createdAt: iso(2)
  }
];
