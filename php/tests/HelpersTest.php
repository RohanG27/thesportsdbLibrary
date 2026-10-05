<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\Tests;

use PHPUnit\Framework\TestCase;
use RohanG27\TheSportsDb\Helpers;
use RohanG27\TheSportsDb\SportsDb;

/** Helpers route to v2 for premium keys and v1 for free keys. */
final class HelpersTest extends TestCase
{
    private static function helpers(Routes $t, bool $premium): Helpers
    {
        return (new SportsDb($premium ? PREMIUM : '123', transport: $t, requestsPerMinute: 0, clock: new FakeClock()))->helpers;
    }

    /** @param list<string> $calls @return list<string> */
    private static function tail(array $calls): array
    {
        $out = array_map(static fn ($c) => substr($c, strrpos($c, '/') + 1), $calls);
        sort($out);
        return $out;
    }

    public function testSeasonEvents(): void
    {
        $p = new Routes(['lookup/league/4328' => 'v2/lookup_league.json', 'schedule/league/4328/2026-2027' => 'v2/schedule_league_season.json']);
        $events = self::helpers($p, true)->seasonEvents(4328);
        self::assertSame(['lookup/league/4328', 'schedule/league/4328/2026-2027'], $p->calls);
        self::assertCount(380, $events);
        $times = array_map(static fn ($e) => $e->timestamp?->getTimestamp(), $events);
        $sorted = $times;
        sort($sorted);
        self::assertSame($sorted, $times);
        $f = new Routes(['lookupleague.php' => 'v1-free/lookup_league.json', 'eventsseason.php' => 'v1-free/events_season.json']);
        self::assertCount(5, self::helpers($f, false)->seasonEvents(4328));
    }

    public function testRoundEventsTakeDifferentRoutes(): void
    {
        $f = new Routes(['eventsround.php' => 'v1-free/events_round.json']);
        self::assertCount(10, self::helpers($f, false)->roundEvents(4328, 1, '2026-2027'));
        self::assertSame(['123/eventsround.php?id=4328&r=1&s=2026-2027'], $f->calls);
        $p = new Routes(['schedule/league/4328/2026-2027' => 'v2/schedule_league_season.json']);
        $expected = \count(array_filter(fixtureRecords('v2/schedule_league_season.json', 'schedule'), static fn ($e) => $e['intRound'] === '1'));
        self::assertCount($expected, self::helpers($p, true)->roundEvents(4328, 1, '2026-2027'));
        self::assertSame(['schedule/league/4328/2026-2027'], $p->calls, 'premium must not call eventsround.php (it 404s)');
    }

    public function testKeyThreeIsFree(): void
    {
        $t = new Routes(['eventsround.php' => 'v1-free/events_round.json']);
        (new SportsDb('3', transport: $t, requestsPerMinute: 0))->helpers->roundEvents(4328, 1, '2026-2027');
        self::assertSame(['3/eventsround.php?id=4328&r=1&s=2026-2027'], $t->calls);
    }

    public function testUpcomingLeagueEvents(): void
    {
        $start = new \DateTimeImmutable('2026-10-17');
        $p = new Routes(['lookup/league/4328' => 'v2/lookup_league.json', 'schedule/league/' => 'v2/schedule_league_season.json']);
        $upcoming = self::helpers($p, true)->upcomingLeagueEvents(4328, 7, $start);
        $expected = \count(array_filter(fixtureRecords('v2/schedule_league_season.json', 'schedule'),
            static fn ($e) => $e['dateEvent'] >= '2026-10-17' && $e['dateEvent'] < '2026-10-24'));
        self::assertGreaterThan(0, $expected);
        self::assertCount($expected, $upcoming);
        $f = new Routes(['eventsday.php' => 'v1-free/events_day.json']);
        self::helpers($f, false)->upcomingLeagueEvents(4328, 3, new \DateTimeImmutable('2026-10-04'));
        self::assertSame(['eventsday.php?d=2026-10-04&l=4328', 'eventsday.php?d=2026-10-05&l=4328', 'eventsday.php?d=2026-10-06&l=4328'],
            self::tail($f->calls));
    }

    public function testTeamSchedule(): void
    {
        $f = new Routes(['eventsnext.php' => 'v1-free/events_next.json', 'eventslast.php' => 'v1-free/events_last.json']);
        $s = self::helpers($f, false)->teamSchedule(133602);
        self::assertCount(2, $f->calls);
        self::assertSame(\count($s), \count(array_unique(array_map(static fn ($e) => $e->id, $s))));
        self::assertCount(48, self::helpers(new Routes(['schedule/full/team/133604' => 'v2/schedule_full_team.json']), true)->teamSchedule(133604));
    }

