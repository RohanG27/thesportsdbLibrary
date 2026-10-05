import { ApiMessageError, InvalidApiKeyError, ResponseParseError } from "./errors.js";
import type { RawRecord } from "./fields.js";

/**
 * Pulls the records out of a response. Every response is one JSON object with one key holding a
 * list of records; the key differs by endpoint, so the expected key is tried first and any
 * list-valued key is the fallback. An empty body, {"key": null} and {"Message": "No data found"}
 * all mean "no results". A string under the record key is an error message.
 * @internal
 */
export function parseRecords(body: string, expectedKey: string, displayUrl: string): RawRecord[] {
  if (body.trim() === "") return [];
  let root: unknown;
  try {
    root = JSON.parse(body);
  } catch (e) {
    throw new ResponseParseError(`Response from ${displayUrl} is not JSON: ${body.slice(0, 120)}`, { cause: e });
  }
  if (root === null || typeof root !== "object" || Array.isArray(root)) {
    throw new ResponseParseError(`Response from ${displayUrl} is not a JSON object`);
  }
  const obj = root as Record<string, unknown>;

  const message = obj["Message"];
  if (typeof message === "string") {
    const lower = message.toLowerCase();
    if (lower.startsWith("no data found")) return [];
    if (lower.startsWith("invalid premium api key")) {
      throw new InvalidApiKeyError(`TheSportsDB rejected the API key (${displayUrl}): ${message}`);
    }
    if (Object.keys(obj).length === 1) throw new ApiMessageError(`TheSportsDB said: ${message} (${displayUrl})`, message);
  }

  const value = expectedKey in obj ? obj[expectedKey] : Object.values(obj).find(Array.isArray);
  if (value == null) return [];
  if (typeof value === "string") {
    // A rejected parameter: {"seasons": "Invalid League ID passed"}
    throw new ApiMessageError(`TheSportsDB said: ${value} (${displayUrl})`, value);
  }
  if (Array.isArray(value)) {
    return value.filter((r): r is Record<string, unknown> => r !== null && typeof r === "object" && !Array.isArray(r)).map(normalize);
  }
  if (typeof value === "object") return [normalize(value as Record<string, unknown>)];
  throw new ResponseParseError(`Unexpected '${expectedKey}' value in response from ${displayUrl}`);
}

/** Every value as text or null: blank strings become null; JSON numbers (v2 search) become text. */
export function normalize(record: Record<string, unknown>): RawRecord {
  const out: Record<string, string | null> = {};
  for (const [k, v] of Object.entries(record)) {
    if (typeof v === "string") out[k] = v.trim() === "" ? null : v;
    else if (typeof v === "number" || typeof v === "bigint") out[k] = String(v);
    else if (typeof v === "boolean") out[k] = v ? "true" : "false";
    else out[k] = null;
  }
  return Object.freeze(out);
}
