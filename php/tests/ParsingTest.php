<?php

declare(strict_types=1);

namespace SportsDb\Tests;

use PHPUnit\Framework\TestCase;
use SportsDb\Exception\ApiMessageException;
use SportsDb\Exception\InvalidApiKeyException;
use SportsDb\Exception\ResponseParseException;
use SportsDb\Internal\Envelope;
use SportsDb\Internal\Rec;
use SportsDb\Model\EventStatus;
use SportsDb\Model\ImageSize;

final class ParsingTest extends TestCase
{
    /** @param array<string, mixed> $values */
    private static function rec(array $values): Rec
    {
        return new Rec(Envelope::normalize($values));
    }

    public function testBlanksAndNullsAreMissing(): void
    {
        $r = self::rec(['a' => '', 'b' => null, 'c' => '  ', 'd' => 'x']);
        self::assertSame([null, null, null, null, 'x'], [$r->s('a'), $r->s('b'), $r->s('c'), $r->s('missing'), $r->s('d')]);
    }

    public function testNumbers(): void
    {
        $r = self::rec(['i' => '42', 'f' => '3.0', 'bad' => 'n/a', 'zero' => '0', 'num' => 133604]);
        self::assertSame([42, 3, null, null, null, 133604], [$r->int('i'), $r->int('f'), $r->int('bad'), $r->year('zero'), $r->id('zero'), $r->id('num')]);
    }

    public function testBooleansInAnyCase(): void
    {
        $r = self::rec(['a' => 'Yes', 'b' => 'NO', 'c' => 'no', 'd' => 'maybe', 'strLocked' => 'unlocked']);
        self::assertSame([true, false, false, null, false], [$r->bool('a'), $r->bool('b'), $r->bool('c'), $r->bool('d'), $r->locked()]);
    }

    public function testTimestampsAreUtc(): void
    {
        $r = self::rec(['e' => '2026-10-10T11:30:00', 'tv' => '2026-10-10 11:30:00', 'z' => '2026-10-10T11:30:00+02:00', 'bad' => 'soon']);
        self::assertSame('2026-10-10T11:30:00+00:00', $r->instant('e')?->format(\DATE_ATOM));
        self::assertSame('2026-10-10T11:30:00+00:00', $r->instant('tv')?->format(\DATE_ATOM));
        self::assertSame('2026-10-10T09:30:00+00:00', $r->instant('z')?->format(\DATE_ATOM));
        self::assertNull($r->instant('bad'));
    }

    public function testDatesAndTimes(): void
    {
        $r = self::rec(['d' => '2026-10-04', 'zero' => '0000-00-00', 't1' => '16:00', 't2' => '16:30:15', 't3' => '16:00:00+00:00', 'tb' => 'TBC']);
        self::assertSame('2026-10-04', $r->date('d')?->format('Y-m-d'));
        self::assertNull($r->date('zero'));
        self::assertSame(['16:00:00', '16:30:15', '16:00:00', null], [$r->time('t1'), $r->time('t2'), $r->time('t3'), $r->time('tb')]);
    }

    public function testCommaLists(): void
    {
        self::assertSame(['Arsenal Football Club', 'AFC', 'Arsenal FC'], self::rec(['x' => 'Arsenal Football Club, AFC, Arsenal FC'])->csv('x'));
        self::assertSame([], self::rec(['x' => null])->csv('x'));
    }

    public function testEnvelopes(): void
    {
        self::assertSame([], Envelope::records('', 'events', 'u'));
        self::assertSame([], Envelope::records('{"events":null}', 'events', 'u'));
        self::assertSame([], Envelope::records('{"Message":"No data found"}', 'lookup', 'u'));
        self::assertCount(1, Envelope::records('{"events":[{"idEvent":"1"}]}', 'events', 'u'));
        self::assertSame('1', Envelope::records('{"renamed":[{"idEvent":"1"}]}', 'events', 'u')[0]['idEvent']);
        foreach ([
            ['{"seasons":"Invalid League ID passed"}', ApiMessageException::class],
            ['{"Message":"Something new"}', ApiMessageException::class],
            ['{"Message":"Invalid Premium API key: Signup here"}', InvalidApiKeyException::class],
            ['<html>Cloudflare</html>', ResponseParseException::class],
        ] as [$body, $class]) {
            try {
                Envelope::records($body, 'seasons', 'u');
                self::fail("expected $class for $body");
            } catch (\Throwable $e) {
                self::assertInstanceOf($class, $e);
            }
        }
    }

    public function testEventStatus(): void
    {
        foreach (['NS' => EventStatus::NotStarted, '2H' => EventStatus::InPlay, 'Q3' => EventStatus::InPlay, 'P2' => EventStatus::InPlay,
                     'IN7' => EventStatus::InPlay, 'FT' => EventStatus::Finished, 'aet' => EventStatus::Finished,
                     'PST' => EventStatus::Postponed, '???' => EventStatus::Unknown] as $code => $status) {
            self::assertSame($status, EventStatus::of((string) $code), (string) $code);
        }
        self::assertSame(EventStatus::Unknown, EventStatus::of(null));
    }

    public function testImageSizes(): void
    {
        $badge = 'https://r2.thesportsdb.com/images/media/team/badge/uyhbfe1612467038.png';
        self::assertSame("$badge/tiny", ImageSize::Tiny->of($badge));
        self::assertSame("$badge/small", ImageSize::Small->of("$badge/tiny"));
        $honour = 'https://www.thesportsdb.com/images/media/honour/logo/mxhjar1650460067.png';
        self::assertSame("$honour/tiny", ImageSize::Tiny->of($honour));
        $thumb = 'https://www.thesportsdb.com/images/sports/soccer.jpg'; // /tiny 404s here
        self::assertSame($thumb, ImageSize::Tiny->of($thumb));
        self::assertNull(ImageSize::Tiny->of(null));
    }
}
