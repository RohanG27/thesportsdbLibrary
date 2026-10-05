TheSportsDB is an open, crowd-sourced database of sports data and artwork. **v1** works with the free keys and with premium keys.

```
GET https://www.thesportsdb.com/api/v1/json/{apiKey}/{endpoint}.php?{parameters}
```

- **Keys:** `123` is the free key for development and testing; the older `3` also works, with the same limits. Premium keys get full results. The key is part of the URL, so treat v1 URLs as secrets.
- **Free limits:** each endpoint's description gives the records returned to the free and premium keys, measured on 5 Oct 2026, next to the documented limits.
- **Responses:** one JSON object with one key holding an array of records. The key depends on the endpoint, and each endpoint lists its key. The records' values are strings or `null`.
- **No results:** the key with `null`, or an empty body. A rejected parameter comes back as text in place of the records, e.g. `{"seasons":"Invalid League ID passed"}`.
- **Times:** `dateEvent`, `strTime` and `strTimestamp` are **UTC**.
- **Rate limit:** 30 requests a minute on the free keys, 100 premium, 120 business. Over the limit: HTTP 429; wait a minute.

The [guides](../index.html) cover these in detail, with [recipes](../recipes.html) for common tasks.
