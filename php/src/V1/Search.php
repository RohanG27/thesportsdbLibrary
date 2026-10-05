<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\V1;

use RohanG27\TheSportsDb\Cache\Freshness;
use RohanG27\TheSportsDb\Internal\Requester;
use RohanG27\TheSportsDb\Model\Event;
use RohanG27\TheSportsDb\Model\Player;
use RohanG27\TheSportsDb\Model\Team;
use RohanG27\TheSportsDb\Model\Venue;

/** Search endpoints. Free keys: one result each. */
final class Search
{
    /** @internal */
    public function __construct(private readonly Requester $r)
    {
    }

    /** @return list<Team> searchteams.php?t= */
    public function teams(string $name): array
    {
        return $this->r->v1('searchteams.php', 'teams', Freshness::Slow, static fn ($x) => new Team($x), ['t' => $name]);
    }

    /**
     * searchevents.php?e=: events by name, "Home vs Away". Narrow with a season or a date, not both.
     *
     * @return list<Event>
     */
    public function events(string $name, ?string $season = null, ?\DateTimeInterface $date = null): array
    {
        if ($season !== null && $date !== null) {
            throw new \InvalidArgumentException('Pass season or date, not both');
        }
        return $this->r->v1('searchevents.php', 'event', Freshness::Medium, static fn ($x) => new Event($x),
            ['e' => $name, 's' => $season, 'd' => $date]);
    }

    /** @return list<Event> searchfilename.php?e=: by Event::$filename */
    public function eventsByFilename(string $filename, ?string $season = null): array
    {
        return $this->r->v1('searchfilename.php', 'event', Freshness::Medium, static fn ($x) => new Event($x),
            ['e' => $filename, 's' => $season]);
    }

    /** @return list<Player> searchplayers.php?p= (summary fields) */
    public function players(string $name): array
    {
        return $this->r->v1('searchplayers.php', 'player', Freshness::Slow, static fn ($x) => new Player($x), ['p' => $name]);
    }

    /** @return list<Venue> searchvenues.php?v= */
    public function venues(string $name): array
    {
        return $this->r->v1('searchvenues.php', 'venues', Freshness::Slow, static fn ($x) => new Venue($x), ['v' => $name]);
    }
}
