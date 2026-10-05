<?php

declare(strict_types=1);

namespace SportsDb\V1;

use SportsDb\Cache\Freshness;
use SportsDb\Internal\Requester;
use SportsDb\Model\LiveScore;

/** Live scores on v1: **undocumented** but answering, including the full feed for free keys. */
final class Live
{
    /** @internal */
    public function __construct(private readonly Requester $r)
    {
    }

    /** @return list<LiveScore> livescore.php?s= (l= is ignored by the API, so it isn't offered) */
    public function sport(string $sport): array
    {
        return $this->r->v1('livescore.php', 'livescore', Freshness::Live, static fn ($x) => new LiveScore($x), ['s' => $sport]);
    }
}
