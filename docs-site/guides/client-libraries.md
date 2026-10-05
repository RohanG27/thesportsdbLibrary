# Client libraries

Community libraries that handle the details on these pages: response formats, "no results", errors, UTC times, rate limits and keys.

| Language | Library | Notes |
|---|---|---|
| Kotlin / Java (JVM) | `sportsdb-kotlin` | v1 and v2, typed models, coroutines plus a `CompletableFuture` API for Java |
| Python | `sportsdb-python` | v1 and v2, typed models, blocking and async |
| PHP | `sportsdb-php` | v1 and v2, typed readonly models, no runtime dependencies |
| JavaScript / TypeScript | `sportsdb-js` | v1 and v2, typed models, Node, Deno, Bun and browsers, no dependencies |

All four are tested against the same recorded real responses, and handle every behaviour described in these guides.

You can also generate a client in any language from the OpenAPI descriptions: [v1](openapi/v1.yaml), [v2](openapi/v2.yaml).

## GraphQL

`sportsdb-graphql-schema` is a GraphQL schema for the same data. It has every record type, the relationships between them (an event's teams, lineup and TV channels; a team's squad and schedule; a player's honours) and the queries a client needs. Each query and relationship is annotated with the v1 and v2 endpoints that resolve it, so it can serve as the blueprint for a GraphQL gateway.
