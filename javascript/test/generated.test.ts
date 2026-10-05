import { execFileSync } from "node:child_process";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { expect, it } from "vitest";

it("src/models.ts is generated from the Kotlin models", () => {
  const tool = join(dirname(fileURLToPath(import.meta.url)), "..", "tools", "gen-models.mjs");
  expect(() => execFileSync(process.execPath, [tool, "--check"], { stdio: "pipe" })).not.toThrow();
});
