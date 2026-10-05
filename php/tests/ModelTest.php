<?php

declare(strict_types=1);

namespace SportsDb\Tests;

use PHPUnit\Framework\TestCase;

final class ModelTest extends TestCase
{
    public function testRecordsAreEqualWhenTheApiSentTheSameData(): void
    {
        $t = (new FakeTransport())->respond(fixture('v1-free/lookup_team.json'))->respond(fixture('v1-free/lookup_team.json'))
            ->respond(fixture('v1-free/search_all_teams_league.json'));
        $c = client($t);
        $a = $c->v1->lookup->team(133604);
        $b = $c->v1->lookup->team(133604);
        self::assertNotSame($a, $b);
        self::assertTrue($a == $b);
        self::assertTrue($a?->equals($b));
        $other = array_values(array_filter($c->v1->list->teamsInLeague('English Premier League'), static fn ($x) => $x->id !== 133604))[0];
        self::assertFalse($a == $other);
    }

    public function testDifferentTypesFromTheSameFieldsDiffer(): void
    {
        $body = '{"tvhighlights":[{"idEvent":"1"}],"tvevents":[{"idEvent":"1"}]}';
        $c = client((new FakeTransport())->respond($body)->respond($body));
        $event = $c->v1->video->highlights(new \DateTimeImmutable('2026-01-01'))[0];
        $listing = $c->v1->tv->channel('x')[0];
        self::assertSame($event->raw, $listing->raw);
        self::assertFalse($event->equals($listing));
    }

    public function testToStringIsShort(): void
    {
        $c = client((new FakeTransport())->respond(fixture('v1-free/lookup_team.json'))->respond(fixture('v1-free/lookup_table.json')));
        self::assertSame('Team(id=133604, name=Arsenal)', (string) $c->v1->lookup->team(133604));
        self::assertMatchesRegularExpression('/^Standing\(rank=1, team=[^,]+, points=\d+\)$/', (string) $c->v1->lookup->table(4328)[0]);
    }

    public function testRecordsAreReadOnly(): void
    {
        $team = client((new FakeTransport())->respond(fixture('v1-free/lookup_team.json')))->v1->lookup->team(133604);
        $this->expectException(\Error::class);
        $team->name = 'x'; // @phpstan-ignore-line deliberately writing a readonly property
    }
}
