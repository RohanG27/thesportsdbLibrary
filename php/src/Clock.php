<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb;

/** Time source for rate limiting, retries and timing. Replace it in tests to avoid real waits. */
interface Clock
{
    public function now(): float;

    public function sleep(float $seconds): void;
}
