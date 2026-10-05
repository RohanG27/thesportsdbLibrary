#!/usr/bin/env node
// Generates ../schema.graphql: the record types from the Kotlin models (so field names match every
// client library), with descriptions from the docs glossary (../../docs-site/fields.yaml), merged with
// the hand-written src/relations.graphql (scalars, @source, Query and relationships).
//
//   node tools/gen-schema.mjs          regenerate
//   node tools/gen-schema.mjs --check  exit 1 if schema.graphql is out of date

import { readFileSync, readdirSync, writeFileSync, existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { mergeTypeDefs } from "@graphql-tools/merge";
import { Kind, print } from "graphql";
import { parse as parseYaml } from "yaml";

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, "..");
const kotlinDir = join(root, "..", "src", "main", "kotlin", "sportsdb", "model");
const glossary = parseYaml(readFileSync(join(root, "..", "docs-site", "fields.yaml"), "utf8"));
const relations = readFileSync(join(root, "src", "relations.graphql"), "utf8");
const out = join(root, "schema.graphql");
const outTs = join(root, "src", "generated.ts");

const SCALARS = {
  String: "String", Int: "Int", Double: "Float", Boolean: "Boolean",
  LocalDate: "Date", LocalTime: "LocalTime", Instant: "DateTime", LocalDateTime: "LocalDateTime",
  Socials: "Socials!", PlayerExternalIds: "PlayerExternalIds!",
  "List<String>": "[String!]!", "Map<String, String>": "[LocalizedText!]!", "List<LeagueRef>": "[LeagueRef!]!",
};
// Images TheSportsDB can resize (not the sport pictures under /images/sports/, which return 404 for sizes).
const IMAGE = /^str(Badge|Logo|Thumb|Cutout|Render|Cartoon|Banner|Poster|Fanart\d?|Square|Trophy|Equipment|TeamBadge|HomeTeamBadge|AwayTeamBadge|LeagueBadge|HonourLogo|HonourTrophy|MilestoneLogo|EventThumb|EventPoster|EventBanner|EventSquare)$/;
const EXTRAS = {
  League: ['"The English description (`strDescriptionEN`)."\n  description: String'],
  Team: ['"The English description (`strDescriptionEN`)."\n  description: String'],
  Player: ['"The English description (`strDescriptionEN`)."\n  description: String'],
  Venue: ['"The English description (`strDescriptionEN`)."\n  description: String', '"`strMap` as coordinates, when it holds them."\n  coordinates: Coordinates'],
  Event: ['"`strStatus` read as a broad status."\n  status: EventStatus!'],
  LiveScore: ['"`strStatus` read as a broad status."\n  status: EventStatus!'],
  PlayerStat: ['"`strValue` as a number, when it is one."\n  numericValue: Float'],
};

