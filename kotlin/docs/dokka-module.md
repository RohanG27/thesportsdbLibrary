# Module thesportsdb-client

A Kotlin/JVM client for the [TheSportsDB](https://www.thesportsdb.com) API, v1 and v2.

Start with [io.github.rohang27.thesportsdb.SportsDbClient] (Kotlin, `suspend`) or [io.github.rohang27.thesportsdb.SportsDbFutures]
(Java, `CompletableFuture`). Every method's documentation names the endpoint it calls, and
every model property names the API field it comes from.

```kotlin
val client = SportsDbClient { apiKey = System.getenv("THESPORTSDB_API_KEY") ?: "123" }
val team = client.v1.lookup.team(133604)
```

# Package io.github.rohang27.thesportsdb

The clients ([SportsDbClient], [SportsDbFutures]), their configuration, the v1 and v2
endpoint groups, [Helpers] for common tasks (v2 or v1 depending on the key), and the
exceptions.

# Package io.github.rohang27.thesportsdb.model

Typed records. Values are parsed leniently: anything missing, blank or malformed is `null`.
Times in `timestamp` properties are UTC. Every model keeps the original fields in `raw`.

# Package io.github.rohang27.thesportsdb.cache

Optional response caching: [io.github.rohang27.thesportsdb.cache.ResponseCache], the in-memory implementation,
and the [io.github.rohang27.thesportsdb.cache.CachePolicy] that sets a time-to-live per kind of data.

# Package io.github.rohang27.thesportsdb.http

The HTTP layer. Implement [io.github.rohang27.thesportsdb.http.HttpTransport] to use another HTTP stack.
