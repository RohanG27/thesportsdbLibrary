#!/usr/bin/env node
// Copies the recorded API responses from the Kotlin project (../kotlin/src/test/resources/fixtures, recorded by
// ../tools/record-fixtures.sh) into test/fixtures, so every library tests against the same responses.
import { cpSync, rmSync, readdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const source = join(here, "..", "..", "kotlin", "src", "test", "resources", "fixtures");
const target = join(here, "..", "test", "fixtures");
rmSync(target, { recursive: true, force: true });
cpSync(source, target, { recursive: true });
const count = readdirSync(target, { recursive: true }).filter((f) => String(f).endsWith(".json")).length;
console.log(`copied ${count} fixtures to test/fixtures`);
