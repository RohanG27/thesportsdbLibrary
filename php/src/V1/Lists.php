<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\V1;

use RohanG27\TheSportsDb\Cache\Freshness;
use RohanG27\TheSportsDb\Internal\Requester;
use RohanG27\TheSportsDb\Model\Country;
use RohanG27\TheSportsDb\Model\League;
use RohanG27\TheSportsDb\Model\Player;
use RohanG27\TheSportsDb\Model\Season;
use RohanG27\TheSportsDb\Model\Sport;
use RohanG27\TheSportsDb\Model\Team;

/** Lists. */
final class Lists
{
    /** @internal */
    public function __construct(private readonly Requester $r)
    {
    }

    /** @return list<Sport> all_sports.php. Free keys: 2. */
    public function sports(): array
    {
        return $this->r->v1('all_sports.php', 'sports', Freshness::Static, static fn ($x) => new Sport($x));
    }

    /** @return list<Country> all_countries.php: names and flags. Free keys: 50. */
    public function countries(): array
    {
        return $this->r->v1('all_countries.php', 'countries', Freshness::Static, static fn ($x) => new Country($x));
    }

    /** @return list<League> all_leagues.php: id, name, sport (alternate names with a premium key only). Free keys: 5. */
    public function leagues(): array
    {
        return $this->r->v1('all_leagues.php', 'leagues', Freshness::Static, static fn ($x) => new League($x));
    }

    /** @return list<League> search_all_leagues.php?c=&s=: full records (the record key really is "countries") */
    public function leaguesInCountry(string $country, ?string $sport = null): array
    {
        return $this->r->v1('search_all_leagues.php', 'countries', Freshness::Slow, static fn ($x) => new League($x),
            ['c' => $country, 's' => $sport]);
    }

    /** @return list<Season> search_all_seasons.php?id=: names, plus one of badges, posters or descriptions */
    public function seasons(int $leagueId, bool $badges = false, bool $posters = false, bool $descriptions = false): array
    {
        if ((int) $badges + (int) $posters + (int) $descriptions > 1) {
            throw new \InvalidArgumentException('Ask for one of badges, posters or descriptions per request');
        }
        return $this->r->v1('search_all_seasons.php', 'seasons', Freshness::Slow, static fn ($x) => new Season($x), [
            'id' => $leagueId, 'badge' => $badges ? 1 : null, 'poster' => $posters ? 1 : null, 'description' => $descriptions ? 1 : null,
        ]);
    }

    /**
     * search_all_teams.php?l=: a league's teams, by league **name**. Free keys: 10.
     * (Not lookup_all_teams.php?id=, which returns another league's teams.)
     *
     * @return list<Team>
     */
    public function teamsInLeague(string $leagueName): array
    {
        return $this->r->v1('search_all_teams.php', 'teams', Freshness::Slow, static fn ($x) => new Team($x), ['l' => $leagueName]);
    }

    /** @return list<Team> search_all_teams.php?s=&c=. Free keys: 10. */
    public function teamsInCountry(string $sport, string $country): array
    {
        return $this->r->v1('search_all_teams.php', 'teams', Freshness::Slow, static fn ($x) => new Team($x),
            ['s' => $sport, 'c' => $country]);
    }

    /** @return list<Player> lookup_all_players.php?id=: a team's squad. Free keys: 10. */
    public function players(int $teamId): array
    {
        return $this->r->v1('lookup_all_players.php', 'player', Freshness::Slow, static fn ($x) => new Player($x), ['id' => $teamId]);
    }
}
