# Module sportsdb-kotlin

A Kotlin/JVM client for the [TheSportsDB](https://www.thesportsdb.com) API, v1 and v2.

Start with [sportsdb.SportsDbClient] (Kotlin, `suspend`) or [sportsdb.SportsDbFutures]
(Java, `CompletableFuture`). Every method's documentation names the endpoint it calls, and
every model property names the API field it comes from.

```kotlin
val client = SportsDbClient { apiKey = System.getenv("THESPORTSDB_API_KEY") ?: "123" }
val team = client.v1.lookup.team(133604)
```

# Package sportsdb

The clients ([SportsDbClient], [SportsDbFutures]), their configuration, the v1 and v2
endpoint groups, [Helpers] for common tasks (v2 or v1 depending on the key), and the
exceptions.

# Package sportsdb.model

Typed records. Values are parsed leniently: anything missing, blank or malformed is `null`.
Times in `timestamp` properties are UTC. Every model keeps the original fields in `raw`.

# Package sportsdb.cache

Optional response caching: [sportsdb.cache.ResponseCache], the in-memory implementation,
and the [sportsdb.cache.CachePolicy] that sets a time-to-live per kind of data.

# Package sportsdb.http

The HTTP layer. Implement [sportsdb.http.HttpTransport] to use another HTTP stack.
