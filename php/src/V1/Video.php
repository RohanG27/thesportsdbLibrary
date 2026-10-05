<?php

declare(strict_types=1);

namespace SportsDb\V1;

use SportsDb\Cache\Freshness;
use SportsDb\Internal\Requester;
use SportsDb\Model\Event;

final class Video
{
    /** @internal */
    public function __construct(private readonly Requester $r)
    {
    }

    /** @return list<Event> eventshighlights.php?d=: events with highlight videos (Event::$video). Free keys: 2. */
    public function highlights(\DateTimeInterface $date, ?int $leagueId = null, ?string $sport = null): array
    {
        return $this->r->v1('eventshighlights.php', 'tvhighlights', Freshness::Medium, static fn ($x) => new Event($x),
            ['d' => $date, 'l' => $leagueId, 's' => $sport]);
    }
}
