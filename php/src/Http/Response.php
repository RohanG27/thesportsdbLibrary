<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\Http;

final class Response
{
    /** @param array<string, string> $headers */
    public function __construct(public readonly int $status, public readonly string $body, public readonly array $headers = [])
    {
    }

    public function header(string $name): ?string
    {
        foreach ($this->headers as $key => $value) {
            if (strcasecmp($key, $name) === 0) {
                return $value;
            }
        }
        return null;
    }
}
