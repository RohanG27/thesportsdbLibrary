<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\V2;

use RohanG27\TheSportsDb\Cache\Freshness;
use RohanG27\TheSportsDb\Internal\Requester;
use RohanG27\TheSportsDb\Model\Player;
use RohanG27\TheSportsDb\Model\Season;
use RohanG27\TheSportsDb\Model\SeasonPoster;
use RohanG27\TheSportsDb\Model\Team;

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

    /** @return list<SeasonPoster> every season poster and badge uploaded for a league, with the uploader */
    public function seasonPosters(int $leagueId): array
    {
        return $this->r->v2('list', Freshness::Slow, static fn ($x) => new SeasonPoster($x), ['list', 'seasonposters', $leagueId]);
    }

    /** @return list<Player> */
    public function players(int $teamId): array
    {
        return $this->r->v2('list', Freshness::Slow, static fn ($x) => new Player($x), ['list', 'players', $teamId]);
    }
}
