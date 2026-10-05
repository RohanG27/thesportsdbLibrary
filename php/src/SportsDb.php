<?php

declare(strict_types=1);

namespace SportsDb;

use SportsDb\Exception\InvalidApiKeyException;
use SportsDb\Http\CurlTransport;
use SportsDb\Http\Transport;
use SportsDb\Internal\Requester;
use SportsDb\V1\V1Api;
use SportsDb\V2\V2Api;

/**
 * The TheSportsDB client.
 *
 * ```php
 * $db = new SportsDb();                          // free key 123: v1 only
 * $arsenal = $db->v1->lookup->team(133604);
 * $db = new SportsDb(apiKey: getenv('THESPORTSDB_API_KEY'), cache: new InMemoryResponseCache());
 * $tv = $db->v2->tv->country('Canada');
 * ```
 *
 * Reuse one client per key: its rate limiter and cache cover every call made through it.
 */
final class SportsDb
{
    public readonly Config $config;
    /** The v1 API (free and premium keys). */
    public readonly V1Api $v1;
    /** The v2 API (premium keys only). */
    public readonly V2Api $v2;
    /** Common tasks in one call, using v2 or v1 depending on the key. */
    public readonly Helpers $helpers;

    /**
     * Named arguments are the Config settings, plus an optional Transport and Clock.
     *
     * @param (\Closure(RequestEvent): void)|null $requestListener
     */
    public function __construct(
        string $apiKey = Config::FREE_API_KEY,
        ?int $requestsPerMinute = null,
        int $maxRetries = 2,
        float $retryBackoff = 0.5,
        bool $retryOnRateLimit = true,
        float $rateLimitWait = 60.0,
        ?Cache\ResponseCache $cache = null,
        ?Cache\CachePolicy $cachePolicy = null,
        float $timeout = 30.0,
        ?\Closure $requestListener = null,
        string $baseUrl = 'https://www.thesportsdb.com',
        string $userAgent = 'sportsdb-php',
        ?Transport $transport = null,
        ?Clock $clock = null,
    ) {
        $this->config = new Config($apiKey, $requestsPerMinute, $maxRetries, $retryBackoff, $retryOnRateLimit, $rateLimitWait,
            $cache, $cachePolicy, $timeout, $requestListener, $baseUrl, $userAgent);
        $requester = new Requester($this->config, $transport ?? new CurlTransport($timeout), $clock ?? new SystemClock());
        $this->v1 = new V1Api($requester);
        $this->v2 = new V2Api($requester);
        $this->helpers = new Helpers($this);
    }

    /** Whether v2 accepts the key. One call (v2 lookup/league/4328) unless it's a free key. */
    public function isPremiumKey(): bool
    {
        if ($this->config->isFreeKey()) {
            return false;
        }
        try {
            $this->v2->lookup->league(4328);
            return true;
        } catch (InvalidApiKeyException) {
            return false;
        }
    }
}