    public function testEventsOnLocalDateSpansTwoUtcDays(): void
    {
        $t = new Routes(['d=2026-10-04' => 'v1-premium/events_day.json', 'd=2026-10-05' => '{"events":null}']);
        $tz = new \DateTimeZone('America/Toronto');
        $events = self::helpers($t, true)->eventsOnLocalDate('2026-10-04', $tz);
        self::assertSame(['eventsday.php?d=2026-10-04', 'eventsday.php?d=2026-10-05'], self::tail($t->calls));
        $from = (new \DateTimeImmutable('2026-10-04 00:00:00', $tz))->getTimestamp();
        $to = (new \DateTimeImmutable('2026-10-05 00:00:00', $tz))->getTimestamp();
        $expected = \count(array_filter(fixtureRecords('v1-premium/events_day.json', 'events'), static function ($e) use ($from, $to) {
            $ts = (new \DateTimeImmutable($e['strTimestamp'], new \DateTimeZone('UTC')))->getTimestamp();
            return $ts >= $from && $ts < $to;
        }));
        self::assertGreaterThan(0, $expected);
        self::assertLessThan(901, $expected);
        self::assertCount($expected, $events);
    }

    public function testEventsOnLocalDateAheadOfUtc(): void
    {
        $t = new Routes();
        self::helpers($t, false)->eventsOnLocalDate('2026-10-04', new \DateTimeZone('Asia/Tokyo'), sport: 'Soccer');
        self::assertSame(['eventsday.php?d=2026-10-03&s=Soccer', 'eventsday.php?d=2026-10-04&s=Soccer'], self::tail($t->calls));
    }

    public function testLiveScores(): void
    {
        $league = fixtureRecords('v1-free/livescore_soccer.json', 'livescore')[0]['idLeague'];
        $f = new Routes(['lookupleague.php' => "{\"leagues\":[{\"idLeague\":\"$league\",\"strSport\":\"Soccer\"}]}",
            'livescore.php?s=Soccer' => 'v1-free/livescore_soccer.json']);
        $scores = self::helpers($f, false)->liveScores(leagueId: (int) $league);
        self::assertNotEmpty($scores);
        foreach ($scores as $s) {
            self::assertSame((int) $league, $s->leagueId);
        }
        self::assertCount(54, self::helpers(new Routes(['livescore/all' => 'v2/livescore_all.json']), true)->liveScores());
        $this->expectException(\InvalidArgumentException::class);
        self::helpers(new Routes(), false)->liveScores();
    }

    public function testLeagueTeamsAndChannels(): void
    {
        $f = new Routes(['lookupleague.php' => 'v1-free/lookup_league.json', 'search_all_teams.php' => 'v1-free/search_all_teams_league.json']);
        self::assertCount(10, self::helpers($f, false)->leagueTeams(4328));
        self::assertStringEndsWith('search_all_teams.php?l=English%20Premier%20League', $f->calls[1]);
        self::assertCount(20, self::helpers(new Routes(['list/teams/4328' => 'v2/list_teams.json']), true)->leagueTeams(4328));
        self::assertCount(13, self::helpers(new Routes(['lookup/event_tv/2494052' => 'v2/lookup_event_tv.json']), true)->eventChannels(2494052));
        self::assertCount(2, self::helpers(new Routes(['lookuptv.php' => 'v1-free/lookup_tv.json']), false)->eventChannels(2494052));
    }

    public function testTvListings(): void
    {
        $start = new \DateTimeImmutable('2026-10-05');
        $p = new Routes(['filter/tv/country/Canada' => 'v2/filter_tv_country.json']);
        $hockey = self::helpers($p, true)->tvListings('Canada', sport: 'ice hockey', days: 2, start: $start);
        self::assertCount(1, $p->calls);
        self::assertNotEmpty($hockey);
        foreach ($hockey as $l) {
            self::assertSame('Ice Hockey', $l->sport);
            self::assertContains($l->date?->format('Y-m-d'), ['2026-10-05', '2026-10-06']);
        }
        $f = new Routes(['eventstv.php' => 'v1-free/events_tv_country.json']);
        self::helpers($f, false)->tvListings('Canada', sport: 'Ice Hockey', days: 2, start: $start);
        self::assertCount(2, $f->calls);
        $this->expectException(\InvalidArgumentException::class);
        self::helpers(new Routes(), false)->tvListings('Canada');
    }
}
