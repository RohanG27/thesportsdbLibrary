import type { Rec, RawRecord } from "./fields.js";

/**
 * Fields every record has. Records are frozen plain objects built by the client from one API
 * record; `kind` names the record type and `raw` holds every original field.
 */
export interface ApiRecord {
  readonly kind: string;
  /** Every field as the API sent it (blank strings as null). Use it for unmodelled fields: team.raw["strKeywords"]. */
  readonly raw: RawRecord;
}

/** True when both are the same kind of record, built from the same API fields. */
export function sameRecord(a: ApiRecord | null | undefined, b: ApiRecord | null | undefined): boolean {
  if (!a || !b || a.kind !== b.kind) return false;
  const ka = Object.keys(a.raw);
  return ka.length === Object.keys(b.raw).length && ka.every((k) => a.raw[k] === b.raw[k]);
}

/** Web and social links; often without a scheme (www.facebook.com/Arsenal). */
export interface Socials {
  readonly website: string | null; // strWebsite
  readonly facebook: string | null; // strFacebook
  readonly twitter: string | null; // strTwitter
  readonly instagram: string | null; // strInstagram
  readonly youtube: string | null; // strYoutube
  readonly rss: string | null; // strRSS
}

/** A league a team plays in: one of idLeague/strLeague .. idLeague7/strLeague7. */
export interface LeagueRef {
  readonly id: number;
  readonly name: string | null;
}

/** Ids of the same player in other databases. */
export interface PlayerExternalIds {
  readonly apiFootball: number | null; // idAPIfootball
  readonly espn: string | null; // idESPN
  readonly google: string | null; // idGoogle, e.g. /g/11cpprmr81
  readonly transfermarkt: string | null; // idTransferMkt
  readonly wikidata: string | null; // idWikidata, e.g. Q9144353
  readonly soccerXmlTeam: string | null; // intSoccerXMLTeamID
}

/** A broad reading of a strStatus code, so you don't need every sport's codes. */
export type EventStatus = "NOT_STARTED" | "IN_PLAY" | "FINISHED" | "POSTPONED" | "CANCELLED" | "ABANDONED" | "UNKNOWN";

const STATUS: Record<string, EventStatus> = {};
for (const [status, codes] of [
  ["NOT_STARTED", "NS,TBD,NOT STARTED,SCHEDULED"],
  ["FINISHED", "FT,AET,PEN,AOT,AP,FINISHED,MATCH FINISHED,FINAL,ENDED,AWD,WO"],
  ["POSTPONED", "PST,POSTPONED,DELAYED,SUSP,INTERRUPTED"],
  ["CANCELLED", "CANC,CANCELLED,CANCELED"],
  ["ABANDONED", "ABD,ABANDONED"],
  ["IN_PLAY", "1H,2H,HT,ET,BT,P,LIVE,INT,BREAK,Q1,Q2,Q3,Q4,OT,P1,P2,P3,SO,IN PROGRESS"],
] as const) {
  for (const code of codes.split(",")) STATUS[code] = status;
}

/** Classifies a raw code (NS, 2H, Q3, P2, IN4, FT, PST...); null and unknown codes give "UNKNOWN". */
export function eventStatus(code: string | null | undefined): EventStatus {
  const c = (code ?? "").trim().toUpperCase();
  if (c === "") return "UNKNOWN";
  return STATUS[c] ?? (/^IN\d+$/.test(c) ? "IN_PLAY" : "UNKNOWN"); // IN1.. = baseball innings
}

/** Image sizes TheSportsDB serves by appending a path suffix. */
export type ImageSize = "medium" | "small" | "tiny";

/**
 * The same image at a smaller size. Only r2.thesportsdb.com and www.thesportsdb.com/images/media/
 * images support the suffixes (others return 404, verified 5 Oct 2026); other URLs and null are
 * returned unchanged.
 */
export function sized<T extends string | null | undefined>(url: T, size: ImageSize): T {
  if (url == null || !(url.startsWith("https://r2.thesportsdb.com/") || url.startsWith("https://www.thesportsdb.com/images/media/"))) {
    return url;
  }
  return (url.replace(/\/(medium|small|tiny)$/, "") + "/" + size) as T;
}

/** @internal */
export function socials(r: Rec): Socials {
  return Object.freeze({
    website: r.s("strWebsite"), facebook: r.s("strFacebook"), twitter: r.s("strTwitter"),
    instagram: r.s("strInstagram"), youtube: r.s("strYoutube"), rss: r.s("strRSS"),
  });
}

/** @internal */
export function leagueRefs(r: Rec): readonly LeagueRef[] {
  const out: LeagueRef[] = [];
  for (let n = 1; n <= 7; n++) {
    const suffix = n === 1 ? "" : String(n);
    const id = r.id(`idLeague${suffix}`);
    if (id != null) out.push(Object.freeze({ id, name: r.s(`strLeague${suffix}`) }));
  }
  return Object.freeze(out);
}

/** @internal */
export function externalIds(r: Rec): PlayerExternalIds {
  return Object.freeze({
    apiFootball: r.id("idAPIfootball"), espn: r.s("idESPN"), google: r.s("idGoogle"),
    transfermarkt: r.s("idTransferMkt"), wikidata: r.s("idWikidata"), soccerXmlTeam: r.s("intSoccerXMLTeamID"),
  });
}

/** @internal `map` as [latitude, longitude], when it holds coordinates. */
export function coordinates(map: string | null): readonly [number, number] | null {
  const parts = (map ?? "").split(",").map((p) => p.trim());
  if (parts.length !== 2 || parts[0] === "" || parts[1] === "") return null;
  const [lat, lon] = [Number(parts[0]), Number(parts[1])];
  return Number.isFinite(lat) && Number.isFinite(lon) ? Object.freeze([lat, lon] as const) : null;
}

/** @internal */
export function numeric(value: string | null): number | null {
  const v = value?.trim();
  if (v == null || v === "") return null;
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
}
