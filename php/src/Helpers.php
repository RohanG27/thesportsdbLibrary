<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb;

use RohanG27\TheSportsDb\Model\Event;
use RohanG27\TheSportsDb\Model\LiveScore;
use RohanG27\TheSportsDb\Model\Team;
use RohanG27\TheSportsDb\Model\TvListing;

/**
 * Common tasks in one call. Each helper uses v2 with a premium key and v1 with a free key (then
 * the free keys' small limits apply). Any key other than the free keys that the API accepts is a
 * paid key, so no extra call is made to find out. Events come back sorted by start time.
 */
final class Helpers
{
    /** @internal */
    public function __construct(private readonly SportsDb $db)
    {
    }

    /** A league's current season name, e.g. "2026-2027"; null if the league is unknown. */
    public function currentSeason(int $leagueId): ?string
    {
        $league = $this->premium() ? $this->db->v2->lookup->league($leagueId) : $this->db->v1->lookup->league($leagueId);
        return $league?->currentSeason;
    }

    /** @return list<Event> every event of a season (default: current). Premium: one call. Free keys: the first 5. */
    public function seasonEvents(int $leagueId, ?string $season = null): array
    {
        $season ??= $this->currentSeason($leagueId);
        if ($season === null) {
            return [];
        }
        return self::byStart($this->premium()
            ? $this->db->v2->schedule->leagueSeason($leagueId, $season)
            : $this->db->v1->schedule->season($leagueId, $season));
    }

    /**
     * One round (matchday). Premium: filtered from the season. Free keys: eventsround.php
     * (undocumented; the whole round). Premium keys get 404 from that endpoint, hence two routes.
     *
     * @return list<Event>
     */
    public function roundEvents(int $leagueId, int $round, ?string $season = null): array
    {
        if ($this->premium()) {
            return array_values(array_filter($this->seasonEvents($leagueId, $season), static fn (Event $e) => $e->round === $round));
        }
        $season ??= $this->currentSeason($leagueId);
        return $season === null ? [] : self::byStart($this->db->v1->schedule->round($leagueId, $round, $season));
    }

    /**
     * Events in the next $days UTC days from $start (default: today, UTC). Premium: filtered from
     * the season. Free keys: one eventsday.php call per day (3 events a day).
     *
     * @return list<Event>
     */
    public function upcomingLeagueEvents(int $leagueId, int $days = 7, ?\DateTimeInterface $start = null): array
    {
        $dates = self::days($days, $start);
        if (!$this->premium()) {
            $events = [];
            foreach ($dates as $d) {
                array_push($events, ...$this->db->v1->schedule->day($d, leagueId: $leagueId));
            }
            return self::byStart(self::distinct($events));
        }
        $wanted = array_map(static fn ($d) => $d->format('Y-m-d'), $dates);
        return array_values(array_filter($this->seasonEvents($leagueId),
            static fn (Event $e) => $e->date !== null && \in_array($e->date->format('Y-m-d'), $wanted, true)));
    }

    /** @return list<Event> a league's latest results. Premium: about 20. Free keys: 1. */
    public function recentLeagueResults(int $leagueId): array
    {
        return self::byStart($this->premium()
            ? $this->db->v2->schedule->leaguePrevious($leagueId)
            : $this->db->v1->schedule->leaguePast($leagueId));
    }

    /** @return list<Event> a team's schedule. Premium: past and future, all competitions. Free keys: next + last, home games only. */
    public function teamSchedule(int $teamId): array
    {
        if ($this->premium()) {
            return self::byStart($this->db->v2->schedule->teamFull($teamId));
        }
        return self::byStart(self::distinct([...$this->db->v1->schedule->teamLast($teamId), ...$this->db->v1->schedule->teamNext($teamId)]));
    }

    /**
     * Events starting on $day in time zone $tz. The API files events under their UTC date, so a
     * local day can span two API days; this fetches each UTC day it overlaps and keeps the events
     * that start on the local day. Free keys: at most 3 events per UTC day.
     *
     * @param \DateTimeInterface|string $day a date (only Y-m-d is used)
     * @return list<Event>
     */
    public function eventsOnLocalDate(\DateTimeInterface|string $day, \DateTimeZone $tz, ?string $sport = null, ?int $leagueId = null): array
    {
        $ymd = $day instanceof \DateTimeInterface ? $day->format('Y-m-d') : substr($day, 0, 10);
        $utc = new \DateTimeZone('UTC');
        $start = (new \DateTimeImmutable("$ymd 00:00:00", $tz))->setTimezone($utc);
        $end = (new \DateTimeImmutable("$ymd 00:00:00", $tz))->modify('+1 day')->setTimezone($utc);
        $events = [];
        for ($d = $start->setTime(0, 0); $d < $end; $d = $d->modify('+1 day')) {
            array_push($events, ...$this->db->v1->schedule->day($d, sport: $sport, leagueId: $leagueId));
        }
        $kept = array_filter(self::distinct($events), static fn (Event $e) => $e->timestamp !== null
            ? $e->timestamp >= $start && $e->timestamp < $end
            : $e->date?->format('Y-m-d') === $ymd);
        return self::byStart(array_values($kept));
    }

