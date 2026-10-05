<?php

declare(strict_types=1);

namespace SportsDb\Cache;

/** Stores raw response bodies. Keys never contain an API key. */
interface ResponseCache
{
    public function get(string $key): ?string;

    public function put(string $key, string $body, float $ttl): void;
}
