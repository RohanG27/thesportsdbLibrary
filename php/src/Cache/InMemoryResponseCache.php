<?php

declare(strict_types=1);

namespace SportsDb\Cache;

/** A bounded, per-process LRU cache with per-entry expiry. */
final class InMemoryResponseCache implements ResponseCache, \Countable
{
    /** @var array<string, array{string, float}> */
    private array $entries = [];

    /** @var \Closure(): float */
    private readonly \Closure $clock;

    /** @param (\Closure(): float)|null $clock */
    public function __construct(private readonly int $maxEntries = 1000, ?\Closure $clock = null)
    {
        $this->clock = $clock ?? static fn (): float => microtime(true);
    }

    public function get(string $key): ?string
    {
        if (!isset($this->entries[$key])) {
            return null;
        }
        [$body, $expires] = $this->entries[$key];
        unset($this->entries[$key]);
        if ($expires <= ($this->clock)()) {
            return null;
        }
        $this->entries[$key] = [$body, $expires]; // most recently used last
        return $body;
    }

    public function put(string $key, string $body, float $ttl): void
    {
        if ($ttl <= 0) {
            return;
        }
        unset($this->entries[$key]);
        $this->entries[$key] = [$body, ($this->clock)() + $ttl];
        while (\count($this->entries) > $this->maxEntries && ($oldest = array_key_first($this->entries)) !== null) {
            unset($this->entries[$oldest]);
        }
    }

    /** @return list<string> */
    public function keys(): array
    {
        return array_map('strval', array_keys($this->entries));
    }

    public function clear(): void
    {
        $this->entries = [];
    }

    public function count(): int
    {
        return \count($this->entries);
    }
}
