<?php

declare(strict_types=1);

namespace SportsDb\Tests;

use PHPUnit\Framework\Attributes\DataProvider;
use PHPUnit\Framework\TestCase;
use SportsDb\Exception\ApiMessageException;
use SportsDb\Model\EventStatus;
use SportsDb\SportsDb;

/** Every v1 method against a real recorded response: the URL it builds, the record key it reads, the fields it parses. */
final class V1Test extends TestCase
{
    /** @return iterable<string, array{string, string, \Closure(SportsDb): array<mixed>, int}> */
    public static function endpoints(): iterable
    {
        $d = new \DateTimeImmutable('2026-10-04');
        $tvDay = new \DateTimeImmutable('2026-10-05');
        $cases = [
            ['search_teams', 'searchteams.php?t=Arsenal', fn (SportsDb $c) => $c->v1->search->teams('Arsenal'), 1],
            ['search_events', 'searchevents.php?e=Arsenal%20vs%20Chelsea', fn (SportsDb $c) => $c->v1->search->events('Arsenal vs Chelsea'), 1],
            ['search_events_season', 'searchevents.php?e=Arsenal_vs_Chelsea&s=2016-2017',
                fn (SportsDb $c) => $c->v1->search->events('Arsenal_vs_Chelsea', season: '2016-2017'), 1],
            ['search_events_date', 'searchevents.php?e=Arsenal%20vs%20Chelsea&d=2015-04-26',
                fn (SportsDb $c) => $c->v1->search->events('Arsenal vs Chelsea', date: new \DateTimeImmutable('2015-04-26')), 1],
            ['search_filename', 'searchfilename.php?e=English%20Premier%20League%202015-04-26%20Arsenal%20vs%20Chelsea',
                fn (SportsDb $c) => $c->v1->search->eventsByFilename('English Premier League 2015-04-26 Arsenal vs Chelsea'), 1],
            ['search_players', 'searchplayers.php?p=Danny%20Welbeck', fn (SportsDb $c) => $c->v1->search->players('Danny Welbeck'), 1],
            ['search_venues', 'searchvenues.php?v=Wembley', fn (SportsDb $c) => $c->v1->search->venues('Wembley'), 1],
            ['lookup_table', 'lookuptable.php?l=4328', fn (SportsDb $c) => $c->v1->lookup->table(4328), 5],
            ['lookup_table_season', 'lookuptable.php?l=4328&s=2024-2025', fn (SportsDb $c) => $c->v1->lookup->table(4328, '2024-2025'), 5],
            ['lookup_equipment', 'lookupequipment.php?id=133597', fn (SportsDb $c) => $c->v1->lookup->equipment(133597), 1],
            ['lookup_honours', 'lookuphonours.php?id=34147178', fn (SportsDb $c) => $c->v1->lookup->honours(34147178), 1],
            ['lookup_former_teams', 'lookupformerteams.php?id=34147178', fn (SportsDb $c) => $c->v1->lookup->formerTeams(34147178), 1],
            ['lookup_milestones', 'lookupmilestones.php?id=34161397', fn (SportsDb $c) => $c->v1->lookup->milestones(34161397), 1],
            ['lookup_contracts', 'lookupcontracts.php?id=34147178', fn (SportsDb $c) => $c->v1->lookup->contracts(34147178), 1],
            ['player_results', 'playerresults.php?id=34160573', fn (SportsDb $c) => $c->v1->lookup->playerResults(34160573), 1],
            ['lookup_player_stats', 'lookupplayerstats.php?id=34146304', fn (SportsDb $c) => $c->v1->lookup->playerStats(34146304), 1],
            ['event_results', 'eventresults.php?id=652890', fn (SportsDb $c) => $c->v1->lookup->eventResults(652890), 1],
            ['lookup_lineup', 'lookuplineup.php?id=1032723', fn (SportsDb $c) => $c->v1->lookup->lineup(1032723), 1],
            ['lookup_timeline', 'lookuptimeline.php?id=1032718', fn (SportsDb $c) => $c->v1->lookup->timeline(1032718), 1],
            ['lookup_event_stats', 'lookupeventstats.php?id=1032723', fn (SportsDb $c) => $c->v1->lookup->eventStats(1032723), 1],
            ['lookup_tv', 'lookuptv.php?id=2494052', fn (SportsDb $c) => $c->v1->lookup->eventTv(2494052), 1],
            ['all_sports', 'all_sports.php', fn (SportsDb $c) => $c->v1->list->sports(), 2],
            ['all_countries', 'all_countries.php', fn (SportsDb $c) => $c->v1->list->countries(), 50],
            ['all_leagues', 'all_leagues.php', fn (SportsDb $c) => $c->v1->list->leagues(), 1],
            ['search_all_leagues', 'search_all_leagues.php?c=England&s=Soccer', fn (SportsDb $c) => $c->v1->list->leaguesInCountry('England', 'Soccer'), 1],
            ['search_all_seasons', 'search_all_seasons.php?id=4328', fn (SportsDb $c) => $c->v1->list->seasons(4328), 1],
            ['search_all_seasons_poster', 'search_all_seasons.php?id=4328&poster=1', fn (SportsDb $c) => $c->v1->list->seasons(4328, posters: true), 1],
            ['search_all_seasons_badge', 'search_all_seasons.php?id=4328&badge=1', fn (SportsDb $c) => $c->v1->list->seasons(4328, badges: true), 1],
            ['search_all_seasons_description', 'search_all_seasons.php?id=4328&description=1',
                fn (SportsDb $c) => $c->v1->list->seasons(4328, descriptions: true), 1],
            ['search_all_teams_league', 'search_all_teams.php?l=English%20Premier%20League',
                fn (SportsDb $c) => $c->v1->list->teamsInLeague('English Premier League'), 10],
            ['search_all_teams_country', 'search_all_teams.php?s=Soccer&c=Spain', fn (SportsDb $c) => $c->v1->list->teamsInCountry('Soccer', 'Spain'), 10],
            ['lookup_all_players', 'lookup_all_players.php?id=133604', fn (SportsDb $c) => $c->v1->list->players(133604), 10],
            ['events_next', 'eventsnext.php?id=133602', fn (SportsDb $c) => $c->v1->schedule->teamNext(133602), 1],
            ['events_last', 'eventslast.php?id=133602', fn (SportsDb $c) => $c->v1->schedule->teamLast(133602), 1],
            ['events_next_league', 'eventsnextleague.php?id=4328', fn (SportsDb $c) => $c->v1->schedule->leagueNext(4328), 1],
            ['events_past_league', 'eventspastleague.php?id=4328', fn (SportsDb $c) => $c->v1->schedule->leaguePast(4328), 1],
            ['events_day', 'eventsday.php?d=2026-10-04', fn (SportsDb $c) => $c->v1->schedule->day($d), 3],
            ['events_day_sport', 'eventsday.php?d=2026-10-04&s=Ice%20Hockey', fn (SportsDb $c) => $c->v1->schedule->day($d, sport: 'Ice Hockey'), 3],
            ['events_season', 'eventsseason.php?id=4328&s=2026-2027', fn (SportsDb $c) => $c->v1->schedule->season(4328, '2026-2027'), 5],
            ['events_round', 'eventsround.php?id=4328&r=1&s=2026-2027', fn (SportsDb $c) => $c->v1->schedule->round(4328, 1, '2026-2027'), 10],
            ['events_tv_day', 'eventstv.php?d=2026-10-05', fn (SportsDb $c) => $c->v1->tv->day($tvDay), 1],
            ['events_tv_country', 'eventstv.php?d=2026-10-05&a=Canada&s=Ice%20Hockey',
                fn (SportsDb $c) => $c->v1->tv->day($tvDay, sport: 'Ice Hockey', country: 'Canada'), 1],
            ['events_tv_channel', 'eventstv.php?c=TSN%201', fn (SportsDb $c) => $c->v1->tv->channel('TSN 1'), 1],
            ['events_tv_channel_id', 'eventstv.php?id=8631', fn (SportsDb $c) => $c->v1->tv->channelId(8631), 1],
            ['events_highlights', 'eventshighlights.php?d=2026-10-04', fn (SportsDb $c) => $c->v1->video->highlights($d), 1],
            ['events_highlights_sport', 'eventshighlights.php?d=2026-10-04&s=Soccer', fn (SportsDb $c) => $c->v1->video->highlights($d, sport: 'Soccer'), 1],
            ['livescore_soccer', 'livescore.php?s=Soccer', fn (SportsDb $c) => $c->v1->live->sport('Soccer'), 1],
        ];
        foreach ($cases as $case) {
            yield $case[0] => $case;
        }
    }

