<?php

declare(strict_types=1);

namespace SportsDb;

/** One finished API call, passed to Config::$requestListener. */
final class RequestEvent
{
    public function __construct(
        /** The URL with any v1 key replaced by ***. Safe to log. */
        public readonly string $url,
        /** The last HTTP status; null if no response arrived or the result came from the cache. */
        public readonly ?int $status,
        /** HTTP requests made, counting retries; 0 when served from the cache. */
        public readonly int $attempts,
        public readonly bool $fromCache,
        /** Seconds for the whole call, including rate-limit waits and retries. */
        public readonly float $duration,
        public readonly ?\Throwable $error,
    ) {
    }
}
