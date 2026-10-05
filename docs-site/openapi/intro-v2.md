**v2** is for premium keys. It adds full team schedules, whole seasons in one call, TV listings by country, sport or channel, and documented live scores.

```
GET https://www.thesportsdb.com/api/v2/json/{group}/{name}/{parameter}
X-API-KEY: your-premium-key
```

- **Keys:** sent in the `X-API-KEY` header. The free keys get HTTP 400.
- **Responses:** one JSON object whose key is the endpoint's group: `search`, `lookup`, `list`, `filter`, `all`, `schedule` or `livescore`. Values are strings or `null`, except that `search/...` sends ids as numbers.
- **No results:** `{"Message":"No data found"}`, with HTTP 200.
- **Limits:** each endpoint's description gives the documented limit and the records measured on 5 Oct 2026. They differ: several endpoints return more than documented.
- **Times:** `dateEvent`, `strTime` and `strTimestamp` are **UTC**.
- **Not in v2:** league tables. Use v1's `lookuptable.php` with your premium key.

The guides cover these in detail, with recipes for common tasks.
