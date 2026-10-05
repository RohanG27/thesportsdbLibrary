<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\V2;

use RohanG27\TheSportsDb\Cache\Freshness;
use RohanG27\TheSportsDb\Internal\Requester;
use RohanG27\TheSportsDb\Model\Event;

/** schedule/.... Event times are UTC. */
final class Schedule
{
    /** @internal */
    public function __construct(private readonly Requester $r)
    {
    }

    /** @return list<Event> */
    public function leagueNext(int $leagueId): array
    {
        return $this->get(['next', 'league', $leagueId]);
    }

    /** @return list<Event> */
    public function leaguePrevious(int $leagueId): array
    {
        return $this->get(['previous', 'league', $leagueId]);
    }

    /** @return list<Event> */
    public function teamNext(int $teamId): array
    {
        return $this->get(['next', 'team', $teamId]);
    }

    /** @return list<Event> */
    public function teamPrevious(int $teamId): array
    {
        return $this->get(['previous', 'team', $teamId]);
    }

    /** @return list<Event> */
    public function venueNext(int $venueId): array
    {
        return $this->get(['next', 'venue', $venueId]);
    }

    /** @return list<Event> */
    public function venuePrevious(int $venueId): array
    {
        return $this->get(['previous', 'venue', $venueId]);
    }

    /** @return list<Event> a team's whole schedule, past and future, across all competitions */
    public function teamFull(int $teamId): array
    {
        return $this->get(['full', 'team', $teamId]);
    }

    /** @return list<Event> a league's whole season in one call; the current season is League::$currentSeason */
    public function leagueSeason(int $leagueId, string $season): array
    {
        return $this->get(['league', $leagueId, $season]);
    }

    /**
     * @param list<string|int> $path
     * @return list<Event>
     */
    private function get(array $path): array
    {
        return $this->r->v2('schedule', Freshness::Medium, static fn ($x) => new Event($x), ['schedule', ...$path]);
    }
}
