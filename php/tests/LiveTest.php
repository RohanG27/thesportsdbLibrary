<?php

declare(strict_types=1);

namespace SportsDb\Tests;

use PHPUnit\Framework\Attributes\Group;
use PHPUnit\Framework\TestCase;
use SportsDb\Config;
use SportsDb\SportsDb;

/** Calls the real API. Excluded by default; run with: vendor/bin/phpunit --group live (THESPORTSDB_API_KEY for v2). */
#[Group('live')]
final class LiveTest extends TestCase
{
    private static function key(): string
    {
        $key = trim((string) getenv('THESPORTSDB_API_KEY'));
        return $key !== '' ? $key : Config::FREE_API_KEY;
    }

    public function testLookupAndSearch(): void
    {
        $db = new SportsDb(self::key());
        self::assertSame('Arsenal', $db->v1->lookup->team(133604)?->name);
        self::assertSame('Toronto Maple Leafs', $db->v1->search->teams('Toronto Maple Leafs')[0]->name);
        self::assertSame([], $db->v1->schedule->day(new \DateTimeImmutable('2026-10-04'), leagueId: 4328));
        self::assertSame(self::key() !== Config::FREE_API_KEY, $db->isPremiumKey());
    }

    public function testHelpers(): void
    {
        $h = (new SportsDb(self::key()))->helpers;
        $season = $h->currentSeason(4328);
        self::assertNotNull($season);
        self::assertNotEmpty($h->seasonEvents(4328, $season));
        $tz = new \DateTimeZone('America/Toronto');
        foreach ($h->eventsOnLocalDate('2026-10-04', $tz, sport: 'Soccer') as $e) {
            self::assertSame('2026-10-04', $e->timestamp?->setTimezone($tz)->format('Y-m-d') ?? '2026-10-04');
        }
    }

    public function testV2(): void
    {
        if (self::key() === Config::FREE_API_KEY) {
            self::markTestSkipped('set THESPORTSDB_API_KEY to test v2');
        }
        $db = new SportsDb(self::key());
        $season = $db->v2->lookup->league(4328)?->currentSeason;
        self::assertNotNull($season);
        self::assertGreaterThan(100, \count($db->v2->schedule->leagueSeason(4328, $season)));
    }
}
