<?php

declare(strict_types=1);

namespace SportsDb\Cache;

/** Time-to-live in seconds for each Freshness. Zero or less means don't cache. */
final class CachePolicy
{
    public function __construct(
        public readonly float $static = 7 * 24 * 3600,
        public readonly float $slow = 24 * 3600,
        public readonly float $medium = 3600,
        public readonly float $live = 0,
    ) {
    }

    public function ttl(Freshness $freshness): float
    {
        return match ($freshness) {
            Freshness::Static => $this->static,
            Freshness::Slow => $this->slow,
            Freshness::Medium => $this->medium,
            Freshness::Live => $this->live,
        };
    }
}
