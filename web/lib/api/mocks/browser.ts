import { setupWorker } from "msw/browser";
import { handlersByDomain, type MockDomain } from "./handlers";

/**
 * Build an MSW worker that only mocks the given domains. Used by `init.ts` to
 * honour the per-domain toggle encoded in `NEXT_PUBLIC_MOCK_API`.
 */
export function buildWorker(domains: MockDomain[]) {
  const handlers = domains.flatMap((d) => handlersByDomain[d] ?? []);
  return setupWorker(...handlers);
}
