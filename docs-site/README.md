# TheSportsDB API documentation

Proposed replacement documentation for TheSportsDB's API. TheSportsDB also publishes its own OpenAPI files (`/api/spec/v1/openapi.yaml`, `/api/spec/v2/openapi.yaml`); ours cover endpoints, parameters and fields those files miss, and leave out fields the API no longer sends (see `guides/known-issues.md`).

It consists of guides plus an OpenAPI 3.1 reference for v1 and v2. It's written neutrally, so TheSportsDB could adopt it, and every count, field and example comes from real responses recorded on 5 Oct 2026.

| Part | Source | Built by |
|---|---|---|
| `openapi/v1.yaml`, `openapi/v2.yaml` | the endpoint table in `tools/build_openapi.py`, the recorded responses (`../src/test/resources/fixtures/`), `fields.yaml`, `openapi/intro-*.md` | `tools/build_openapi.py` (generated: don't edit by hand) |
| `fields.yaml` | a hand-written glossary of every record field | — (the build fails if a recorded field is missing) |
| `guides/*.md` | hand-written | — |
| `site/` (git-ignored) | everything above | `tools/build_site.py` (Python Markdown plus the Redocly CLI, which needs Node) |

## Building

```sh
python3 -m venv .venv && .venv/bin/pip install pyyaml openapi-spec-validator markdown
.venv/bin/python tools/build_openapi.py        # specs, from the fixtures
npx @redocly/cli lint openapi/v1.yaml openapi/v2.yaml
.venv/bin/python tools/build_site.py           # site/index.html and site/reference/v1.html, v2.html
```

`site/` is static: open `site/index.html`, or host the folder anywhere.

## Keeping it current

The documentation follows the same evidence as the client libraries:
1. Re-record responses with `../tools/record-fixtures.sh`.
2. Rebuild the specs. New fields appear automatically, and the build fails until they're described in `fields.yaml`.
3. Update the guides and `../docs/THESPORTSDB-API-BEHAVIOUR.md` for any change in behaviour.