    /**
     * Games in progress. Premium: v2. Free keys: v1's undocumented feed, which needs a sport; for a
     * league, its sport is looked up and the feed filtered. Entries can be stale: check $updated.
     *
     * @return list<LiveScore>
     */
    public function liveScores(?string $sport = null, ?int $leagueId = null): array
    {
        if ($this->premium()) {
            return match (true) {
                $leagueId !== null => $this->db->v2->live->league($leagueId),
                $sport !== null => $this->db->v2->live->sport($sport),
                default => $this->db->v2->live->all(),
            };
        }
        if ($sport === null && $leagueId !== null) {
            $sport = $this->db->v1->lookup->league($leagueId)?->sport;
        }
        if ($sport === null) {
            throw new \InvalidArgumentException('With a free key, live scores need a sport or a leagueId');
        }
        $scores = $this->db->v1->live->sport($sport);
        return $leagueId === null ? $scores : array_values(array_filter($scores, static fn (LiveScore $s) => $s->leagueId === $leagueId));
    }

    /** @return list<Team> a league's teams with badges. Free keys: looks up the league name, then up to 10 teams. */
    public function leagueTeams(int $leagueId): array
    {
        if ($this->premium()) {
            return $this->db->v2->list->teams($leagueId);
        }
        $name = $this->db->v1->lookup->league($leagueId)?->name;
        return $name === null ? [] : $this->db->v1->list->teamsInLeague($name);
    }

    /** @return list<TvListing> the channels showing an event. Free keys: at most 2. */
    public function eventChannels(int $eventId): array
    {
        return $this->premium() ? $this->db->v2->lookup->eventTv($eventId) : $this->db->v1->lookup->eventTv($eventId);
    }

    /**
     * A country's TV listings for $days days. Premium: the country's week in one call, filtered.
     * Free keys: one call per day, which needs a sport.
     *
     * @return list<TvListing>
     */
    public function tvListings(string $country, ?string $sport = null, int $days = 7, ?\DateTimeInterface $start = null): array
    {
        $dates = self::days($days, $start);
        if ($this->premium()) {
            $wanted = array_map(static fn ($d) => $d->format('Y-m-d'), $dates);
            $listings = array_values(array_filter($this->db->v2->tv->country($country), static fn (TvListing $t) =>
                $t->date !== null && \in_array($t->date->format('Y-m-d'), $wanted, true)
                && ($sport === null || strcasecmp($t->sport ?? '', $sport) === 0)));
        } else {
            if ($sport === null) {
                throw new \InvalidArgumentException('With a free key, TV listings for a country need a sport');
            }
            $listings = [];
            foreach ($dates as $d) {
                array_push($listings, ...$this->db->v1->tv->day($d, sport: $sport, country: $country));
            }
        }
        usort($listings, static fn (TvListing $a, TvListing $b) => self::compareTimes($a->timestamp, $b->timestamp));
        return $listings;
    }

    private function premium(): bool
    {
        return !$this->db->config->isFreeKey();
    }

    /** @return list<\DateTimeImmutable> */
    private static function days(int $days, ?\DateTimeInterface $start): array
    {
        if ($days < 1 || $days > 31) {
            throw new \InvalidArgumentException('days must be between 1 and 31');
        }
        $utc = new \DateTimeZone('UTC');
        $first = new \DateTimeImmutable(($start ?? new \DateTimeImmutable('now', $utc))->format('Y-m-d'), $utc);
        return array_map(static fn (int $n) => $first->modify("+$n day"), range(0, $days - 1));
    }

    /**
     * @param list<Event> $events
     * @return list<Event>
     */
    private static function byStart(array $events): array
    {
        usort($events, static fn (Event $a, Event $b) => self::compareTimes($a->timestamp, $b->timestamp));
        return $events;
    }

    private static function compareTimes(?\DateTimeImmutable $a, ?\DateTimeImmutable $b): int
    {
        return match (true) {
            $a === null && $b === null => 0,
            $a === null => 1,
            $b === null => -1,
            default => $a <=> $b,
        };
    }

    /**
     * @param list<Event> $events
     * @return list<Event>
     */
    private static function distinct(array $events): array
    {
        $seen = [];
        $out = [];
        foreach ($events as $e) {
            $key = $e->id ?? spl_object_id($e);
            if (!isset($seen[$key])) {
                $seen[$key] = true;
                $out[] = $e;
            }
        }
        return $out;
    }
}
