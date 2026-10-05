<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb;

use RohanG27\TheSportsDb\Cache\CachePolicy;
use RohanG27\TheSportsDb\Cache\ResponseCache;

/** Client settings. Pass them to SportsDb's constructor as named arguments. */
final class Config
{
    /** The documented public key for development and testing. */
    public const FREE_API_KEY = '123';

    /** Keys the API accepts without payment: 123 and the older 3 (same limits; measured 5 Oct 2026). */
    public const FREE_API_KEYS = ['123', '3'];

    public readonly CachePolicy $cachePolicy;

    /**
     * @param int|null $requestsPerMinute client-side limit; null = 30 for a free key, 100 otherwise; 0 = off
     * @param int $maxRetries retries for network errors and HTTP 5xx, with exponential backoff
     * @param float $retryBackoff first backoff in seconds; doubles on every retry
     * @param bool $retryOnRateLimit on HTTP 429, wait (Retry-After, or $rateLimitWait) and retry once
     * @param float $timeout seconds before a request gives up (connect and read)
     * @param (\Closure(RequestEvent): void)|null $requestListener called once per call; exceptions it throws are ignored
     */
    public function __construct(
        public readonly string $apiKey = self::FREE_API_KEY,
        public readonly ?int $requestsPerMinute = null,
        public readonly int $maxRetries = 2,
        public readonly float $retryBackoff = 0.5,
        public readonly bool $retryOnRateLimit = true,
        public readonly float $rateLimitWait = 60.0,
        public readonly ?ResponseCache $cache = null,
        ?CachePolicy $cachePolicy = null,
        public readonly float $timeout = 30.0,
        public readonly ?\Closure $requestListener = null,
        public readonly string $baseUrl = 'https://www.thesportsdb.com',
        public readonly string $userAgent = 'thesportsdb-client-php',
    ) {
        $this->cachePolicy = $cachePolicy ?? new CachePolicy();
    }

    public function isFreeKey(): bool
    {
        return \in_array($this->apiKey, self::FREE_API_KEYS, true);
    }

    public function effectiveRequestsPerMinute(): int
    {
        return $this->requestsPerMinute ?? ($this->isFreeKey() ? 30 : 100);
    }
}
