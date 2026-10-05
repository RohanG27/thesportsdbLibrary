<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\V1;

use RohanG27\TheSportsDb\Cache\Freshness;
use RohanG27\TheSportsDb\Internal\Requester;
use RohanG27\TheSportsDb\Model\Event;

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
