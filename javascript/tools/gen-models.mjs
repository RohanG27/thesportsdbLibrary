#!/usr/bin/env node
// Generates src/models.ts from the Kotlin models (../src/main/kotlin/sportsdb/model/*.kt), so the
// JavaScript models have exactly the same fields, sources and documentation as the other libraries.
//
//   node tools/gen-models.mjs          regenerate
//   node tools/gen-models.mjs --check  exit 1 if src/models.ts is out of date

import { readFileSync, readdirSync, writeFileSync, existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const kotlinDir = join(here, "..", "..", "src", "main", "kotlin", "sportsdb", "model");
const out = join(here, "..", "src", "models.ts");

const TYPES = {
  String: "string", Long: "number", Int: "number", Double: "number", Boolean: "boolean",
  LocalDate: "string", LocalTime: "string", Instant: "Date", LocalDateTime: "string",
  Socials: "Socials", PlayerExternalIds: "PlayerExternalIds",
  "List<String>": "readonly string[]", "Map<String, String>": "Readonly<Record<string, string>>", "List<LeagueRef>": "readonly LeagueRef[]",
};
const NOTES = { LocalDate: "YYYY-MM-DD", LocalTime: "HH:MM:SS", Instant: "UTC", LocalDateTime: "YYYY-MM-DDTHH:MM:SS, zone not stated" };
const SPECIAL = {
  "Team.leagues": "leagueRefs(r)",
  "Player.externalIds": "externalIds(r)",
  "Season.name": 'r.s("strSeason") ?? ""',
};
// Kotlin computed properties, computed once here so records stay plain data.
const EXTRAS = {
  League: [["description", "string | null", "The English description.", 'descriptions["EN"] ?? null']],
  Team: [["description", "string | null", "The English description.", 'descriptions["EN"] ?? null']],
  Player: [["description", "string | null", "The English description.", 'descriptions["EN"] ?? null']],
  Venue: [["description", "string | null", "The English description.", 'descriptions["EN"] ?? null'],
          ["coordinates", "readonly [number, number] | null", "`map` as [latitude, longitude], when it holds coordinates.", "coordinates(map)"]],
  Event: [["status", "EventStatus", "The status code read as a broad status.", "eventStatus(statusCode)"],
          ["stage", "RoundStage | null", "The stage when `round` is a stage code (e.g. 200 = final), or null for an ordinary round.", "roundStage(round)"]],
  LiveScore: [["status", "EventStatus", "The status code read as a broad status.", "eventStatus(statusCode)"]],
  PlayerStat: [["numericValue", "number | null", "`value` as a number, when it is one.", "numeric(value)"]],
};
const READERS = [
  [/long\("idDupe"\)\?\.takeIf \{ it != 0L \}/g, 'r.id("idDupe")'],
  [/(?<![.\w])str\(/g, "r.s("], [/(?<![.\w])id\(/g, "r.id("], [/(?<![.\w])int\(/g, "r.int("], [/(?<![.\w])year\(/g, "r.year("],
  [/(?<![.\w])double\(/g, "r.float("], [/(?<![.\w])bool\(/g, "r.bool("], [/(?<![.\w])date\(/g, "r.date("], [/(?<![.\w])time\(/g, "r.time("],
  [/(?<![.\w])instant\(/g, "r.instant("], [/(?<![.\w])localDateTime\(/g, "r.localDateTime("], [/(?<![.\w])list\(/g, "r.csv("],
  [/(?<![.\w])numbered\(/g, "r.numbered("], [/(?<![.\w])descriptions\(\)/g, "r.descriptions()"], [/(?<![.\w])locked\(\)/g, "r.locked()"],
  [/(?<![.\w])socials\(\)/g, "socials(r)"], [/\?:/g, "??"],
];

function mapperArgs(src, name) {
  const start = src.indexOf(`internal fun JsonObject.to${name}()`);
  const open = src.indexOf(`${name}(`, src.indexOf("=", start)) + name.length;
  let depth = 0, i = open;
  for (;; i++) {
    if ("([{".includes(src[i])) depth++;
    if (")]}".includes(src[i])) depth--;
    if (depth === 0) break;
  }
  const args = [];
  let cur = "";
  depth = 0;
  for (const ch of src.slice(open + 1, i)) {
    if ("([{".includes(ch)) depth++;
    if (")]}".includes(ch)) depth--;
    if (ch === "," && depth === 0) { args.push(cur); cur = ""; } else cur += ch;
  }
  args.push(cur);
  const map = {};
  for (const a of args) {
    const eq = a.indexOf("=");
    if (eq > 0) map[a.slice(0, eq).trim()] = a.slice(eq + 1).split(/\s+/).join(" ").trim();
  }
  return map;
}

const src = readdirSync(kotlinDir).filter((f) => f.endsWith(".kt")).sort().map((f) => readFileSync(join(kotlinDir, f), "utf8")).join("");
const classRe = /((?:\/\*\*(?:(?!\*\/)[\s\S])*\*\/\n)?)public class (\w+) internal constructor\(((?:(?!public class)[\s\S])*?)\n\) : ApiRecord\(raw\) \{/g;
const blocks = [];
const parsers = [];
const names = [];
for (const [, doc, name, body] of src.matchAll(classRe)) {
  names.push(name);
  const mapping = mapperArgs(src, name);
  const props = [...body.matchAll(/((?:\/\*\*(?:(?!\*\/)[\s\S])*\*\/\s*)?)public val (\w+): ([\w<>, ?]+),/g)];
  const lines = [], assigns = [];
  for (const [, pdoc, prop, ktype] of props) {
    const base = ktype.replace(/\?$/, "");
    const nullable = ktype.endsWith("?");
    let text = pdoc.replace(/\/\*\*|\*\//g, "").split("\n").map((l) => l.replace(/^\s*\*?\s?/, "").trim()).join(" ").trim();
    if (NOTES[base]) text = text ? `${text} (${NOTES[base]})` : NOTES[base];
    const type = TYPES[base] + (nullable && !base.startsWith("List") && !base.startsWith("Map") ? " | null" : "");
    lines.push(`${text ? `  /** ${text} */\n` : ""}  readonly ${prop}: ${type};`);
    let expr = SPECIAL[`${name}.${prop}`];
    if (!expr) {
      expr = mapping[prop];
      for (const [re, rep] of READERS) expr = expr.replace(re, rep);
    }
    assigns.push([prop, expr]);
  }
  for (const [prop, type, d] of EXTRAS[name] ?? []) lines.push(`  /** ${d} */\n  readonly ${prop}: ${type};`);
  const classDoc = doc.replace(/\/\*\*|\*\//g, "").split("\n").map((l) => l.replace(/^\s*\*?\s?/, "")).filter((l, i, a) => l.trim() !== "" || (i > 0 && i < a.length - 1)).map((l) => ` * ${l}`.trimEnd()).join("\n") || ` * ${name} record.`;
  blocks.push(`/**\n${classDoc}\n */\nexport interface ${name} extends ApiRecord {\n  readonly kind: "${name}";\n${lines.join("\n")}\n}\n`);
  const extras = (EXTRAS[name] ?? []).map(([prop, , , expr]) => `    ${prop}: ${expr},`);
  const needs = new Set((EXTRAS[name] ?? []).flatMap(([, , , expr]) => [...expr.matchAll(/\b(descriptions|statusCode|map|value|round)\b/g)].map((m) => m[1])));
  const locals = assigns.filter(([p]) => needs.has(p)).map(([p, e]) => `  const ${p} = ${e};`);
  const fields = assigns.map(([p, e]) => (needs.has(p) ? `    ${p},` : `    ${p}: ${e},`));
  parsers.push(`/** @internal */\nexport function parse${name}(raw: RawRecord): ${name} {\n  const r = new Rec(raw);\n${locals.join("\n")}${locals.length ? "\n" : ""}  return Object.freeze({\n    kind: "${name}",\n    raw,\n${fields.join("\n")}\n${extras.join("\n")}${extras.length ? "\n" : ""}  });\n}\n`);
}

const text = `// GENERATED by tools/gen-models.mjs from the Kotlin models. Do not edit; run \`npm run generate\`.
/* eslint-disable */
import { Rec, type RawRecord } from "./fields.js";
import {
  coordinates, eventStatus, externalIds, leagueRefs, numeric, roundStage, socials,
  type ApiRecord, type EventStatus, type LeagueRef, type PlayerExternalIds, type RoundStage, type Socials,
} from "./model-support.js";

${blocks.join("\n")}
/** Every record type. */
export type AnyRecord = ${names.join(" | ")};

${parsers.join("\n")}`;

if (process.argv.includes("--check")) {
  if (!existsSync(out) || readFileSync(out, "utf8") !== text) {
    console.error("src/models.ts is out of date (run npm run generate)");
    process.exit(1);
  }
} else {
  writeFileSync(out, text);
  console.log(`wrote ${names.length} models to src/models.ts`);
}
