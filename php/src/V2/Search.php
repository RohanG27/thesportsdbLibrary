<?php

declare(strict_types=1);

namespace SportsDb\V2;

use SportsDb\Cache\Freshness;
use SportsDb\Internal\Requester;
use SportsDb\Model\Event;
use SportsDb\Model\League;
use SportsDb\Model\Player;
use SportsDb\Model\Team;
use SportsDb\Model\Venue;

/** search/...: up to about 10 results, summary fields only. Ids arrive as JSON numbers here. */
final class Search
{
    /** @internal */
    public function __construct(private readonly Requester $r)
    {
    }

    /** @return list<League> */
    public function leagues(string $name): array
    {
        return $this->r->v2('search', Freshness::Slow, static fn ($x) => new League($x), ['search', 'league', $name]);
    }

    /** @return list<Team> */
    public function teams(string $name): array
    {
        return $this->r->v2('search', Freshness::Slow, static fn ($x) => new Team($x), ['search', 'team', $name]);
    }

    /** @return list<Player> */
    public function players(string $name): array
    {
        return $this->r->v2('search', Freshness::Slow, static fn ($x) => new Player($x), ['search', 'player', $name]);
    }

    /** @return list<Event> needs the exact stored event name; a loose "Arsenal vs Chelsea" finds nothing */
    public function events(string $name): array
    {
        return $this->r->v2('search', Freshness::Medium, static fn ($x) => new Event($x), ['search', 'event', $name]);
    }

    /** @return list<Venue> */
    public function venues(string $name): array
    {
        return $this->r->v2('search', Freshness::Slow, static fn ($x) => new Venue($x), ['search', 'venue', $name]);
    }
}
