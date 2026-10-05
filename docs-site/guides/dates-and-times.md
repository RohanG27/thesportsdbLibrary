# Dates and times

## Everything is UTC

| Field | Format | Zone |
|---|---|---|
| `strTimestamp` (events, schedules, live scores) | `2026-10-10T11:30:00` | **UTC**, though the text has no zone |
| `dateEvent`, `strTime` | `2026-10-10`, `11:30:00` | **UTC** |
| `strTimeStamp` (TV listings; note the capital S) | `2026-10-10 11:30:00` (a space, not `T`) | **UTC** |
| `dateEventLocal`, `strTimeLocal` | `2026-10-10`, `16:30:00` | the venue's local time; often missing for future events |
| `strEventTime` (live scores) | `16:00` | |
| `dateUpdated`, `updated`, `date` | `2026-09-24 10:00:07` | not stated |

For example, a game in Los Angeles at 4 pm (`strTimeLocal: 16:00:00`) has `strTimestamp: …T23:00:00`: 4 pm PDT is 23:00 UTC.

## "Saturday's games" in your time zone

`eventsday.php?d=` returns the events whose **UTC** date is `d`. In the Americas, evening games fall on the **next** UTC day, so one request for a local date misses some games and includes some from the day before.

To get the events of a local calendar day:

1. Convert the start and end of the local day to UTC. For Toronto on 4 Oct 2026, that's 04:00 UTC on 4 Oct to 04:00 UTC on 5 Oct.
2. Request every UTC date in that range: `eventsday.php?d=2026-10-04` and `?d=2026-10-05`.
3. Keep the events whose `strTimestamp` falls inside the range.

For time zones ahead of UTC (e.g. Tokyo), the range starts on the previous UTC date instead.

## Event status

`strStatus` is a short code. The codes differ by sport. These were seen in responses:

| Meaning | Codes |
|---|---|
| Not started | `NS` |
| In play (soccer) | `1H`, `HT`, `2H` |
| In play (basketball, American football) | `Q1`–`Q4`, `OT` |
| In play (ice hockey) | `P1`–`P3`, `OT`, `SO` |
| In play (baseball) | `IN1`, `IN2`, … (innings) |
| Finished | `FT`, `AET` (after extra time), `PEN` (after penalties) |
| Postponed | `PST` |

Other codes can appear. Treat a code you don't recognise as unknown, rather than as finished or not started.

- **Older events often have no `strStatus`**, even with a final score.
- `strPostponed` (`yes`/`no`) is a separate flag.
- In live scores, `strProgress` is the minute, or `Final`.

## Live scores can be stale

The live feeds (v1 `livescore.php`, v2 `livescore/…`) can keep listing a game after it ends. On 5 Oct 2026 the feed still showed an ice hockey game from the previous day, in its third period. Check `updated` before showing a score as live.
