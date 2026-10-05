/**
 * The GraphQL schema for TheSportsDB API, as SDL.
 *
 * ```ts
 * import { typeDefs, sources } from "sportsdb-graphql-schema";
 * import { createSchema, createYoga } from "graphql-yoga";
 * const yoga = createYoga({ schema: createSchema({ typeDefs, resolvers }) });
 * ```
 *
 * The file itself is also exported: `sportsdb-graphql-schema/schema.graphql`.
 */
export { typeDefs, sources, type Source } from "./generated.js";
