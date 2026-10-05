<?php

declare(strict_types=1);

namespace SportsDb\Exception;

/** HTTP 429 after the retry was used up or disabled. */
final class RateLimitException extends SportsDbException
{
    /** @param float|null $retryAfter seconds the server asked to wait, if it said */
    public function __construct(string $message, public readonly ?float $retryAfter)
    {
        parent::__construct($message);
    }
}
