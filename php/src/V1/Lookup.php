<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\V1;

use RohanG27\TheSportsDb\Cache\Freshness;
use RohanG27\TheSportsDb\Internal\Requester;
use RohanG27\TheSportsDb\Model\Contract;
use RohanG27\TheSportsDb\Model\Equipment;
use RohanG27\TheSportsDb\Model\Event;
use RohanG27\TheSportsDb\Model\EventResult;
use RohanG27\TheSportsDb\Model\EventStat;
use RohanG27\TheSportsDb\Model\FormerTeam;
use RohanG27\TheSportsDb\Model\Honour;
use RohanG27\TheSportsDb\Model\League;
use RohanG27\TheSportsDb\Model\LineupEntry;
use RohanG27\TheSportsDb\Model\Milestone;
use RohanG27\TheSportsDb\Model\Player;
use RohanG27\TheSportsDb\Model\PlayerStat;
use RohanG27\TheSportsDb\Model\Standing;
use RohanG27\TheSportsDb\Model\Team;
use RohanG27\TheSportsDb\Model\TimelineEntry;
use RohanG27\TheSportsDb\Model\TvListing;
use RohanG27\TheSportsDb\Model\Venue;

/** Lookups by id. */
final class Lookup
{
    /** @internal */
    public function __construct(private readonly Requester $r)
    {
    }

    /** lookupleague.php?id= */
    public function league(int $id): ?League
    {
        return $this->r->v1('lookupleague.php', 'leagues', Freshness::Slow, static fn ($x) => new League($x), ['id' => $id])[0] ?? null;
    }

    /** @return list<Standing> lookuptable.php?l=: the league table. Free keys: top 5. Only some leagues have tables. */
    public function table(int $leagueId, ?string $season = null): array
    {
        return $this->r->v1('lookuptable.php', 'table', Freshness::Medium, static fn ($x) => new Standing($x),
            ['l' => $leagueId, 's' => $season]);
    }

    /** lookupteam.php?id= */
    public function team(int $id): ?Team
    {
        return $this->r->v1('lookupteam.php', 'teams', Freshness::Slow, static fn ($x) => new Team($x), ['id' => $id])[0] ?? null;
    }

    /** @return list<Equipment> lookupequipment.php?id=. Free keys: 2. */
    public function equipment(int $teamId): array
    {
        return $this->r->v1('lookupequipment.php', 'equipment', Freshness::Slow, static fn ($x) => new Equipment($x), ['id' => $teamId]);
    }

    /** lookupplayer.php?id= */
    public function player(int $id): ?Player
    {
        return $this->r->v1('lookupplayer.php', 'players', Freshness::Slow, static fn ($x) => new Player($x), ['id' => $id])[0] ?? null;
    }

    /** @return list<Honour> lookuphonours.php?id=. Free keys: 5. */
    public function honours(int $playerId): array
    {
        return $this->r->v1('lookuphonours.php', 'honours', Freshness::Slow, static fn ($x) => new Honour($x), ['id' => $playerId]);
    }

    /** @return list<FormerTeam> lookupformerteams.php?id=. Free keys: 5. */
    public function formerTeams(int $playerId): array
    {
        return $this->r->v1('lookupformerteams.php', 'formerteams', Freshness::Slow, static fn ($x) => new FormerTeam($x),
            ['id' => $playerId]);
    }

    /** @return list<Milestone> lookupmilestones.php?id=. Free keys: 5. */
    public function milestones(int $playerId): array
    {
        return $this->r->v1('lookupmilestones.php', 'milestones', Freshness::Slow, static fn ($x) => new Milestone($x),
            ['id' => $playerId]);
    }

    /** @return list<Contract> lookupcontracts.php?id=. Free keys: 1. */
    public function contracts(int $playerId): array
    {
        return $this->r->v1('lookupcontracts.php', 'contracts', Freshness::Slow, static fn ($x) => new Contract($x), ['id' => $playerId]);
    }

    /** @return list<EventResult> playerresults.php?id=: individual-sport results. Free keys: 5. */
    public function playerResults(int $playerId): array
    {
        return $this->r->v1('playerresults.php', 'results', Freshness::Medium, static fn ($x) => new EventResult($x), ['id' => $playerId]);
    }

    /** @return list<PlayerStat> lookupplayerstats.php?id=. Free keys: 10. */
    public function playerStats(int $playerId): array
    {
        return $this->r->v1('lookupplayerstats.php', 'playerstats', Freshness::Medium, static fn ($x) => new PlayerStat($x),
            ['id' => $playerId]);
    }

    /** lookupevent.php?id= */
    public function event(int $id): ?Event
    {
        return $this->r->v1('lookupevent.php', 'events', Freshness::Medium, static fn ($x) => new Event($x), ['id' => $id])[0] ?? null;
    }

    /** @return list<EventResult> eventresults.php?id=. Free keys: 5. */
    public function eventResults(int $eventId): array
    {
        return $this->r->v1('eventresults.php', 'results', Freshness::Medium, static fn ($x) => new EventResult($x), ['id' => $eventId]);
    }

    /** @return list<LineupEntry> lookuplineup.php?id= (not lookuplineups.php, which is gone). Free keys: 5. */
    public function lineup(int $eventId): array
    {
        return $this->r->v1('lookuplineup.php', 'lineup', Freshness::Medium, static fn ($x) => new LineupEntry($x), ['id' => $eventId]);
    }

    /** @return list<TimelineEntry> lookuptimeline.php?id=. Free keys: 5. */
    public function timeline(int $eventId): array
    {
        return $this->r->v1('lookuptimeline.php', 'timeline', Freshness::Medium, static fn ($x) => new TimelineEntry($x),
            ['id' => $eventId]);
    }

    /** @return list<EventStat> lookupeventstats.php?id=. Free keys: 5. */
    public function eventStats(int $eventId): array
    {
        return $this->r->v1('lookupeventstats.php', 'eventstats', Freshness::Medium, static fn ($x) => new EventStat($x),
            ['id' => $eventId]);
    }

    /** @return list<TvListing> lookuptv.php?id=: the channels showing an event. Free keys: 2. */
    public function eventTv(int $eventId): array
    {
        return $this->r->v1('lookuptv.php', 'tvevent', Freshness::Medium, static fn ($x) => new TvListing($x), ['id' => $eventId]);
    }

    /** lookupvenue.php?id= */
    public function venue(int $id): ?Venue
    {
        return $this->r->v1('lookupvenue.php', 'venues', Freshness::Slow, static fn ($x) => new Venue($x), ['id' => $id])[0] ?? null;
    }
}
