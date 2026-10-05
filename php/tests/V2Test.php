<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\Tests;

use PHPUnit\Framework\Attributes\DataProvider;
use PHPUnit\Framework\TestCase;
use RohanG27\TheSportsDb\Exception\InvalidApiKeyException;
use RohanG27\TheSportsDb\Exception\PremiumRequiredException;
use RohanG27\TheSportsDb\Model\EventStatus;
use RohanG27\TheSportsDb\SportsDb;

/** Every v2 method against a real premium response, plus v2's key handling. */
final class V2Test extends TestCase
{
    /** @return iterable<string, array{string, string, \Closure(SportsDb): mixed, int}> */
    public static function endpoints(): iterable
    {
        $cases = [
            ['search_team', 'search/team/Arsenal', fn (SportsDb $c) => $c->v2->search->teams('Arsenal'), 12],
            ['search_league', 'search/league/English%20Premier%20League', fn (SportsDb $c) => $c->v2->search->leagues('English Premier League'), 2],
            ['search_player', 'search/player/Danny%20Welbeck', fn (SportsDb $c) => $c->v2->search->players('Danny Welbeck'), 1],
            ['search_venue', 'search/venue/Wembley', fn (SportsDb $c) => $c->v2->search->venues('Wembley'), 3],
            ['lookup_team_equipment', 'lookup/team_equipment/133597', fn (SportsDb $c) => $c->v2->lookup->teamEquipment(133597), 18],
            ['lookup_player_contracts', 'lookup/player_contracts/34147178', fn (SportsDb $c) => $c->v2->lookup->playerContracts(34147178), 1],
            ['lookup_player_results', 'lookup/player_results/34160573', fn (SportsDb $c) => $c->v2->lookup->playerResults(34160573), 24],
            ['lookup_player_honours', 'lookup/player_honours/34147178', fn (SportsDb $c) => $c->v2->lookup->playerHonours(34147178), 5],
            ['lookup_player_milestones', 'lookup/player_milestones/34161397', fn (SportsDb $c) => $c->v2->lookup->playerMilestones(34161397), 3],
            ['lookup_player_teams', 'lookup/player_teams/34147178', fn (SportsDb $c) => $c->v2->lookup->playerTeams(34147178), 6],
            ['lookup_player_stats', 'lookup/player_stats/34146304', fn (SportsDb $c) => $c->v2->lookup->playerStats(34146304), 315],
            ['lookup_event_lineup', 'lookup/event_lineup/1032723', fn (SportsDb $c) => $c->v2->lookup->eventLineup(1032723), 22],
            ['lookup_event_results', 'lookup/event_results/652890', fn (SportsDb $c) => $c->v2->lookup->eventResults(652890), 22],
            ['lookup_event_stats', 'lookup/event_stats/1032723', fn (SportsDb $c) => $c->v2->lookup->eventStats(1032723), 16],
            ['lookup_event_timeline', 'lookup/event_timeline/1032718', fn (SportsDb $c) => $c->v2->lookup->eventTimeline(1032718), 10],
            ['lookup_event_tv', 'lookup/event_tv/2494052', fn (SportsDb $c) => $c->v2->lookup->eventTv(2494052), 13],
            ['lookup_event_highlights', 'lookup/event_highlights/441613', fn (SportsDb $c) => $c->v2->lookup->eventHighlights(441613), 1],
            ['list_teams', 'list/teams/4328', fn (SportsDb $c) => $c->v2->list->teams(4328), 20],
            ['list_seasons', 'list/seasons/4328', fn (SportsDb $c) => $c->v2->list->seasons(4328), 35],
            ['list_players', 'list/players/133604', fn (SportsDb $c) => $c->v2->list->players(133604), 27],
            ['list_seasonposters', 'list/seasonposters/4328', fn (SportsDb $c) => $c->v2->list->seasonPosters(4328), 9],
            ['all_countries', 'all/countries', fn (SportsDb $c) => $c->v2->all->countries(), 256],
            ['all_sports', 'all/sports', fn (SportsDb $c) => $c->v2->all->sports(), 37],
            ['all_leagues', 'all/leagues', fn (SportsDb $c) => $c->v2->all->leagues(), 1547],
            ['schedule_next_league', 'schedule/next/league/4328', fn (SportsDb $c) => $c->v2->schedule->leagueNext(4328), 20],
            ['schedule_previous_league', 'schedule/previous/league/4328', fn (SportsDb $c) => $c->v2->schedule->leaguePrevious(4328), 20],
            ['schedule_next_team', 'schedule/next/team/133604', fn (SportsDb $c) => $c->v2->schedule->teamNext(133604), 10],
            ['schedule_previous_team', 'schedule/previous/team/133604', fn (SportsDb $c) => $c->v2->schedule->teamPrevious(133604), 10],
            ['schedule_next_venue', 'schedule/next/venue/16163', fn (SportsDb $c) => $c->v2->schedule->venueNext(16163), 3],
            ['schedule_previous_venue', 'schedule/previous/venue/16163', fn (SportsDb $c) => $c->v2->schedule->venuePrevious(16163), 10],
            ['schedule_full_team', 'schedule/full/team/133604', fn (SportsDb $c) => $c->v2->schedule->teamFull(133604), 48],
            ['schedule_league_season', 'schedule/league/4328/2026-2027', fn (SportsDb $c) => $c->v2->schedule->leagueSeason(4328, '2026-2027'), 380],
            ['filter_tv_day', 'filter/tv/day/2026-10-05', fn (SportsDb $c) => $c->v2->tv->day(new \DateTimeImmutable('2026-10-05')), 287],
            ['filter_tv_country', 'filter/tv/country/Canada', fn (SportsDb $c) => $c->v2->tv->country('Canada'), 90],
            ['filter_tv_sport', 'filter/tv/sport/Ice%20Hockey', fn (SportsDb $c) => $c->v2->tv->sport('Ice Hockey'), 189],
            ['filter_tv_channel', 'filter/tv/channel/TSN%201', fn (SportsDb $c) => $c->v2->tv->channel('TSN 1'), 3],
            ['filter_tv_channel_id', 'filter/tv/channelid/8631', fn (SportsDb $c) => $c->v2->tv->channelId(8631), 4],
            ['livescore_all', 'livescore/all', fn (SportsDb $c) => $c->v2->live->all(), 54],
            ['livescore_soccer', 'livescore/soccer', fn (SportsDb $c) => $c->v2->live->sport('soccer'), 17],
        ];
        foreach ($cases as $case) {
            yield $case[0] => $case;
        }
    }

