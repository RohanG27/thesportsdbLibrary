import { defineConfig } from "vitest/config";

// Calls the real API: npm run test:live (set THESPORTSDB_API_KEY for v2).
export default defineConfig({
  test: { include: ["test/live.test.ts"], testTimeout: 60_000 },
});
