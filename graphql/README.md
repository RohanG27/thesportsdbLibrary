# thesportsdb-graphql-schema

A GraphQL schema for [TheSportsDB](https://www.thesportsdb.com) API: every record type, the relationships between them, and the queries a client needs. Each relationship and query is annotated with the v1 and v2 endpoints that resolve it. It's a schema, not a server: use it to build a GraphQL gateway, generate typed clients, or document the data.

- **21 record types** (`Team`, `Event`, `Player`, `League`, `TvListing`, `LiveScore`…), generated from the same models as the Kotlin, Python, PHP and JavaScript libraries in this repository, so field names match across all of them.
- **Every field is documented** with the API field it comes from and what it means, e.g. `shortName`: "`strTeamShort`: Short code, e.g. `ARS`."
- **Relationships:** `Event.homeTeamDetails`, `Event.lineup`, `Event.tvListings`, `Team.players`, `League.table`, `Player.honours`, `TvListing.eventDetails`, and more.
- **23 queries:** lookups, searches, `eventsOnDay`, `eventsOnLocalDate`, `seasonEvents`, `roundEvents`, `table`, `tvListings`, `liveScores`…
- **`@source` on every query and relationship** names the endpoints that resolve it, for example:

  ```graphql
  homeTeamDetails: Team @source(v1: "lookupteam.php?id={homeTeamId}", v2: "lookup/team/{homeTeamId}")
  ```

  `{placeholders}` refer to the parent record's fields or the field's arguments. Tests check every one against the OpenAPI descriptions in `../docs-site/openapi/`.
- **Typed scalars:**
  - `DateTime` (UTC instants), `Date`, `LocalTime` and `LocalDateTime`, each with `@specifiedBy` pointing at a published specification;
  - `ID` for ids, because some are too big for GraphQL's 32-bit `Int`;
  - `JSONObject` for `raw`, every field as the API sent it.
- **Image sizes:** image fields take an optional `size: ImageSize` (`MEDIUM`, `SMALL`, `TINY`), e.g. `badge(size: TINY)`.

## Install

Not published yet: until the first release, build it from this folder (see [Development](#development)).

```sh
npm install thesportsdb-graphql-schema
```

## Using it

```ts
import { typeDefs, sources } from "thesportsdb-graphql-schema";
// The .graphql file itself: "thesportsdb-graphql-schema/schema.graphql"
```

- `typeDefs` is the SDL string, for GraphQL Yoga, Apollo Server, graphql-js `buildSchema`, or code generators such as GraphQL Code Generator.
- `sources` maps `"Type.field"` to its `{ v1, v2, note }` endpoints, so resolvers can be wired from data.

The package has no runtime dependencies. `graphql` is an optional peer dependency (16 or 17).

A typical server resolves relationships through one of the client libraries, which already handle the API's quirks: rate limits, retries, "no results" forms, free versus premium keys, and UTC times. With the JavaScript library, for example:

```ts
import { SportsDb } from "thesportsdb-client";
const db = new SportsDb({ apiKey: process.env.THESPORTSDB_API_KEY });
const resolvers = {
  Query: { team: (_: unknown, { id }: { id: string }) => db.v1.lookup.team(Number(id)) },
  Event: { homeTeamDetails: (e: { homeTeamId: number | null }) => (e.homeTeamId ? db.v1.lookup.team(e.homeTeamId) : null) },
  // descriptions: [LocalizedText] from the record's descriptions map; badge(size) via sized()...
};
```

The libraries' records use the same field names as this schema, with three shape differences a resolver maps:
- `descriptions` is a map in the libraries and a list of `LocalizedText` here;
- `coordinates` is a pair in the libraries and a `Coordinates` object here;
- `status` uses the same values, as an enum here.

## Example queries

A match, its teams and where to watch it:

```graphql
query MatchDay {
  event(id: "2494052") {
    name
    timestamp
    status
    homeTeamDetails { name badge(size: TINY) venueDetails { name capacity } }
    awayTeamDetails { name badge(size: TINY) }
    tvListings { channel country timestamp }
  }
}
```

Saturday's games in Toronto. Events are filed under their UTC date, so this spans two API days:

```graphql
query LocalDay {
  eventsOnLocalDate(date: "2026-10-04", timeZone: "America/Toronto", sport: "Ice Hockey") {
    name
    timestamp
    homeTeam
    awayTeam
    homeScore
    awayScore
    status
  }
}
```

A league table with each club's squad:

```graphql
query Table {
  table(leagueId: "4328") {
    rank
    team
    points
    form
    teamDetails { shortName players { name position number } }
  }
}
```

A player's career:

```graphql
query Career {
  player(id: "34145937") {
    name
    nationality
    born
    cutout(size: SMALL)
    honours { name season team }
    formerTeams { team joined departed moveType }
  }
}
```

Live scores, with what's on TV:

```graphql
query Live {
  liveScores(sport: "Soccer") {
    homeTeam
    awayTeam
    homeScore
    awayScore
    progress
    updated
    eventDetails { tvListings { channel } }
  }
}
```

## Development

```sh
npm install
npm run generate     # rebuild schema.graphql and src/generated.ts after changing the models, the glossary or src/relations.graphql
npm test             # the schema builds; every field is documented; every @source matches the OpenAPI specs; examples run
npm run build
```

Sources:
- `src/relations.graphql` (hand-written): scalars, `@source`, `Query` and the relationships.
- The record types: generated from `../kotlin/src/main/kotlin/io/github/rohang27/thesportsdb/model/`.
- Field descriptions: from `../docs-site/fields.yaml`.

## License

MIT. See [LICENSE](LICENSE). TheSportsDB's data and artwork are covered by [its own terms](https://www.thesportsdb.com/docs_terms_of_use.php).
