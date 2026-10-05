# thesportsdbLibrary

Client libraries, a GraphQL schema and API documentation for [TheSportsDB](https://www.thesportsdb.com), the open, crowd-sourced sports database. They cover v1 and v2, built from real recorded responses and kept at parity with each other.

| Folder | What | Highlights |
|---|---|---|
| [`kotlin/`](kotlin/README.md) | **Kotlin/JVM** client (`io.github.rohang27:thesportsdb-client`) | coroutines, a `CompletableFuture` API for Java, Dokka docs |
| [`python/`](python/README.md) | **Python** client (`thesportsdb-client`) | blocking and async (asyncio/trio), strict typing, httpx |
| [`php/`](php/README.md) | **PHP** client (`rohang27/thesportsdb-client`) | PHP 8.2+, readonly models, no runtime dependencies |
| [`javascript/`](javascript/README.md) | **JavaScript/TypeScript** client (`thesportsdb-client`) | Node 18+, Deno, Bun and browsers; no dependencies |
| [`graphql/`](graphql/README.md) | **GraphQL schema** (`thesportsdb-graphql-schema`) | every record type and relationship, each annotated with the endpoints that resolve it |
| [`docs-site/`](docs-site/README.md) | **API documentation**: guides, an OpenAPI 3.1 reference, and each library's API reference | live at **https://rohang27.github.io/thesportsdbLibrary/** |
| [`docs/`](docs/) | Shared references | [measured API behaviour](docs/THESPORTSDB-API-BEHAVIOUR.md), [endpoint map](docs/ENDPOINTS.md), [library parity checklist](docs/LIBRARY-PARITY.md) |

## What every library does

- **Every endpoint:** each documented v1 and v2 endpoint, plus the useful undocumented ones, as one method each.
- **Typed models:** read-only and parsed leniently. Blank or malformed values become null instead of failing the call, timestamps are UTC, and the original fields stay available.
- **The API's quirks are handled:**
  - per-endpoint record keys;
  - the several forms "no results" takes;
  - errors sent as text where records belong;
  - two timestamp formats;
  - every sport's status codes;
  - round stage codes.
- **Safe by default:**
  - a rate limiter matched to your key, with retries for 429s, 5xx and network errors;
  - a 30-second timeout;
  - API keys never appear in errors or cache keys.
- **Helpers for common tasks** (season fixtures, a team's schedule, events on a local calendar day, live scores, TV listings) that use v2 with a premium key and v1 with the free key.

The [parity checklist](docs/LIBRARY-PARITY.md) lists every rule the libraries share and where each one is implemented and tested.

## Quick start

```kotlin
val arsenal = SportsDbClient().v1.lookup.team(133604)                        // Kotlin
```
```python
arsenal = SportsDB().v1.lookup.team(133604)                                  # Python
```
```php
$arsenal = (new SportsDb())->v1->lookup->team(133604);                       // PHP
```
```js
const arsenal = await new SportsDb().v1.lookup.team(133604);                 // JavaScript
```

With no key, the libraries use TheSportsDB's free key `123` (v1 only, small result limits). Pass a premium key for v2 and full results.

## Development

- **Running tests:** each folder is a self-contained project with its own README, build and tests.
- **Shared recordings:** the real API responses all four libraries test against live in `kotlin/src/test/resources/fixtures/`. Re-record them with [`tools/record-fixtures.sh`](tools/record-fixtures.sh), then copy them into each project with its `sync-fixtures` script.
- **CI** (`.github/workflows/ci.yml`) builds and tests every project on each push. A weekly job runs the live tests against the real API, using the `THESPORTSDB_API_KEY` repository secret if set.

## License

MIT. See [LICENSE](LICENSE). TheSportsDB's data and artwork are covered by [its own terms](https://www.thesportsdb.com/docs_terms_of_use.php), not by this license. This project isn't affiliated with TheSportsDB.
