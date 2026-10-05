<?php

declare(strict_types=1);

namespace SportsDb\V2;

use SportsDb\Cache\Freshness;
use SportsDb\Internal\Requester;
use SportsDb\Model\Player;
use SportsDb\Model\Season;
use SportsDb\Model\Team;

/** list/... */
final class Lists
{
    /** @internal */
    public function __construct(private readonly Requester $r)
    {
    }

    /** @return list<Team> a league's teams with badges and colours, but no alternate names */
    public function teams(int $leagueId): array
    {
        return $this->r->v2('list', Freshness::Slow, static fn ($x) => new Team($x), ['list', 'teams', $leagueId]);
    }

    /** @return list<Season> */
    public function seasons(int $leagueId): array
    {
        return $this->r->v2('list', Freshness::Slow, static fn ($x) => new Season($x), ['list', 'seasons', $leagueId]);
    }

    /** @return list<Player> */
    public function players(int $teamId): array
    {
        return $this->r->v2('list', Freshness::Slow, static fn ($x) => new Player($x), ['list', 'players', $teamId]);
    }
}