    /** @param \Closure(SportsDb): array<mixed> $call */
    #[DataProvider('endpoints')]
    public function testEndpoint(string $fixture, string $expectedCall, \Closure $call, int $minimum): void
    {
        $t = (new FakeTransport())->respond(fixture("v1-free/$fixture.json"));
        $result = $call(client($t));
        self::assertSame("https://www.thesportsdb.com/api/v1/json/123/$expectedCall", $t->lastUrl());
        self::assertGreaterThanOrEqual($minimum, \count($result));
    }

    public function testTeamFields(): void
    {
        $team = client((new FakeTransport())->respond(fixture('v1-free/lookup_team.json')))->v1->lookup->team(133604);
        self::assertNotNull($team);
        self::assertSame([133604, 'Arsenal', 'ARS'], [$team->id, $team->name, $team->shortName]);
        self::assertContains('Arsenal FC', $team->alternateNames);
        self::assertSame(4328, $team->leagues[0]->id);
        self::assertGreaterThan(1, \count($team->leagues));
        self::assertArrayHasKey('DE', $team->descriptions);
        self::assertNotNull($team->description());
        self::assertSame('#EF0107', $team->colours[0]);
        self::assertFalse($team->isLocked);
    }

    public function testSeasonTimesAreUtc(): void
    {
        $first = client((new FakeTransport())->respond(fixture('v1-free/events_season.json')))->v1->schedule->season(4328, '2026-2027')[0];
        self::assertSame('2026-08-21', $first->date?->format('Y-m-d'));
        self::assertSame('19:00:00', $first->time);
        self::assertSame('2026-08-21T19:00:00+00:00', $first->timestamp?->format(\DATE_ATOM));
    }