const escape = (s) => s.replace(/\\/g, "\\\\").replace(/"/g, '\\"');
const clean = (doc) => doc.replace(/\/\*\*|\*\//g, "").split("\n").map((l) => l.replace(/^\s*\*?\s?/, "").trim()).join(" ").trim();

function meaning(type, apiField) {
  const entry = glossary[apiField];
  if (entry == null) return null;
  return typeof entry === "string" ? entry : (entry[type] ?? entry.default);
}

function describe(type, kdoc) {
  const m = /^`(\w+)`(?:[,:]\s*(.*))?$/.exec(kdoc);
  if (!m) return { text: kdoc, apiField: null };
  const apiField = m[1];
  const glossed = meaning(type, apiField);
  return { text: glossed ? `\`${apiField}\`: ${glossed}` : kdoc, apiField };
}

const src = readdirSync(kotlinDir).filter((f) => f.endsWith(".kt")).sort().map((f) => readFileSync(join(kotlinDir, f), "utf8")).join("");
const classRe = /((?:\/\*\*(?:(?!\*\/)[\s\S])*\*\/\n)?)public class (\w+) internal constructor\(((?:(?!public class)[\s\S])*?)\n\) : ApiRecord\(raw\) \{/g;
const types = [];
for (const [, doc, name, body] of src.matchAll(classRe)) {
  const fields = [];
  for (const [, pdoc, prop, ktype] of body.matchAll(/((?:\/\*\*(?:(?!\*\/)[\s\S])*\*\/\s*)?)public val (\w+): ([\w<>, ?]+),/g)) {
    const base = ktype.replace(/\?$/, "");
    let gql = base === "Long" ? (prop === "id" || prop.endsWith("Id") || prop === "duplicateOf" ? "ID" : "Int") : SCALARS[base];
    if (!gql) throw new Error(`no GraphQL type for ${name}.${prop}: ${ktype}`);
    if (!ktype.endsWith("?") && !gql.endsWith("!")) gql += "!";
    let { text, apiField } = describe(name, clean(pdoc));
    text ||= { socials: "Web and social links.", externalIds: "Ids of the same player in other databases." }[prop] ?? "";
    if (base === "List<String>" && apiField && /comma-separated/.test(text)) text += " Returned already split into a list.";
    const args = apiField && IMAGE.test(apiField) ? "(\"A smaller copy of the image.\" size: ImageSize)" : "";
    fields.push(`${text ? `  "${escape(text)}"\n` : ""}  ${prop}${args}: ${gql}`);
  }
  for (const extra of EXTRAS[name] ?? []) fields.push(`  ${extra}`);
  fields.push('  "Every field as the API sent it (blank strings as null), for fields not modelled here."\n  raw: JSONObject!');
  const typeDoc = clean(doc) || `A ${name} record.`;
  types.push(`"""\n${typeDoc}\n"""\ntype ${name} {\n${fields.join("\n")}\n}`);
}

// Record types first so each type lists its own fields before the relationships added by
// relations.graphql; then reorder so the file reads top-down: directive, scalars, enums, Query,
// support types, records.
const merged = mergeTypeDefs([types.join("\n\n"), relations], { sort: false, throwOnConflict: true });
const recordNames = new Set(types.map((t) => /type (\w+) \{/.exec(t)[1]));
const rank = (d) => {
  if (d.kind === "DirectiveDefinition") return 0;
  if (d.kind === "ScalarTypeDefinition") return 1;
  if (d.kind === "EnumTypeDefinition") return 2;
  if (d.name?.value === "Query") return 3;
  return recordNames.has(d.name?.value) ? 5 : 4;
};
const relationsOrder = [...relations.matchAll(/^(?:type|enum|scalar|directive @?)(\w+)/gm)].map((m) => m[1]);
const position = (d) => (recordNames.has(d.name?.value) ? 0 : relationsOrder.indexOf(d.name?.value));
merged.definitions = [...merged.definitions].sort((a, b) => rank(a) - rank(b) || position(a) - position(b));
const text = `# GENERATED by tools/gen-schema.mjs from the Kotlin models, docs-site/fields.yaml and src/relations.graphql.
# Do not edit; run \`npm run generate\`.

${print(merged)}
`;

// Every @source, keyed "Type.field", for implementers who want to wire resolvers from data.
const sources = {};
for (const def of merged.definitions) {
  if (def.kind !== Kind.OBJECT_TYPE_DEFINITION) continue;
  for (const f of def.fields ?? []) {
    const dir = f.directives?.find((d) => d.name.value === "source");
    if (!dir) continue;
    const entry = {};
    for (const a of dir.arguments) entry[a.name.value] = a.value.value;
    sources[`${def.name.value}.${f.name.value}`] = entry;
  }
}
const ts = `// GENERATED by tools/gen-schema.mjs. Do not edit; run \`npm run generate\`.

/** Where a field's data comes from: the endpoints named by its @source directive. */
export interface Source {
  /** A v1 endpoint, relative to /api/v1/json/{key}/, with {placeholders}. */
  readonly v1?: string;
  /** A v2 endpoint, relative to /api/v2/json/, with {placeholders}. */
  readonly v2?: string;
  readonly note?: string;
}

/** The schema as SDL, for GraphQL Yoga, Apollo Server, graphql-js buildSchema or code generators. */
export const typeDefs: string = ${JSON.stringify(text)};

/** Every @source in the schema, keyed "Type.field". */
export const sources: Readonly<Record<string, Source>> = ${JSON.stringify(sources, null, 2)};
`;

if (process.argv.includes("--check")) {
  const stale = [[out, text], [outTs, ts]].filter(([p, t]) => !existsSync(p) || readFileSync(p, "utf8") !== t).map(([p]) => p);
  if (stale.length) {
    console.error(`out of date: ${stale.join(", ")} (run npm run generate)`);
    process.exit(1);
  }
} else {
  writeFileSync(out, text);
  writeFileSync(outTs, ts);
  console.log(`wrote schema.graphql (${types.length} record types) and src/generated.ts (${Object.keys(sources).length} sources)`);
}
