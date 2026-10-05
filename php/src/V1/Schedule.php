<?php

declare(strict_types=1);

namespace SportsDb\V1;

use SportsDb\Cache\Freshness;
use SportsDb\Internal\Requester;
use SportsDb\Model\Event;

/** Schedules and results. Event times are UTC. */
final class Schedule
{
    /** @internal */
    public function __construct(private readonly Requester $r)
    {
    }

    /** @return list<Event> eventsnext.php?id=. Free keys: 1, home games only. */
    public function teamNext(int $teamId): array
    {
        return $this->events('eventsnext.php', 'events', ['id' => $teamId]);
    }

    /** @return list<Event> eventslast.php?id= (record key "results"). Free keys: 1, home games only. */
    public function teamLast(int $teamId): array
    {
        return $this->events('eventslast.php', 'results', ['id' => $teamId]);
    }

    /** @return list<Event> eventsnextleague.php?id=. Free keys: 1. */
    public function leagueNext(int $leagueId): array
    {
        return $this->events('eventsnextleague.php', 'events', ['id' => $leagueId]);
    }

    /** @return list<Event> eventspastleague.php?id=. Free keys: 1. */
    public function leaguePast(int $leagueId): array
    {
        return $this->events('eventspastleague.php', 'events', ['id' => $leagueId]);
    }

    /** @return list<Event> eventsday.php?d=: every event on a UTC day, optionally one sport or league. Free keys: 3. */
    public function day(\DateTimeInterface $date, ?string $sport = null, ?int $leagueId = null, ?string $leagueName = null): array
    {
        if ($leagueId !== null && $leagueName !== null) {
            throw new \InvalidArgumentException('Pass leagueId or leagueName, not both');
        }
        return $this->events('eventsday.php', 'events', ['d' => $date, 's' => $sport, 'l' => $leagueId ?? $leagueName]);
    }

    /** @return list<Event> eventsseason.php?id=&s=: a whole season (premium). Free keys: 5. */
    public function season(int $leagueId, string $season): array
    {
        return $this->events('eventsseason.php', 'events', ['id' => $leagueId, 's' => $season]);
    }

    /**
     * eventsround.php?id=&r=&s=: one round. **Undocumented and free keys only**: premium keys get
     * HTTP 404. $db->helpers->roundEvents() picks the right route for any key.
     *
     * @return list<Event>
     */
    public function round(int $leagueId, int $round, string $season): array
    {
        return $this->events('eventsround.php', 'events', ['id' => $leagueId, 'r' => $round, 's' => $season]);
    }

    /**
     * @param array<string, scalar|\DateTimeInterface|null> $params
     * @return list<Event>
     */
    private function events(string $endpoint, string $key, array $params): array
    {
        return $this->r->v1($endpoint, $key, Freshness::Medium, static fn ($x) => new Event($x), $params);
    }
}
