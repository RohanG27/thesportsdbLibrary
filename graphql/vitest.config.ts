import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    include: ["test/**/*.test.ts"],
    // graphql 17 ships separate development/production builds; let Vite resolve graphql-tools too, so the
    // tests and graphql-tools share one copy of graphql (otherwise: "from another module or realm").
    server: { deps: { inline: [/@graphql-tools\//] } },
  },
});
