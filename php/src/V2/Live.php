<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\V2;

use RohanG27\TheSportsDb\Cache\Freshness;
use RohanG27\TheSportsDb\Internal\Requester;
use RohanG27\TheSportsDb\Model\LiveScore;

/** livescore/.... Not cached unless the cache policy says so. Entries can be stale: check updated. */
final class Live
{
    /** @internal */
    public function __construct(private readonly Requester $r)
    {
    }

    /** @return list<LiveScore> */
    public function sport(string $sport): array
    {
        return $this->get($sport);
    }

    /** @return list<LiveScore> */
    public function league(int $leagueId): array
    {
        return $this->get($leagueId);
    }

    /** @return list<LiveScore> */
    public function all(): array
    {
        return $this->get('all');
    }

    /** @return list<LiveScore> */
    private function get(string|int $param): array
    {
        return $this->r->v2('livescore', Freshness::Live, static fn ($x) => new LiveScore($x), ['livescore', $param]);
    }
}
