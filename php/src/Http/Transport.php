<?php

declare(strict_types=1);

namespace SportsDb\Http;

/**
 * Performs GET requests. Implement it to use another HTTP stack (e.g. a PSR-18 client) or to
 * fake the network in tests. Throw TransportException when no response arrives; return every
 * HTTP status, including 4xx and 5xx, as a Response.
 */
interface Transport
{
    /** @param array<string, string> $headers */
    public function get(string $url, array $headers): Response;
}
