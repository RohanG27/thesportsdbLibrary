<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\Internal;

use RohanG27\TheSportsDb\Clock;

/**
 * Sliding window: at most $permits calls in any $window seconds; callers wait for a slot.
 * Per client object (PHP keeps no state between requests unless you reuse the client).
 *
 * @internal
 */
final class RateLimiter
{
    /** @var list<float> */
    private array $starts = [];

    public function __construct(private readonly int $permits, private readonly Clock $clock, private readonly float $window = 60.0)
    {
    }

    public function acquire(): void
    {
        while (true) {
            $t = $this->clock->now();
            $this->starts = array_values(array_filter($this->starts, fn (float $s) => $t - $s < $this->window));
            if (\count($this->starts) < $this->permits) {
                $this->starts[] = $t;
                return;
            }
            $this->clock->sleep($this->window - ($t - $this->starts[0]));
        }
    }
}
