#!/usr/bin/env python3
"""Generates php/src/Model/*.php from the Kotlin models (../src/main/kotlin/sportsdb/model/*.kt), so
the PHP models have exactly the same fields, sources and documentation as the Kotlin (and Python) ones.

    python3 php/tools/gen_models.py          # regenerate
    python3 php/tools/gen_models.py --check  # exit 1 if src/Model is out of date
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

PHP = Path(__file__).resolve().parents[1]
KOTLIN = PHP.parent / "src" / "main" / "kotlin" / "sportsdb" / "model"
OUT = PHP / "src" / "Model"

TYPES = {
    "String": "string", "Long": "int", "Int": "int", "Double": "float", "Boolean": "bool",
    "LocalDate": "\\DateTimeImmutable", "LocalTime": "string", "Instant": "\\DateTimeImmutable",
    "LocalDateTime": "\\DateTimeImmutable", "Socials": "Socials", "PlayerExternalIds": "PlayerExternalIds",
}
DOC_TYPES = {  # phpdoc for arrays
    "List<String>": "list<string>", "Map<String, String>": "array<string, string>", "List<LeagueRef>": "list<LeagueRef>",
}
TYPE_NOTES = {
    "LocalDate": "a date, as midnight UTC",
    "LocalTime": "HH:MM:SS",
    "Instant": "UTC",
    "LocalDateTime": "zone not stated",
}

SPECIAL = {
    ("Team", "leagues"): "self::leagues($r)",
    ("Player", "externalIds"): "new PlayerExternalIds($r->id('idAPIfootball'), $r->s('idESPN'), $r->s('idGoogle'), "
                               "$r->s('idTransferMkt'), $r->s('idWikidata'), $r->s('intSoccerXMLTeamID'))",
}

EXTRAS = {
    "League": ["description"], "Team": ["description", "leagues"], "Player": ["description"],
    "Venue": ["description", "coordinates"], "Event": ["status"], "LiveScore": ["status"], "PlayerStat": ["numericValue"],
}

EXTRA_CODE = {
    "description": '''
    /** The English description. */
    public function description(): ?string
    {
        return $this->descriptions['EN'] ?? null;
    }
''',
    "status": '''
    /** The status code read as a broad status. */
    public function status(): EventStatus
    {
        return EventStatus::of($this->statusCode);
    }
''',
    "coordinates": '''
    /** @return array{float, float}|null `map` as [latitude, longitude], when it holds coordinates */
    public function coordinates(): ?array
    {
        $parts = array_map('trim', explode(',', $this->map ?? ''));
        return \\count($parts) === 2 && is_numeric($parts[0]) && is_numeric($parts[1]) ? [(float) $parts[0], (float) $parts[1]] : null;
    }
''',
    "numericValue": '''
    /** `value` as a number, when it is one. */
    public function numericValue(): ?float
    {
        return $this->value !== null && is_numeric(trim($this->value)) ? (float) trim($this->value) : null;
    }
''',
    "leagues": '''
    /** @return list<LeagueRef> */
    private static function leagues(Rec $r): array
    {
        $out = [];
        for ($n = 1; $n <= 7; $n++) {
            $suffix = $n === 1 ? '' : (string) $n;
            if (($id = $r->id("idLeague$suffix")) !== null) {
                $out[] = new LeagueRef($id, $r->s("strLeague$suffix"));
            }
        }
        return $out;
    }
''',
}

READERS = [
    (r'long\("idDupe"\)\?\.takeIf \{ it != 0L \}', "$r->id('idDupe')"),
    (r"(?<!->)\bstr\(", "$r->s("), (r"(?<!->)\bid\(", "$r->id("), (r"(?<!->)\bint\(", "$r->int("), (r"(?<!->)\byear\(", "$r->year("),
    (r"(?<!->)\bdouble\(", "$r->float("), (r"(?<!->)\bbool\(", "$r->bool("), (r"(?<!->)\bdate\(", "$r->date("), (r"(?<!->)\btime\(", "$r->time("),
    (r"(?<!->)\binstant\(", "$r->instant("), (r"(?<!->)\blocalDateTime\(", "$r->localDateTime("), (r"(?<!->)\blist\(", "$r->csv("),
    (r"(?<!->)\bnumbered\(", "$r->numbered("), (r"(?<!->)\bdescriptions\(\)", "$r->descriptions()"), (r"(?<!->)\blocked\(\)", "$r->locked()"),
    (r"(?<!->)\bsocials\(\)", "Socials::fromRec($r)"), (r"\?:", "??"), (r'"([^"]*)"', r"'\1'"),
]


def php_expr(cls: str, prop: str, kotlin: str) -> str:
    if (cls, prop) in SPECIAL:
        return SPECIAL[(cls, prop)]
    expr = kotlin
    for pattern, repl in READERS:
        expr = re.sub(pattern, repl, expr)
    if cls == "Season" and prop == "name":
        expr = "$r->s('strSeason') ?? ''"
    return expr


def mapper_args(src: str, name: str) -> dict[str, str]:
    """The `prop = expr` arguments of `JsonObject.to{name}()`, split at top-level commas."""
    start = src.index(f"internal fun JsonObject.to{name}()")
    open_at = src.index(f"{name}(", src.index("=", start)) + len(name)
    depth, i = 0, open_at
    while True:
        ch = src[i]
        depth += ch in "([{"
        depth -= ch in ")]}"
        if depth == 0:
            break
        i += 1
    body = src[open_at + 1:i]
    args, depth, cur = [], 0, ""
    for ch in body:
        if ch in "([{":
            depth += 1
        elif ch in ")]}":
            depth -= 1
        if ch == "," and depth == 0:
            args.append(cur)
            cur = ""
        else:
            cur += ch
    args.append(cur)
    out = {}
    for a in args:
        if "=" in a and a.strip():
            k, v = a.split("=", 1)
            out[k.strip()] = " ".join(v.split())
    return out


def main() -> int:
    src = "".join(p.read_text() for p in sorted(KOTLIN.glob("*.kt")))
    classes = re.findall(r"((?:/\*\*(?:(?!\*/).)*\*/\n)?)public class (\w+) internal constructor\(((?:(?!public class).)*?)\n\) : ApiRecord\(raw\) \{\n"
                         r"    override fun toString\(\): String = \"\w+\((.*?)\)\"", src, re.S)
    mappers = {name: mapper_args(src, name) for name in re.findall(r"internal fun JsonObject\.to(\w+)\(\)", src)}
    files: dict[Path, str] = {}
    for doc, name, body, summary in classes:
        props = re.findall(r"((?:/\*\*(?:(?!\*/).)*\*/\s*)?)public val (\w+): ([\w<>, ?]+),", body, re.S)
        mapping = mappers[name]
        decls, assigns = [], []
        for pdoc, prop, ktype in props:
            base = ktype.rstrip("?")
            nullable = ktype.endswith("?")
            text = " ".join(l.strip(" */") for l in pdoc.strip().splitlines()).strip()
            if base in DOC_TYPES:
                phptype = "array"
                doc_line = f"/** @var {DOC_TYPES[base]} {text} */"
            else:
                phptype = ("?" if nullable else "") + TYPES[base]
                note = TYPE_NOTES.get(base)
                doc_line = f"/** {text}{' (' + note + ')' if note else ''} */" if text or note else ""
            decls.append((f"    {doc_line}\n" if doc_line else "") + f"    public {phptype} ${prop};")
            assigns.append(f"        $this->{prop} = {php_expr(name, prop, mapping[prop])};")
        summary_php = re.sub(r"\$(\w+)", r"{$this->fmt($this->\1)}", summary)
        class_doc = "\n".join(" * " + l.strip(" */") if l.strip(" */") else " *" for l in doc.strip().splitlines()
                              if l.strip() not in ("/**", "*/")) if doc.strip() else f" * {name} record."
        extras = "".join(EXTRA_CODE[e] for e in EXTRAS.get(name, []))
        uses = ["use SportsDb\\Internal\\Rec;"]
        files[OUT / f"{name}.php"] = f"""<?php

// GENERATED by php/tools/gen_models.py from the Kotlin model {name}. Do not edit.

declare(strict_types=1);

namespace SportsDb\\Model;

{chr(10).join(uses)}

/**
{class_doc}
 */
final readonly class {name} extends ApiRecord
{{
{chr(10).join(decls)}

    /**
     * @internal Records are built by the client from API responses.
     * @param array<string, string|null> $raw
     */
    public function __construct(array $raw)
    {{
        parent::__construct($raw);
        $r = new Rec($raw);
{chr(10).join(assigns)}
    }}

    public function __toString(): string
    {{
        return "{name}({summary_php})";
    }}
{extras}}}
"""
    if "--check" in sys.argv:
        stale = [p.name for p, t in files.items() if not p.exists() or p.read_text() != t]
        if stale:
            print("out of date: " + ", ".join(stale) + " (run python3 php/tools/gen_models.py)")
            return 1
        return 0
    OUT.mkdir(exist_ok=True)
    for path, text in files.items():
        path.write_text(text)
    print(f"wrote {len(files)} models to {OUT.relative_to(PHP.parent)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
