#!/usr/bin/env python3
"""Regenerates section 6 ("Fields of each record type") of docs/THESPORTSDB-API-BEHAVIOUR.md
from the recorded fixtures. Endpoint names come from tools/record-fixtures.sh, so the two stay
in sync. The section sits between the FIELDS:BEGIN and FIELDS:END markers."""
import json, pathlib, re

root = pathlib.Path(__file__).resolve().parent.parent
fixtures = root / "src/test/resources/fixtures"
doc = root / "docs/THESPORTSDB-API-BEHAVIOUR.md"
script = (root / "tools/record-fixtures.sh").read_text()

def calls(array):
    body = re.search(array + r"=\((.*?)\n\)", script, re.S).group(1)
    return re.findall(r'"([^|"]+)\|([^"]+)"', body)

def fields(path):
    if not path.exists() or not path.read_text().strip():
        return None
    try:
        data = json.loads(path.read_text())
    except json.JSONDecodeError:  # e.g. an HTML 404 page
        return None
    records = next((v for v in data.values() if isinstance(v, list)), None) if isinstance(data, dict) else None
    if not records:
        return None
    return tuple(sorted({k for r in records for k in r}, key=str.lower))

def render(fs):
    langs = [f[-2:] for f in fs if re.fullmatch(r"strDescription[A-Z]{2}", f)]
    out = []
    for f in fs:
        if re.fullmatch(r"strDescription[A-Z]{2}", f) and len(langs) > 1:
            if f[-2:] == langs[0]:
                out.append("`strDescription{XX}` (" + ", ".join(langs) + ")")
            continue
        out.append(f"`{f}`")
    return ", ".join(out)

def section(title, items):
    groups = {}
    for label, path in items:
        fs = fields(path)
        if fs:
            names = groups.setdefault(fs, [])
            if label not in names:
                names.append(label)
    lines = [f"### {title}", ""]
    for fs, names in groups.items():
        lines.append("- " + ", ".join(f"`{n}`" for n in names) + f" ({len(fs)} field{'' if len(fs) == 1 else 's'}): " + render(fs))
    return "\n".join(lines) + "\n"

v1 = [(call.split("?")[0] + ("?…&" + call.split("&")[-1] if re.search(r"&(poster|badge|description)=1", call) else ""),
       next((p for p in (fixtures / "v1-premium" / f"{name}.json", fixtures / "v1-free" / f"{name}.json") if fields(p)),
            fixtures / "v1-premium" / f"{name}.json")) for name, call in calls("V1")]
v2 = [(call, fixtures / "v2" / f"{name}.json") for name, call in calls("V2")]

generated = (
    "All fields returned, alphabetical, from the recorded responses. Endpoints that return the same shape\n"
    "are grouped. `strDescription{XX}` stands for one field per language code.\n\n"
    + section("v1 (premium key; the free key returns the same fields, except that `all_leagues.php` omits `strLeagueAlternate`)", v1) + "\n"
    + section("v2", v2)
)
text = doc.read_text()
text = re.sub(r"(<!-- FIELDS:BEGIN -->\n).*?(<!-- FIELDS:END -->)", lambda m: m.group(1) + generated + m.group(2), text, flags=re.S)
doc.write_text(text)
print("updated", doc)
