# Client libraries

Community libraries that handle the details on these pages: response formats, "no results", errors, UTC times, rate limits and keys.

| Language | Library | Notes | API reference |
|---|---|---|---|
| Kotlin / Java (JVM) | `io.github.rohang27:thesportsdb-client` | v1 and v2, typed models, coroutines plus a `CompletableFuture` API for Java | [Dokka](api/kotlin/index.html) |
| Python | `thesportsdb-client` | v1 and v2, typed models, blocking and async | [pdoc](api/python/thesportsdb_client.html) |
| PHP | `rohang27/thesportsdb-client` | v1 and v2, typed readonly models, no runtime dependencies | [phpDocumentor](api/php/index.html) |
| JavaScript / TypeScript | `thesportsdb-client` | v1 and v2, typed models, Node, Deno, Bun and browsers, no dependencies | [TypeDoc](api/javascript/index.html) |

The source is at [github.com/RohanG27/thesportsdbLibrary](https://github.com/RohanG27/thesportsdbLibrary).

All four are tested against the same recorded real responses, and handle every behaviour described in these guides.

You can also generate a client in any language from the OpenAPI descriptions: [v1](openapi/v1.yaml), [v2](openapi/v2.yaml).

## GraphQL

`thesportsdb-graphql-schema` is a GraphQL schema for the same data. It has every record type, the relationships between them (an event's teams, lineup and TV channels; a team's squad and schedule; a player's honours) and the queries a client needs. Each query and relationship is annotated with the v1 and v2 endpoints that resolve it, so it can serve as the blueprint for a GraphQL gateway.
