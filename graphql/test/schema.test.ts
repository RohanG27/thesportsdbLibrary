import { execFileSync } from "node:child_process";
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { addMocksToSchema } from "@graphql-tools/mock";
import { makeExecutableSchema } from "@graphql-tools/schema";
import {
  buildSchema, graphql, isObjectType, parse, validate, validateSchema, type GraphQLField, type GraphQLObjectType,
} from "graphql";
import { describe, expect, it } from "vitest";
import { parse as parseYaml } from "yaml";
import { sources, typeDefs } from "../src/index.js";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const sdl = readFileSync(join(root, "schema.graphql"), "utf8");
const schema = buildSchema(sdl);
const objectTypes = Object.values(schema.getTypeMap()).filter((t): t is GraphQLObjectType => isObjectType(t) && !t.name.startsWith("__"));

describe("the schema", () => {
  it("is valid GraphQL", () => {
    expect(validateSchema(schema)).toEqual([]);
  });

  it("is generated and up to date", () => {
    expect(() => execFileSync(process.execPath, [join(root, "tools", "gen-schema.mjs"), "--check"], { stdio: "pipe" })).not.toThrow();
    expect(typeDefs).toBe(sdl);
  });

  it("documents every type and field", () => {
    const builtIn = ["String", "Int", "Float", "Boolean", "ID"];
    // Members of small support types whose names say it all.
    const selfExplanatory = new Set(["LocalizedText.language", "LocalizedText.text", "Coordinates.latitude", "Coordinates.longitude",
      "LeagueRef.id", "LeagueRef.name"]);
    const missing: string[] = [];
    for (const type of Object.values(schema.getTypeMap())) {
      if (type.name.startsWith("__") || builtIn.includes(type.name)) continue;
      if (!type.description) missing.push(type.name);
      if (!isObjectType(type)) continue;
      for (const f of Object.values(type.getFields())) {
        const path = `${type.name}.${f.name}`;
        if (!f.description && !selfExplanatory.has(path)) missing.push(path);
      }
    }
    expect(missing).toEqual([]);
  });

  it("has a record type for every model, with the same field names as the client libraries", () => {
    const models = readFileSync(join(root, "..", "javascript", "src", "models.ts"), "utf8");
    for (const [, name, body] of models.matchAll(/export interface (\w+) extends ApiRecord \{\n([\s\S]*?)\n\}/g)) {
      const type = schema.getType(name ?? "");
      expect(type, name).toBeDefined();
      const fields = Object.keys((type as GraphQLObjectType).getFields());
      for (const [, prop] of (body ?? "").matchAll(/readonly (\w+):/g)) {
        if (prop === "kind") continue;
        expect(fields, `${name}.${prop}`).toContain(prop);
      }
    }
  });

  it("documents every field that appears in recorded responses (cited by a description, or kept in raw)", () => {
    const glossary = parseYaml(readFileSync(join(root, "..", "docs-site", "fields.yaml"), "utf8")) as Record<string, unknown>;
    const cited = new Set([...sdl.matchAll(/`(\w+)`/g)].map((m) => m[1]));
    const families = /^(strDescription[A-Z]{2}|strFanart\d|strColour\d|(id|str)League\d)$/;
    const notCited = Object.keys(glossary).filter((f) => !cited.has(f) && !families.test(f) && f !== "Message");
    expect(notCited).toEqual([]);
  });
});

// --- @source against the OpenAPI descriptions ---------------------------------------------------

const openapi = (v: string) => parseYaml(readFileSync(join(root, "..", "docs-site", "openapi", `${v}.yaml`), "utf8")) as {
  paths: Record<string, { get: { parameters?: Array<{ name?: string; $ref?: string }> } }>;
};
const v1Spec = openapi("v1");
const v2Spec = openapi("v2");

function owner(key: string): { type: GraphQLObjectType; field: GraphQLField<unknown, unknown> } {
  const [typeName, fieldName] = key.split(".") as [string, string];
  const type = schema.getType(typeName) as GraphQLObjectType;
  return { type, field: type.getFields()[fieldName] as GraphQLField<unknown, unknown> };
}

function placeholders(template: string): string[] {
  return [...template.matchAll(/\{(\w+)\}/g)].map((m) => m[1] as string);
}

describe("@source", () => {
  const entries = Object.entries(sources);

  it("annotates every query and every relationship field", () => {
    const queryFields = Object.keys(schema.getQueryType()?.getFields() ?? {});
    expect(queryFields.filter((f) => !sources[`Query.${f}`])).toEqual([]);
    expect(entries.length).toBeGreaterThan(60);
  });

  it.each(entries)("%s: placeholders refer to parent fields or arguments", (key, src) => {
    const { type, field } = owner(key);
    const known = new Set([...Object.keys(type.getFields()), ...field.args.map((a) => a.name)]);
    for (const template of [src.v1, src.v2].filter((t): t is string => !!t)) {
      for (const p of placeholders(template)) expect(known, `${key}: {${p}} in ${template}`).toContain(p);
    }
  });

  it.each(entries.filter(([, s]) => s.v1))("%s: the v1 endpoint and its parameters exist", (key, src) => {
    const [endpoint, query = ""] = (src.v1 as string).split("?") as [string, string?];
    const op = v1Spec.paths[`/api/v1/json/{apiKey}/${endpoint}`];
    expect(op, `${key}: ${endpoint} not in the v1 spec`).toBeDefined();
    const names = new Set((op?.get.parameters ?? []).map((p) => p.name).filter(Boolean));
    for (const pair of query.split("&").filter(Boolean)) expect(names, `${key}: ${pair}`).toContain(pair.split("=")[0]);
  });

  it.each(entries.filter(([, s]) => s.v2))("%s: the v2 endpoint exists", (key, src) => {
    const wanted = (src.v2 as string).split("/");
    const match = Object.keys(v2Spec.paths).some((path) => {
      const segs = path.replace("/api/v2/json/", "").split("/");
      return segs.length === wanted.length && segs.every((s, i) => s.startsWith("{") || (wanted[i] as string).startsWith("{") || s === wanted[i]);
    });
    expect(match, `${key}: ${src.v2} not in the v2 spec`).toBe(true);
  });
});

// --- The README's example queries -------------------------------------------------------------

const readme = readFileSync(join(root, "README.md"), "utf8");
const examples = [...readme.matchAll(/```graphql\n([\s\S]*?)```/g)].map((m) => m[1] as string).filter((q) => /^\s*query\b/.test(q));

describe("README examples", () => {
  const mocked = addMocksToSchema({
    schema: makeExecutableSchema({ typeDefs: sdl }),
    mocks: {
      Date: () => "2026-10-04", DateTime: () => "2026-10-04T19:00:00Z", LocalTime: () => "19:00:00",
      LocalDateTime: () => "2026-10-04T19:00:00", JSONObject: () => ({}), ID: () => "133604",
    },
  });

  it("has examples", () => {
    expect(examples.length).toBeGreaterThanOrEqual(5);
  });

  it.each(examples.map((q) => [/query (\w+)/.exec(q)?.[1] ?? "?", q]))("%s validates and runs", async (_name, query) => {
    expect(validate(schema, parse(query))).toEqual([]);
    const result = await graphql({ schema: mocked, source: query });
    expect(result.errors).toBeUndefined();
    expect(result.data).toBeTruthy();
  });
});
