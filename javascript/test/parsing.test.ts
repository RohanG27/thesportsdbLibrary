import { describe, expect, it } from "vitest";
import { normalize, parseRecords } from "../src/envelope.js";
import { Rec } from "../src/fields.js";
import { ApiMessageError, InvalidApiKeyError, ResponseParseError, eventStatus, sized } from "../src/index.js";

const rec = (values: Record<string, unknown>) => new Rec(normalize(values));

describe("lenient readers", () => {
  it("treats blanks and nulls as missing", () => {
    const r = rec({ a: "", b: null, c: "  ", d: "x" });
    expect([r.s("a"), r.s("b"), r.s("c"), r.s("missing"), r.s("d")]).toEqual([null, null, null, null, "x"]);
  });
  it("reads numbers from text or JSON numbers", () => {
    const r = rec({ i: "42", f: "3.0", bad: "n/a", zero: "0", num: 133604 });
    expect([r.int("i"), r.int("f"), r.int("bad"), r.year("zero"), r.id("zero"), r.id("num")]).toEqual([42, 3, null, null, null, 133604]);
  });
  it("reads flags in any case", () => {
    const r = rec({ a: "Yes", b: "NO", c: "no", d: "maybe", strLocked: "unlocked" });
    expect([r.bool("a"), r.bool("b"), r.bool("c"), r.bool("d"), r.locked()]).toEqual([true, false, false, null, false]);
  });
  it("reads timestamps as UTC", () => {
    const r = rec({ e: "2026-10-10T11:30:00", tv: "2026-10-10 11:30:00", z: "2026-10-10T11:30:00+02:00", bad: "soon" });
    expect(r.instant("e")?.toISOString()).toBe("2026-10-10T11:30:00.000Z");
    expect(r.instant("tv")?.toISOString()).toBe("2026-10-10T11:30:00.000Z");
    expect(r.instant("z")?.toISOString()).toBe("2026-10-10T09:30:00.000Z");
    expect(r.instant("bad")).toBeNull();
  });
  it("reads dates and times", () => {
    const r = rec({ d: "2026-10-04", zero: "0000-00-00", feb: "2026-02-30", t1: "16:00", t2: "16:30:15", t3: "16:00:00+00:00", tb: "TBC" });
    expect([r.date("d"), r.date("zero"), r.date("feb")]).toEqual(["2026-10-04", null, null]);
    expect([r.time("t1"), r.time("t2"), r.time("t3"), r.time("tb")]).toEqual(["16:00:00", "16:30:15", "16:00:00", null]);
  });
  it("splits comma lists", () => {
    expect(rec({ x: "Arsenal Football Club, AFC, Arsenal FC" }).csv("x")).toEqual(["Arsenal Football Club", "AFC", "Arsenal FC"]);
    expect(rec({ x: null }).csv("x")).toEqual([]);
  });
});

describe("envelopes", () => {
  it("handles every form", () => {
    expect(parseRecords("", "events", "u")).toEqual([]);
    expect(parseRecords('{"events":null}', "events", "u")).toEqual([]);
    expect(parseRecords('{"Message":"No data found"}', "lookup", "u")).toEqual([]);
    expect(parseRecords('{"events":[{"idEvent":"1"}]}', "events", "u")).toHaveLength(1);
    expect(parseRecords('{"renamed":[{"idEvent":"1"}]}', "events", "u")[0]?.["idEvent"]).toBe("1");
    expect(() => parseRecords('{"seasons":"Invalid League ID passed"}', "seasons", "u")).toThrow(ApiMessageError);
    expect(() => parseRecords('{"Message":"Something new"}', "x", "u")).toThrow(ApiMessageError);
    expect(() => parseRecords('{"Message":"Invalid Premium API key: Signup here"}', "x", "u")).toThrow(InvalidApiKeyError);
    expect(() => parseRecords("<html>Cloudflare</html>", "x", "u")).toThrow(ResponseParseError);
  });
});

describe("event status and images", () => {
  it.each([["NS", "NOT_STARTED"], ["2H", "IN_PLAY"], ["Q3", "IN_PLAY"], ["P2", "IN_PLAY"], ["IN7", "IN_PLAY"], ["FT", "FINISHED"],
    ["aet", "FINISHED"], ["PST", "POSTPONED"], [null, "UNKNOWN"], ["???", "UNKNOWN"]] as const)("%s is %s", (code, status) => {
    expect(eventStatus(code)).toBe(status);
  });
  it("sizes only TheSportsDB media images", () => {
    const badge = "https://r2.thesportsdb.com/images/media/team/badge/uyhbfe1612467038.png";
    expect(sized(badge, "tiny")).toBe(`${badge}/tiny`);
    expect(sized(`${badge}/tiny`, "small")).toBe(`${badge}/small`);
    const honour = "https://www.thesportsdb.com/images/media/honour/logo/mxhjar1650460067.png";
    expect(sized(honour, "tiny")).toBe(`${honour}/tiny`);
    const thumb = "https://www.thesportsdb.com/images/sports/soccer.jpg"; // /tiny 404s here
    expect(sized(thumb, "tiny")).toBe(thumb);
    expect(sized(null, "tiny")).toBeNull();
  });
});
