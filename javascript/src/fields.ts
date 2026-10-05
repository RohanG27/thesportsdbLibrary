/**
 * Lenient readers for a normalized record (every value a string or null). A malformed value
 * becomes null, never an exception. Mirrors Fields.kt, _fields.py and Rec.php.
 * @internal
 */

export type RawRecord = Readonly<Record<string, string | null>>;

const TIME = /^(\d{1,2}):(\d{2})(?::(\d{2}))?/;
const INSTANT = /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?(Z|[+-]\d{2}:?\d{2})?$/;
const NAIVE = /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?$/;
const DESCRIPTION = /^strDescription([A-Z]{2})$/;

export class Rec {
  constructor(readonly raw: RawRecord) {}

  s(key: string): string | null {
    const v = this.raw[key];
    return v != null && v.trim() !== "" ? v : null;
  }

  int(key: string): number | null {
    const v = this.s(key)?.trim();
    if (v == null) return null;
    if (/^-?\d+$/.test(v)) return Number.parseInt(v, 10);
    const f = Number(v);
    return Number.isFinite(f) ? Math.trunc(f) : null;
  }

  /** Ids: 0 means none. */
  id(key: string): number | null {
    const v = this.int(key);
    return v === 0 ? null : v;
  }

  /** Years: 0 means unknown. */
  year(key: string): number | null {
    const v = this.int(key);
    return v != null && v > 0 ? v : null;
  }

  float(key: string): number | null {
    const v = this.s(key)?.trim().replace(/%$/, "");
    if (v == null || v === "") return null;
    const f = Number(v);
    return Number.isFinite(f) ? f : null;
  }

  /** yes/no, true/false, 1/0, any case. */
  bool(key: string): boolean | null {
    switch ((this.s(key) ?? "").trim().toLowerCase()) {
      case "yes": case "true": case "1": case "y": return true;
      case "no": case "false": case "0": case "n": return false;
      default: return null;
    }
  }

  /** strLocked: "locked" / "unlocked". */
  locked(): boolean | null {
    const v = (this.s("strLocked") ?? "").trim().toLowerCase();
    return v === "locked" ? true : v === "unlocked" ? false : null;
  }

  /** A date as YYYY-MM-DD (validated), or null. */
  date(key: string): string | null {
    const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(this.s(key)?.trim() ?? "");
    if (!m) return null;
    const [y, mo, d] = [Number(m[1]), Number(m[2]), Number(m[3])];
    const check = new Date(Date.UTC(y, mo - 1, d));
    return check.getUTCFullYear() === y && check.getUTCMonth() === mo - 1 && check.getUTCDate() === d ? `${m[1]}-${m[2]}-${m[3]}` : null;
  }

  /** A time of day as HH:MM:SS. Accepts 16:00, 16:00:00, 16:00:00+00:00 (the offset is ignored). */
  time(key: string): string | null {
    const m = TIME.exec(this.s(key)?.trim() ?? "");
    if (!m) return null;
    const [h, mi, se] = [Number(m[1]), Number(m[2]), Number(m[3] ?? 0)];
    if (h > 23 || mi > 59 || se > 59) return null;
    return [h, mi, se].map((n) => String(n).padStart(2, "0")).join(":");
  }

  /**
   * A UTC timestamp. Accepts 2026-10-10T11:30:00 and 2026-10-10 11:30:00 (no zone: UTC, as
   * measured) and ISO strings with an offset or Z.
   */
  instant(key: string): Date | null {
    const m = INSTANT.exec(this.s(key)?.trim() ?? "");
    if (!m) return null;
    const zone = m[7] ? (m[7] === "Z" ? "Z" : m[7].replace(/^([+-]\d{2})(\d{2})$/, "$1:$2")) : "Z";
    const d = new Date(`${m[1]}-${m[2]}-${m[3]}T${m[4]}:${m[5]}:${m[6] ?? "00"}${zone}`);
    return Number.isNaN(d.getTime()) ? null : d;
  }

  /** A date-time with no stated zone (e.g. dateUpdated), as written: YYYY-MM-DDTHH:MM:SS. */
  localDateTime(key: string): string | null {
    const m = NAIVE.exec(this.s(key)?.trim() ?? "");
    return m ? `${m[1]}-${m[2]}-${m[3]}T${m[4]}:${m[5]}:${m[6] ?? "00"}` : null;
  }

  /** A comma-separated list, trimmed, blanks dropped. */
  csv(key: string): readonly string[] {
    return (this.s(key) ?? "").split(",").map((p) => p.trim()).filter((p) => p !== "");
  }

  /** Values of prefix1..prefixN that are present. */
  numbered(prefix: string, count: number): readonly string[] {
    const out: string[] = [];
    for (let n = 1; n <= count; n++) {
      const v = this.s(`${prefix}${n}`);
      if (v != null) out.push(v);
    }
    return out;
  }

  /** strDescriptionEN, strDescriptionDE... keyed by language code. */
  descriptions(): Readonly<Record<string, string>> {
    const out: Record<string, string> = {};
    for (const k of Object.keys(this.raw)) {
      const m = DESCRIPTION.exec(k);
      const v = m ? this.s(k) : null;
      if (m?.[1] && v != null) out[m[1]] = v;
    }
    return out;
  }
}
