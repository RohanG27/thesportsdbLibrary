<?php

declare(strict_types=1);

namespace SportsDb\V2;

use SportsDb\Cache\Freshness;
use SportsDb\Internal\Requester;
use SportsDb\Model\ApiRecord;
use SportsDb\Model\Contract;
use SportsDb\Model\Equipment;
use SportsDb\Model\Event;
use SportsDb\Model\EventResult;
use SportsDb\Model\EventStat;
use SportsDb\Model\FormerTeam;
use SportsDb\Model\Honour;
use SportsDb\Model\League;
use SportsDb\Model\LineupEntry;
use SportsDb\Model\Milestone;
use SportsDb\Model\Player;
use SportsDb\Model\PlayerStat;
use SportsDb\Model\Team;
use SportsDb\Model\TimelineEntry;
use SportsDb\Model\TvListing;
use SportsDb\Model\Venue;

/** lookup/...: full records by id. */
final class Lookup
{
    /** @internal */
    public function __construct(private readonly Requester $r)
    {
    }

    public function league(int $id): ?League
    {
        return $this->many('league', $id, League::class, Freshness::Slow)[0] ?? null;
    }

    public function team(int $id): ?Team
    {
        return $this->many('team', $id, Team::class, Freshness::Slow)[0] ?? null;
    }

    /** @return list<Equipment> */
    public function teamEquipment(int $teamId): array
    {
        return $this->many('team_equipment', $teamId, Equipment::class, Freshness::Slow);
    }

    public function player(int $id): ?Player
    {
        return $this->many('player', $id, Player::class, Freshness::Slow)[0] ?? null;
    }

    /** @return list<Contract> */
    public function playerContracts(int $playerId): array
    {
        return $this->many('player_contracts', $playerId, Contract::class, Freshness::Slow);
    }

    /** @return list<EventResult> */
    public function playerResults(int $playerId): array
    {
        return $this->many('player_results', $playerId, EventResult::class, Freshness::Medium);
    }

    /** @return list<Honour> */
    public function playerHonours(int $playerId): array
    {
        return $this->many('player_honours', $playerId, Honour::class, Freshness::Slow);
    }

    /** @return list<Milestone> */
    public function playerMilestones(int $playerId): array
    {
        return $this->many('player_milestones', $playerId, Milestone::class, Freshness::Slow);
    }

    /** @return list<FormerTeam> a player's former teams (v1 calls these formerteams) */
    public function playerTeams(int $playerId): array
    {
        return $this->many('player_teams', $playerId, FormerTeam::class, Freshness::Slow);
    }

    /** @return list<PlayerStat> */
    public function playerStats(int $playerId): array
    {
        return $this->many('player_stats', $playerId, PlayerStat::class, Freshness::Medium);
    }

    public function event(int $id): ?Event
    {
        return $this->many('event', $id, Event::class, Freshness::Medium)[0] ?? null;
    }

    /** @return list<LineupEntry> */
    public function eventLineup(int $eventId): array
    {
        return $this->many('event_lineup', $eventId, LineupEntry::class, Freshness::Medium);
    }

    /** @return list<EventResult> */
    public function eventResults(int $eventId): array
    {
        return $this->many('event_results', $eventId, EventResult::class, Freshness::Medium);
    }

    /** @return list<EventStat> */
    public function eventStats(int $eventId): array
    {
        return $this->many('event_stats', $eventId, EventStat::class, Freshness::Medium);
    }

    /** @return list<TimelineEntry> */
    public function eventTimeline(int $eventId): array
    {
        return $this->many('event_timeline', $eventId, TimelineEntry::class, Freshness::Medium);
    }

    /** @return list<TvListing> the channels showing an event */
    public function eventTv(int $eventId): array
    {
        return $this->many('event_tv', $eventId, TvListing::class, Freshness::Medium);
    }

    /** @return list<Event> the event with its highlight video in Event::$video */
    public function eventHighlights(int $eventId): array
    {
        return $this->many('event_highlights', $eventId, Event::class, Freshness::Medium);
    }

    public function venue(int $id): ?Venue
    {
        return $this->many('venue', $id, Venue::class, Freshness::Slow)[0] ?? null;
    }

    /**
     * @template T of ApiRecord
     * @param class-string<T> $class
     * @return list<T>
     */
    private function many(string $name, int $id, string $class, Freshness $freshness): array
    {
        return $this->r->v2('lookup', $freshness, static fn ($x) => new $class($x), ['lookup', $name, $id]);
    }
}