    public function testOlderEventsHaveNoStatus(): void
    {
        $event = client((new FakeTransport())->respond(fixture('v1-free/lookup_event.json')))->v1->lookup->event(441613);
        self::assertNotNull($event);
        self::assertNull($event->statusCode);
        self::assertSame(EventStatus::Unknown, $event->status());
        self::assertSame(4, $event->homeScore);
    }

    public function testLeaguePlayerVenue(): void
    {
        $t = (new FakeTransport())->respond(fixture('v1-free/lookup_league.json'))->respond(fixture('v1-free/lookup_player.json'))
            ->respond(fixture('v1-free/lookup_venue.json'));
        $c = client($t);
        self::assertSame('1992-08-15', $c->v1->lookup->league(4328)?->firstEventDate?->format('Y-m-d'));
        $player = $c->v1->lookup->player(34145937);
        self::assertNotNull($player?->born);
        self::assertNotNull($player->externalIds->wikidata);
        $venue = $c->v1->lookup->venue(16163);
        self::assertSame(90000, $venue?->capacity);
        self::assertSame([51.555556, -0.279444], $venue->coordinates());
    }

    public function testTvTimestampsWithASpaceAreUtc(): void
    {
        $tv = client((new FakeTransport())->respond(fixture('v1-free/lookup_tv.json')))->v1->lookup->eventTv(2494052);
        foreach ($tv as $listing) {
            self::assertSame('+00:00', $listing->timestamp?->format('P'));
        }
        $byChannel = client((new FakeTransport())->respond(fixture('v1-free/events_tv_channel_id.json')))->v1->tv->channelId(8631);
        self::assertSame([8631], array_values(array_unique(array_map(static fn ($l) => $l->channelId, $byChannel))));
    }

    public function testEmptyResultsAndErrors(): void
    {
        $t = (new FakeTransport())->respond(fixture('v1-free/events_day_none.json'))->respond('')
            ->respond(fixture('v1-free/search_all_seasons_bad_param.json'));
        $c = client($t);
        self::assertSame([], $c->v1->schedule->day(new \DateTimeImmutable('2026-10-04'), leagueId: 4328));
        self::assertSame([], $c->v1->tv->channel('x'));
        try {
            $c->v1->list->seasons(4328);
            self::fail('expected ApiMessageException');
        } catch (ApiMessageException $e) {
            self::assertSame('Invalid League ID passed', $e->apiMessage);
        }
    }

    public function testArgumentRulesAreCheckedBeforeAnyRequest(): void
    {
        $t = new FakeTransport();
        $c = client($t);
        foreach ([
            fn () => $c->v1->tv->day(new \DateTimeImmutable(), country: 'Canada'),
            fn () => $c->v1->search->events('x', season: '2020', date: new \DateTimeImmutable()),
            fn () => $c->v1->list->seasons(1, badges: true, posters: true),
        ] as $call) {
            try {
                $call();
                self::fail('expected InvalidArgumentException');
            } catch (\InvalidArgumentException) {
            }
        }
        self::assertSame([], $t->requests);
    }

    public function testPremiumResultsAreBigger(): void
    {
        $t = (new FakeTransport())->respond(fixture('v1-premium/events_day.json'))->respond(fixture('v1-premium/lookup_table.json'));
        $c = client($t, PREMIUM);
        self::assertGreaterThan(500, \count($c->v1->schedule->day(new \DateTimeImmutable('2026-10-04'))));
        self::assertSame(range(1, 20), array_map(static fn ($s) => $s->rank, $c->v1->lookup->table(4328)));
    }
}