    /** @param \Closure(SportsDb): mixed $call */
    #[DataProvider('endpoints')]
    public function testEndpoint(string $fixture, string $path, \Closure $call, int $count): void
    {
        $t = (new FakeTransport())->respond(fixture("v2/$fixture.json"));
        $result = $call(client($t, PREMIUM));
        [$url, $headers] = $t->requests[0];
        self::assertSame("https://www.thesportsdb.com/api/v2/json/$path", $url);
        self::assertSame(PREMIUM, $headers['X-API-KEY']);
        self::assertStringNotContainsString(PREMIUM, $url);
        self::assertCount($count, $result);
    }

    public function testSingleLookups(): void
    {
        foreach (['league' => [4328, 'lookup_league'], 'team' => [133604, 'lookup_team'], 'player' => [34145937, 'lookup_player'],
                     'event' => [441613, 'lookup_event'], 'venue' => [16163, 'lookup_venue']] as $method => [$id, $name]) {
            $t = (new FakeTransport())->respond(fixture("v2/$name.json"));
            self::assertNotNull(client($t, PREMIUM)->v2->lookup->$method($id), $method);
        }
    }

    public function testSearchIdsArriveAsJsonNumbers(): void
    {
        $team = client((new FakeTransport())->respond(fixture('v2/search_team.json')), PREMIUM)->v2->search->teams('Arsenal')[0];
        self::assertSame(133604, $team->id);
        self::assertSame('133604', $team->raw['idTeam']);
    }

    public function testV2Specifics(): void
    {
        $t = (new FakeTransport())->respond(fixture('v2/all_countries.json'))->respond(fixture('v2/schedule_full_team.json'))
            ->respond(fixture('v2/livescore_all.json'))->respond(fixture('v2/search_event_none.json'));
        $c = client($t, PREMIUM);
        $andorra = array_values(array_filter($c->v2->all->countries(), static fn ($x) => $x->name === 'Andorra'))[0];
        self::assertSame(['AD', 'Andorre', null], [$andorra->code, $andorra->nameFr, $andorra->apiFootballId]);
        $first = $c->v2->schedule->teamFull(133604)[0];
        self::assertSame('2027-05-30T15:00:00+00:00', $first->timestamp?->format(\DATE_ATOM));
        self::assertSame(EventStatus::NotStarted, $first->status());
        $live = $c->v2->live->all()[0];
        self::assertSame(['P3', EventStatus::InPlay, 2], [$live->statusCode, $live->status(), $live->homeScore]);
        self::assertSame([], $c->v2->search->events('Arsenal vs Chelsea'));
    }

    public function testFreeKeysNeverCallV2(): void
    {
        foreach (['123', '3'] as $key) {
            $t = new FakeTransport();
            $c = client($t, $key);
            try {
                $c->v2->all->sports();
                self::fail('expected PremiumRequiredException');
            } catch (PremiumRequiredException) {
            }
            self::assertSame([], $t->requests);
            self::assertFalse($c->isPremiumKey());
        }
    }

    public function testInvalidKeyAndPremiumDetection(): void
    {
        $this->expectException(InvalidApiKeyException::class);
        self::assertTrue(client((new FakeTransport())->respond(fixture('v2/lookup_league.json')), PREMIUM)->isPremiumKey());
        self::assertFalse(client((new FakeTransport())->respond(fixture('v2/invalid_key.json'), 400), '1234567890')->isPremiumKey());
        client((new FakeTransport())->respond(fixture('v2/invalid_key.json'), 400), '1234567890')->v2->lookup->league(4328);
    }
}
