<?php

declare(strict_types=1);

namespace SportsDb\Exception;

/** Any other non-2xx HTTP response, after retries for 5xx. */
final class HttpStatusException extends SportsDbException
{
    public function __construct(string $message, public readonly int $status, public readonly string $bodySnippet)
    {
        parent::__construct($message, $status);
    }
}
