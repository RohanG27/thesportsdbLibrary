# Test fixtures

Real response bodies from TheSportsDB, recorded 5 Oct 2026 with `tools/record-fixtures.sh`.
They contain response bodies only, never API keys.

- `v1-free/`: v1 with the public free key `123`.
- `v1-premium/`: the same v1 calls with a premium key (full result sizes).
- `v2/`: v2 with a premium key, plus `invalid_key.json` (the response to a non-premium key).

Re-record with `tools/record-fixtures.sh`; set `THESPORTSDB_PREMIUM_KEY` to include the premium sets.
Data changes over time (live scores, schedules), so tests assert on shapes and stable facts, not on today's scores.
